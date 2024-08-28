/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2023
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.oss.mediation.o1.yang.handlers.netconf;

import static com.ericsson.oss.mediation.o1.yang.handlers.netconf.api.CommonConstants.FDN;
import static com.ericsson.oss.mediation.o1.yang.handlers.netconf.api.CommonConstants.ID;
import static com.ericsson.oss.mediation.o1.yang.handlers.netconf.api.CommonConstants.MANAGED_ELEMENT_ID;
import static com.ericsson.oss.mediation.o1.yang.handlers.netconf.api.CommonConstants.NTF_SUBSCRIPTION_CONTROL;
import static com.ericsson.oss.mediation.util.netconf.api.editconfig.Operation.CREATE;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import com.ericsson.oss.itpf.common.event.handler.annotation.EventHandler;
import com.ericsson.oss.itpf.common.event.handler.exception.EventHandlerException;
import com.ericsson.oss.mediation.o1.common.handlers.netconf.O1AbstractYangHandler;
import com.ericsson.oss.mediation.o1.util.FdnUtil;
import com.ericsson.oss.mediation.o1.yang.handlers.netconf.util.GlobalPropUtils;
import com.ericsson.oss.mediation.util.netconf.api.editconfig.Operation;

import lombok.extern.slf4j.Slf4j;

/**
 * Handler responsible for the creation and deletion of the NtfSubscriptionControl MO on the node.
 * This MO is required to subscribe and receive event notifications from the O1 node.
 *
 * The handler requires the following parameters:
 * <ul>
 * <li>fdn (String)</li>
 * <li>active (boolean)</li>
 * <li>VIP (String)</li>
 * <li>NtfSubscriptionControlId (String)</li>
 * <li>NtfSubscriptionControlO1MessageEventPath (String)</li>
 * <li>NtfSubscriptionControlNotificationTypes (List)</li>
 * <li>ManagedElementId (String)</li>
 * <li>netconfManager (NetconfOperationConnection)</li>
 * </ul>
 *
 * For details please refer to the handler model.
 */
@Slf4j
@EventHandler

public class NtfSubscriptionControlHandler extends O1AbstractYangHandler {

    private static final int EVENT_LISTENER_PORT = 8099;
    private static final String EVENT_LISTENER_REST_URL = "eventListener/v1";
    private static final String NOTIFICATION_RECIPIENT_ADDRESS = "notificationRecipientAddress";
    private static final String NOTIFICATION_TYPES = "notificationTypes";
    private static final String VIP = "VIP";
    private static final String NTF_SUBSCRIPTION_CONTROL_ID = "NtfSubscriptionControlId";
    private static final String NTF_SUBSCRIPTION_CONTROL_O1_MESSAGE_EVENT_PATH = "NtfSubscriptionControlO1MessageEventPath";
    private static final String NTF_SUBSCRIPTION_CONTROL_NOTIFICATION_TYPES = "NtfSubscriptionControlNotificationTypes";

    @Override
    protected void addSpecificYangData() {
        moData.setFdn(getNtfSubscriptionControlFdn((String) headers.get(MANAGED_ELEMENT_ID)));
        moData.setType(NTF_SUBSCRIPTION_CONTROL);
        if (isActivate()) {
            moData.setOperation(CREATE);
            final Map<String, Object> createAttributes = new HashMap<>();
            createAttributes.put(ID, getNtfSubscriptionControlId());
            createAttributes.put(NOTIFICATION_TYPES, getNotificationTypesSupported());
            createAttributes.put(NOTIFICATION_RECIPIENT_ADDRESS, getNotificationRecipientUrl());
            moData.setCreateAttributes(createAttributes);
        } else {
            moData.setOperation(Operation.DELETE);
        }
    }

    private List<String> getNotificationTypesSupported() {
        final List<String> notificationTypes =
                (List<String>) headers.getOrDefault(NTF_SUBSCRIPTION_CONTROL_NOTIFICATION_TYPES, Collections.emptyList());
        if (notificationTypes.isEmpty()) {
            throw new EventHandlerException(
                    "Cannot determine NtfSubscriptionControl.notificationTypes as 'NtfSubscriptionControlNotificationTypes' header has not been set");
        }
        return notificationTypes;
    }

    private Object getNtfSubscriptionControlId() {
        final String ntfSubscriptionControlId = (String) headers.getOrDefault(NTF_SUBSCRIPTION_CONTROL_ID, "");
        if (ntfSubscriptionControlId.isEmpty()) {
            throw new EventHandlerException("Cannot determine NtfSubscriptionControl.id as 'NtfSubscriptionControlId' header has not been set");
        }
        return ntfSubscriptionControlId;
    }

    private String getNtfSubscriptionControlFdn(final String managedElementId) {
        final String networkElementFdn = FdnUtil.getNetworkElementFdn(getHeaderFdn());
        final String meContextFdn = dpsRead.getMeContextFdn(networkElementFdn);
        return FdnUtil.createFdn(FdnUtil.getManagedElementFdn(meContextFdn, managedElementId), getNtfSubscriptionControlRdn());
    }

    private String getNotificationRecipientUrl() {
        try {
            final URL url = new URL("http", getVesCollectorIp(), EVENT_LISTENER_PORT, getEventListenerRestUrl());
            return url.toString();
        } catch (final MalformedURLException ex) {
            throw new EventHandlerException("Failed to build URL {}", ex);
        }
    }

    private String getEventListenerRestUrl() {
            final String path = (String) headers.get(NTF_SUBSCRIPTION_CONTROL_O1_MESSAGE_EVENT_PATH);
            if (StringUtils.isEmpty(path)) {
                throw new EventHandlerException("Cannot determine 'NTF_SUBSCRIPTION_CONTROL_O1_MESSAGE_EVENT_PATH' header has not been sent");
            } else {
                return String.format("/%s/%s", path, EVENT_LISTENER_REST_URL);
            }
        }

    private String getVesCollectorIp() {
        final String vesCollectorIpPropertyName = (String) moData.getHeaders().get(VIP);
        if (vesCollectorIpPropertyName == null || vesCollectorIpPropertyName.isEmpty()) {
            throw new EventHandlerException("Cannot determine VES collector ipaddress as 'VIP' header has not been set");
        }
        return GlobalPropUtils.getGlobalValue(vesCollectorIpPropertyName, String.class);
    }

    private String getNtfSubscriptionControlRdn() {
        return String.format("%s=%s", NTF_SUBSCRIPTION_CONTROL, getNtfSubscriptionControlId());
    }

    @Override
    protected Logger getLogger() {
        return log;
    }
}
