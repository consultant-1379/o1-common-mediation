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

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;

import com.ericsson.oss.itpf.common.event.handler.annotation.EventHandler;
import com.ericsson.oss.itpf.common.event.handler.exception.EventHandlerException;
import com.ericsson.oss.mediation.o1.common.handlers.netconf.O1AbstractYangHandler;
import com.ericsson.oss.mediation.o1.util.FdnUtil;
import com.ericsson.oss.mediation.util.netconf.api.editconfig.Operation;

import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * Creates HeartbeatControl MO on the node. It is child class of NtfSubscriptionControl MO.
 * Note the creation of the MO is skipped if it finds that the HeartbeatControl MO has already been created - this is
 * done to support the idempotent use case.
 * </p><p>
 * The value HeartbeatControl.heartbeatNtfPeriod is set with the value of FmFunction.heartbeatinterval
 * </p>
 *
 * The handler requires the following parameters:
 * <ul>
 * <li>fdn (String)</li>
 * <li>active (boolean)</li>
 * <li>heartbeatInterval (integer)</li>
 * <li>NtfSubscriptionControlId (String)</li>
 * <li>ManagedElementId (String)</li>
 * <li>netconfManager (NetconfOperationConnection)</li>
 * </ul>
 *
 * For details please refer to the handler model.
 */
@Slf4j
@EventHandler
public class HeartbeatControlHandler extends O1AbstractYangHandler {

    private static final String NTF_SUBSCRIPTION_CONTROL_ID = "NtfSubscriptionControlId";
    private static final String HEARTBEAT_CONTROL = "HeartbeatControl";
    private static final String HEARTBEAT_CONTROL_RDN = "HeartbeatControl=1";
    private static final String HEARTBEAT_INTERVAL = "heartbeatinterval";
    private static final String HEARTBEAT_NTF_PERIOD = "heartbeatNtfPeriod";

    @Override
    protected void addSpecificYangData() {
        moData.setFdn(getHeartbeatControlFdn((String) headers.get(MANAGED_ELEMENT_ID)));
        moData.setType(HEARTBEAT_CONTROL);
        moData.setOperation(Operation.CREATE);
        moData.setCreateAttributes(getHeartbeatControlMoAttributes());
    }

    @Override
    protected boolean skipHandler() {
        return !isActivate();
    }

    @Override
    protected Logger getLogger() {
        return log;
    }

    private String getHeartbeatControlFdn(final String managedElementId) {
        final String networkElementFdn = FdnUtil.getNetworkElementFdn(getHeaderFdn());
        final String meContextFdn = dpsRead.getMeContextFdn(networkElementFdn);
        return FdnUtil.createFdn(FdnUtil.getManagedElementFdn(meContextFdn, managedElementId), getNtfSubscriptionControlRdn(), HEARTBEAT_CONTROL_RDN);
    }

    private String getNtfSubscriptionControlRdn() {
        return NTF_SUBSCRIPTION_CONTROL + "=" + getNtfSubscriptionControlId();
    }

    private Map<String, Object> getHeartbeatControlMoAttributes() {
        final int heartbeatInterval = (Integer) moData.getHeaders().get(HEARTBEAT_INTERVAL);
        final Map<String, Object> attributes = new HashMap<>();
        attributes.put(ID, "1");
        attributes.put(HEARTBEAT_NTF_PERIOD, heartbeatInterval);
        return attributes;
    }

    private Object getNtfSubscriptionControlId() {
        final String ntfSubscriptionControlId = (String) headers.getOrDefault(NTF_SUBSCRIPTION_CONTROL_ID, "");
        if (ntfSubscriptionControlId.isEmpty()) {
            throw new EventHandlerException("Cannot determine NtfSubscriptionControl.id as 'NtfSubscriptionControlId' header has not been set");
        }
        return ntfSubscriptionControlId;
    }

}
