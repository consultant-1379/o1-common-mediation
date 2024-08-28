/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2024
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.oss.mediation.o1.common.handlers.netconf;

import static com.ericsson.oss.mediation.adapter.netconf.jca.xa.yang.provider.YangNetconfOperationResult.YangNetconfOperationResultCode.ERROR;
import static com.ericsson.oss.mediation.o1.yang.handlers.netconf.api.CommonConstants.ACTIVE;
import static com.ericsson.oss.mediation.o1.yang.handlers.netconf.api.CommonConstants.INCLUDE_NS_PREFIX;
import static com.ericsson.oss.mediation.o1.yang.handlers.netconf.api.CommonConstants.NAMESPACE;
import static com.ericsson.oss.mediation.o1.yang.handlers.netconf.api.CommonConstants.NE_TYPE;
import static com.ericsson.oss.mediation.o1.yang.handlers.netconf.api.CommonConstants.VERSION;

import java.util.Map;

import javax.inject.Inject;

import com.ericsson.oss.itpf.common.event.handler.EventHandlerContext;
import com.ericsson.oss.itpf.common.event.handler.exception.EventHandlerException;
import com.ericsson.oss.mediation.adapter.netconf.jca.api.operation.NetconfOperationConnection;
import com.ericsson.oss.mediation.adapter.netconf.jca.api.operation.NetconfOperationRequest;
import com.ericsson.oss.mediation.adapter.netconf.jca.xa.yang.provider.MO2EditConfigOperationConverter;
import com.ericsson.oss.mediation.adapter.netconf.jca.xa.yang.provider.YangEditConfigOperation;
import com.ericsson.oss.mediation.adapter.netconf.jca.xa.yang.provider.YangNetconfOperationResult;
import com.ericsson.oss.mediation.adapter.netconf.jca.xa.yang_read.provider.MO2GetConfigOperationConverter;
import com.ericsson.oss.mediation.o1.util.dps.DpsRead;
import com.ericsson.oss.mediation.o1.yang.handlers.netconf.api.MoData;
import com.ericsson.oss.mediation.util.netconf.api.Datastore;
import com.ericsson.oss.mediation.util.netconf.api.editconfig.Operation;

/**
 * An abstract class for handlers that want to send Yang netconf operations.
 */
public abstract class O1AbstractYangHandler extends O1AbstractHandler {

    protected MoData moData;

    @Inject
    protected DpsRead dpsRead;

    @Override
    public void init(final EventHandlerContext ctx) {
        super.init(ctx);
        moData = new MoData();
        moData.setHeaders(headers); // headers be enough?
        moData.setIncludeNsPrefix((Boolean) headers.getOrDefault(INCLUDE_NS_PREFIX, Boolean.TRUE));
        moData.setNameSpace(NAMESPACE);
        moData.setVersion(VERSION);
    }

    @Override
    public O1NetconfOperationStatus executeO1Handler() {

        O1NetconfOperationStatus o1NetconfOperationStatus = new O1NetconfOperationStatus(this.getClass(), getNodeAddressFdn());
        YangNetconfOperationResult yangResult = null;

        try {
            preExecuteYangOperation();
            yangResult = executeYangRequest(constructYangRequest());
            processYangResponse(yangResult);
        } catch (Exception e) {
            o1NetconfOperationStatus = createO1NetconfOperationStatusFailed(e);
        } finally {
            postExecuteYangOperation(yangResult, o1NetconfOperationStatus);
        }

        return o1NetconfOperationStatus;
    }

    @Override
    public void destroy() {
        super.destroy();
        moData = null;
        dpsRead = null;
    }

    protected boolean isActivate() {
        boolean active = false;
        if (headers.containsKey("supervisionAttributes")) { // supervision based client
            final Map<String, Object> supervisionAttributes = (Map<String, Object>) headers.get("supervisionAttributes");
            active = (boolean) supervisionAttributes.getOrDefault(ACTIVE, false);
        } else if (headers.containsKey(ACTIVE)) { // event based client
            active = (boolean) headers.getOrDefault(ACTIVE, false);
        }
        return active;
    }

    protected YangNetconfOperationResult executeYangRequest(final NetconfOperationRequest netconfOperationRequest) {
        getLogger().trace("Request is [{}]", netconfOperationRequest);
        final NetconfOperationConnection netconfConnection = (NetconfOperationConnection) headers.get("netconfManager");
        final YangNetconfOperationResult yangResult =
                (YangNetconfOperationResult) netconfConnection.executeXAResourceOperation(netconfOperationRequest);

        if (yangResult.getResultCode().equals(ERROR)) {
            throw new EventHandlerException("YangResult was error");
        }

        getLogger().trace("Yang result code is [{}]", yangResult.getResultCode());
        return yangResult;
    }

    protected void preExecuteYangOperation() {
        addSpecificYangData();
    }

    protected void postExecuteYangOperation(final YangNetconfOperationResult yangResult, final O1NetconfOperationStatus operationStatus) {}

    protected void processYangResponse(final YangNetconfOperationResult yangResult) {}

    protected NetconfOperationRequest constructYangRequest() {

        if (moData.getOperation() != null) { // for CREATE, DELETE or MERGE setOperation() is called

            final Operation operation = moData.getOperation();

            getLogger().info("moData is  : {}", moData);
            final String networkElementFdn = dpsRead.getNetworkElementFdn(moData.getFdn());
            final String neType = dpsRead.getAttributeValue(networkElementFdn, NE_TYPE);

            final MO2EditConfigOperationConverter converter = new MO2EditConfigOperationConverter();
            converter.setFdn(moData.getFdn());
            converter.setNamespace(moData.getNameSpace());
            converter.setType(moData.getType());
            converter.setVersion(moData.getVersion());
            converter.setNodeType(neType);
            converter.setNamespacePrefix(moData.isIncludeNsPrefix());
            converter.setnodeName(moData.getNodeName());
            converter.setnodeNameSpace(moData.getNodeNamespace());
            converter.setOperation(moData.getOperation());

            if (operation.equals(Operation.MERGE)) {
                converter.setAttributes(moData.getModifyAttributes());
            }
            if (operation.equals(Operation.CREATE)) {
                converter.setAttributes(moData.getCreateAttributes());
            }
            return converter.convert();

        } else { // for GET setOperation() is not set

            final MO2GetConfigOperationConverter converter = new MO2GetConfigOperationConverter();
            final String fdn = moData.getFdn();

            final String networkElementFdn = dpsRead.getNetworkElementFdn(fdn);
            final String neType = dpsRead.getAttributeValue(networkElementFdn, NE_TYPE);
            converter.setFdn(fdn);
            converter.setNamespace(moData.getNameSpace());
            converter.setType(moData.getType());
            converter.setVersion(moData.getVersion());
            converter.setNodeType(neType);

            converter.setAttributes(moData.getReadAttributes());
            return converter.convert();
        }
    }

    protected abstract void addSpecificYangData();

}
