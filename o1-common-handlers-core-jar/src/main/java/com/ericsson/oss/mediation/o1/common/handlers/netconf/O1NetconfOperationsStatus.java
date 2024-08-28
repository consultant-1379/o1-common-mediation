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

import static com.ericsson.oss.mediation.o1.common.handlers.netconf.O1NetconfOperationResult.OPERATION_FAILED;

import java.util.ArrayList;
import java.util.Collection;

import lombok.Data;

/**
 * The main class that stores the information of all O1 netconf operations executed and those that failed.
 */
@Data
public class O1NetconfOperationsStatus {

    private Collection<O1NetconfOperationStatus> allOperations = new ArrayList<>();
    private O1NetconfOperationStatus failedOperation = null;
    private boolean suppressTerminateFlow = false;

    public void add(final O1NetconfOperationStatus o1NetconfOperationStatus) {
        allOperations.add(o1NetconfOperationStatus);
        if (o1NetconfOperationStatus.getResult().equals(OPERATION_FAILED)) {
            failedOperation = o1NetconfOperationStatus;
        }
    }

    public O1NetconfOperationStatus getOperation(final Class handler) {
        for (final O1NetconfOperationStatus o : allOperations) {
            if (o.getHandlerName().equals(handler.getCanonicalName())) {
                return o;
            }
        }
        return null;
    }
}
