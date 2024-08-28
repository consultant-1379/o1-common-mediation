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

import static com.ericsson.oss.mediation.o1.yang.handlers.netconf.api.CommonConstants.ID;
import static com.ericsson.oss.mediation.o1.yang.handlers.netconf.api.CommonConstants.MANAGED_ELEMENT;
import static com.ericsson.oss.mediation.o1.yang.handlers.netconf.api.CommonConstants.MANAGED_ELEMENT_ID;

import java.util.Map;

import javax.inject.Inject;

import org.json.XML;
import org.slf4j.Logger;

import com.ericsson.oss.itpf.common.event.handler.annotation.EventHandler;
import com.ericsson.oss.itpf.common.event.handler.exception.EventHandlerException;
import com.ericsson.oss.mediation.o1.common.handlers.netconf.O1AbstractHandler;
import com.ericsson.oss.mediation.o1.common.handlers.netconf.O1NetconfOperationResult;
import com.ericsson.oss.mediation.o1.common.handlers.netconf.O1NetconfOperationStatus;
import com.ericsson.oss.mediation.o1.yang.handlers.netconf.util.NetconfHelper;
import com.ericsson.oss.mediation.o1.yang.handlers.netconf.util.XPathFilter;
import com.ericsson.oss.mediation.util.netconf.api.NetconfManager;
import com.ericsson.oss.mediation.util.netconf.api.NetconfResponse;

import lombok.extern.slf4j.Slf4j;

/**
 * Handler to read the ManagedElement id from the node using a netconf GET call. The handling of the netconf connection and disconnection is done
 * within this handler as it does not extend the O1AbstractYangHandler class.
 * <br><br>
 * This handler is made to behave like the other handlers that extend the O1AbstractYangHandler so if an exception occurs it is caught and the header
 * property 'netconfSessionOperationsStatus' is updated to indicate to the other handlers that a netconf handler has failed, so they will not execute
 * and eventually the NetconfSessionReleaseHandler releases the session. Then the NetconfErrorControlHandler determines whether to allow the flow
 * continue or not.
 * <br><br>
 * Note if an exception does occur, it must be set the NetconfSessionOperationErrorCode.setAdditionalErrorMessage() so that the error message can be
 * rethrown by the NetconfErrorControlHandler later on.
 *
 * The handler requires the following parameter:
 *
 * netconfManager (NetconfOperationConnection)
 *
 * For details please refer to the handler model.
 */
@Slf4j
@EventHandler
public class ReadManagedElementIdHandler extends O1AbstractHandler {

    @Inject
    private NetconfHelper netconfHelper;

    @Override
    public O1NetconfOperationStatus executeO1Handler() {

        O1NetconfOperationStatus o1NetconfOperationStatus = createO1NetconfOperationStatusSuccess();
        NetconfManager netconfManager = null;

        try {
            netconfManager = netconfHelper.getNetconfManager(headers);
            netconfHelper.connect(netconfManager);

            final NetconfResponse netconfResponse = netconfHelper.readMo(netconfManager, getManagedElementXpathFilter());
            final String managedElementId = extractId(netconfResponse);

            headers.put(MANAGED_ELEMENT_ID, managedElementId);
            log.trace("Read managedElementId successful, [{}]", managedElementId);
            o1NetconfOperationStatus.setResult(O1NetconfOperationResult.OPERATION_SUCCESS);

        } finally {
            netconfHelper.disconnect(netconfManager);
        }
        return o1NetconfOperationStatus;
    }

    private String extractId(final NetconfResponse netconfResponse) {
        if (netconfResponse == null) {
            throw new EventHandlerException("Netconf GET response was null");
        }

        if (netconfResponse.getData().isEmpty() || netconfResponse.isError()) {
            throw new EventHandlerException("Error - netconf GET response was: " + netconfResponse);
        }

        final Map<String, Object> responseAsMap = XML.toJSONObject(netconfResponse.getData()).toMap();
        final Object managedElementId = ((Map<String, Object>) responseAsMap.get(MANAGED_ELEMENT)).get(ID);

        if (managedElementId == null) {
            throw new EventHandlerException("No managedElementId found on the node");
        }

        return managedElementId instanceof String ? (String) managedElementId : String.valueOf(managedElementId);
    }

    @Override
    protected Logger getLogger() {
        return log;
    }

    private static XPathFilter getManagedElementXpathFilter() {
        return new XPathFilter(new StringBuilder().append("/ManagedElement/attributes").toString());
    }
}
