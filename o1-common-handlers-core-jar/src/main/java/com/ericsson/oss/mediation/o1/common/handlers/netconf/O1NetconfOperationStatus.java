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

import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * Data class for storing the O1NetconfOperationResult of a netconf operation.
 */
@Data
@RequiredArgsConstructor
public class O1NetconfOperationStatus {

    private final String handlerName;
    private final String nodeFdn;
    private O1NetconfOperationResult result = O1NetconfOperationResult.OPERATION_SUCCESS;
    private Exception exception;

    public O1NetconfOperationStatus(final Class handlerClass, final String nodeFdn) {
        handlerName = handlerClass.getCanonicalName();
        this.nodeFdn = nodeFdn;
    }
}
