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

import static com.ericsson.oss.mediation.o1.common.handlers.netconf.O1NetconfOperationResult.OPERATION_ABORTED;
import static com.ericsson.oss.mediation.o1.common.handlers.netconf.O1NetconfOperationResult.OPERATION_FAILED;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.jms.ObjectMessage;

import com.ericsson.oss.mediation.o1.yang.handlers.netconf.api.CommonConstants;
import org.slf4j.Logger;

import com.ericsson.oss.itpf.common.event.handler.EventHandlerContext;
import com.ericsson.oss.itpf.common.event.handler.ResultEventInputHandler;
import com.ericsson.oss.itpf.common.event.handler.exception.EventHandlerException;
import com.ericsson.oss.itpf.sdk.recording.ErrorSeverity;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.mediation.flow.events.MediationComponentEvent;
import com.ericsson.oss.mediation.fm.o1.common.util.EventFormatter;

/**
 * An abstract class that catches any handler exceptions so that they can
 * be processed later on by the NetconfErrorControlHandler.
 */
public abstract class O1AbstractHandler implements ResultEventInputHandler {

    protected Map<String, Object> headers = new HashMap<>();
    protected Object payload = null;
    protected MediationComponentEvent inputMediationComponentEvent = null;
    protected ObjectMessage objectMessage;

    @Inject
    private SystemRecorder recorder;

    @Override
    public final Object onEventWithResult(final Object inputObject) {
        O1NetconfOperationStatus operationStatus = createO1NetconfOperationStatusSuccess();

        try {
            extractInputObject(inputObject);

            boolean execute = false;
            if (cannotSkipHandler()) {
                execute = true;
            } else {
                if (previousHandlerNotSuccessful() || skipHandler()) {
                    operationStatus.setResult(OPERATION_ABORTED);
                    execute = false;
                } else {
                    execute = true;
                }
            }

            if (execute) {
                operationStatus = executeO1Handler();
            }
        } catch (O1TerminateFlowException fex) {
            throw fex; // rolls back TX
        } catch (Exception e) {
            operationStatus = createO1NetconfOperationStatusFailed(e);
            recordError(operationStatus);
        } finally {
            O1NetconfOperationsStatusHelper.putHandlerStatusInHeaders(operationStatus, headers);
        }
        return new MediationComponentEvent(headers, payload);
    }

    // Should only override from error handler
    protected boolean cannotSkipHandler() {
        return false;
    }

    // checks only the O1 handlers status
    protected boolean previousHandlerNotSuccessful() {
        return getO1NetconfOperationsStatus().getFailedOperation() != null;
    }

    private O1NetconfOperationsStatus getO1NetconfOperationsStatus() {
        return O1NetconfOperationsStatusHelper.getO1NetconfOperationsStatus(headers);
    }

    private void extractInputObject(final Object inputObject) {
        if (inputObject == null) {
            throw new EventHandlerException("Internal error - input event received is null");
        } else if (inputObject instanceof MediationComponentEvent) {
            final MediationComponentEvent mediationComponentEvent = (MediationComponentEvent) inputObject;
            headers.putAll(mediationComponentEvent.getHeaders());
            payload = mediationComponentEvent.getPayload();
            logHeadersAndPayload();
        } else if (inputObject instanceof ObjectMessage) {
            objectMessage = (ObjectMessage) inputObject;
        } else {
            throw new EventHandlerException("Internal error - input event received was an unexpected type [" + inputObject + "]");
        }
    }

    protected void recordError(final O1NetconfOperationStatus operationStatus) {
        recorder.recordError(operationStatus.getHandlerName(), ErrorSeverity.ERROR, getFlowUrn(), operationStatus.getNodeFdn(),
                operationStatus.getException().toString());
    }

    protected void recordWarning(final O1NetconfOperationStatus operationStatus, final String additionalInfo) {
        recorder.recordError(operationStatus.getHandlerName(), ErrorSeverity.WARNING, getFlowUrn(), operationStatus.getNodeFdn(),
                additionalInfo);
    }

    protected String getFlowUrn() {
        return (String) headers.get("flowUrn");
    }

    @Override
    public final void onEvent(final Object o) {
        throw new IllegalArgumentException("Not supported");
    }

    @Override
    public void init(final EventHandlerContext ctx) {
        final Map<String, Object> allProperties = ctx.getEventHandlerConfiguration().getAllProperties();
        headers.putAll(allProperties);
    }

    @Override
    public void destroy() {
        headers = null;
        payload = null;
        objectMessage = null;
        inputMediationComponentEvent = null;
    }

    protected O1NetconfOperationStatus createO1NetconfOperationStatusSuccess() {
        return new O1NetconfOperationStatus(this.getClass(), getNodeAddressFdn());
    }

    protected O1NetconfOperationStatus createO1NetconfOperationStatusFailed(final Exception e) {
        final O1NetconfOperationStatus operationStatus = new O1NetconfOperationStatus(this.getClass(), getNodeAddressFdn());
        operationStatus.setResult(OPERATION_FAILED);
        operationStatus.setException(e);
        return operationStatus;
    }

    protected abstract O1NetconfOperationStatus executeO1Handler();

    protected abstract Logger getLogger();

    // Override if there are certain conditions when the handler should abort.
    protected boolean skipHandler() {
        return false;
    }

    protected String getNodeAddressFdn() {
        return (String) headers.get("nodeAddress");
    }

    protected String getHeaderFdn() {
        return (String) headers.get(CommonConstants.FDN);
    }

    private void logHeadersAndPayload() {
        if (getLogger().isTraceEnabled()) {
            getLogger().trace("with headers: {}", EventFormatter.format(headers));
            getLogger().trace("with payload: {}", EventFormatter.format(payload));
        }
    }
}
