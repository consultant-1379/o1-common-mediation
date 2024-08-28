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
import static com.ericsson.oss.mediation.o1.yang.handlers.netconf.api.CommonConstants.MANAGED_ELEMENT;
import static com.ericsson.oss.mediation.o1.yang.handlers.netconf.api.CommonConstants.MANAGED_ELEMENT_ID;
import static com.ericsson.oss.mediation.util.netconf.api.editconfig.Operation.MERGE;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;

import com.ericsson.oss.itpf.common.event.handler.annotation.EventHandler;
import com.ericsson.oss.mediation.o1.common.handlers.netconf.O1AbstractYangHandler;
import com.ericsson.oss.mediation.o1.util.FdnUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * A handler to update the 'ManagedElement.dnPrefix' attribute on the node with the MeContext FDN.
 *
 * The handler requires the following parameters:
 * <ul>
 * <li>fdn (String)</li>
 * <li>active (boolean)</li>
 * <li>ManagedElementId (String)</li>
 * <li>netconfManager (NetconfOperationConnection)</li>
 * </ul>
 *
 * For details please refer to the handler model.
 */
@Slf4j
@EventHandler
public class SetDnPrefixHandler extends O1AbstractYangHandler {

    private static final String DN_PREFIX = "dnPrefix";

    @Override
    public boolean skipHandler() {
        log.trace("Skipping as supervision is set to [{}]", isActivate());
        return !isActivate();
    }

    @Override
    protected void addSpecificYangData() {
        moData.setFdn(getManagedElementFdn((String) headers.get(MANAGED_ELEMENT_ID)));
        moData.setType(MANAGED_ELEMENT);
        moData.setOperation(MERGE);
        final Map<String, Object> modifiedAttributes = new HashMap<>();
        modifiedAttributes.put(DN_PREFIX, getMeContextFdn());
        moData.setModifyAttributes(modifiedAttributes);
    }

    private String getMeContextFdn() {
        final String networkElementFdn = FdnUtil.getNetworkElementFdn(getHeaderFdn());
        return dpsRead.getMeContextFdn(networkElementFdn);
    }

    private String getManagedElementFdn(final String managedElementId) {
        return FdnUtil.getManagedElementFdn(getMeContextFdn(), managedElementId);
    }

    @Override
    protected Logger getLogger() {
        return log;
    }
}
