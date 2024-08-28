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

package com.ericsson.oss.mediation.o1.yang.handlers.netconf;

import org.slf4j.Logger;

import com.ericsson.oss.itpf.common.event.handler.annotation.EventHandler;
import com.ericsson.oss.mediation.o1.common.handlers.netconf.O1AbstractHandler;
import com.ericsson.oss.mediation.o1.common.handlers.netconf.O1NetconfOperationStatus;
import com.ericsson.oss.mediation.o1.common.handlers.netconf.O1NetconfOperationsStatus;
import com.ericsson.oss.mediation.o1.common.handlers.netconf.O1NetconfOperationsStatusHelper;
import com.ericsson.oss.mediation.o1.common.handlers.netconf.O1TerminateFlowException;

import lombok.extern.slf4j.Slf4j;

/**
 * This handler checks for failed operations in the header property 'O1NetconfOperationsStatus'. If a failed
 * operation is found then the following is done:
 * <ul>
 * <li>The error is recorded using SystemRecorder</li>
 * <li>An AlarmSupervisionResponse event is sent to FM indicating a supervision failure</li>
 * <li>An O1TerminateFlowException is thrown with the original exception message which will rollback any transactions
 * and terminate the flow.</li>
 * </ul>
 * <p>
 * If no failed operations are found then the handler does nothing and the flow continues.
 * <br> Note - An aborted operation is not regarded as a failed operation by O1NetconfOperationsStatus.
 */
@Slf4j
@EventHandler
public class NetconfErrorControlHandler extends O1AbstractHandler {

    @Override
    public O1NetconfOperationStatus executeO1Handler() {

        final O1NetconfOperationsStatus operationsStatus = O1NetconfOperationsStatusHelper.getO1NetconfOperationsStatus(headers);

        O1NetconfOperationStatus operationStatus = createO1NetconfOperationStatusSuccess();
        if (operationsStatus.isSuppressTerminateFlow()) {
            return operationStatus;
        }

        if (operationsStatus.getFailedOperation() != null) {
            final O1NetconfOperationStatus failedOperation = operationsStatus.getFailedOperation();
            recordError(failedOperation);
            throw new O1TerminateFlowException(failedOperation.getException().getMessage(), failedOperation.getException());
        }
        return operationStatus;
    }

    @Override
    protected boolean cannotSkipHandler() {
        return true;
    }

    @Override
    protected Logger getLogger() {
        return log;
    }
}
