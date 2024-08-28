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

import java.util.Map;

import com.google.gson.Gson;

import lombok.extern.slf4j.Slf4j;

/**
 * Helper class for updating the 'O1NetconfOperationsStatus' in the headers.
 */
@Slf4j
public class O1NetconfOperationsStatusHelper {

    private static final String O1_STATUS_HEADER_PROPERTY = O1NetconfOperationsStatus.class.getSimpleName();

    private O1NetconfOperationsStatusHelper() {}

    /**
     * Method to put the 'O1NetconfOperationsStatus' status in the headers, required for error handling.
     */
    public static void putHandlerStatusInHeaders(final O1NetconfOperationStatus operationStatus,
            final Map<String, Object> headers) {

        final O1NetconfOperationsStatus operationsStatus = getO1NetconfOperationsStatus(headers);
        log.trace("OperationStatus is [{}]", operationStatus.toString());
        operationsStatus.add(operationStatus);
        headers.put(O1_STATUS_HEADER_PROPERTY, operationsStatus);
    }

    public static void setSuppressTerminateFlow(final boolean suppressTerminateFlow, final Map<String, Object> headers) {
        final O1NetconfOperationsStatus operationsStatus = getO1NetconfOperationsStatus(headers);
        operationsStatus.setSuppressTerminateFlow(suppressTerminateFlow);
        headers.put(O1_STATUS_HEADER_PROPERTY, operationsStatus);
    }

    public static O1NetconfOperationsStatus getO1NetconfOperationsStatus(final Map<String, Object> headers) {
        if (headers.get(O1_STATUS_HEADER_PROPERTY) != null) {

            final Gson gson = new Gson();
            return gson.fromJson(gson.toJson(headers.get(O1_STATUS_HEADER_PROPERTY)), O1NetconfOperationsStatus.class);
        } else {
            final O1NetconfOperationsStatus operationsStatus = new O1NetconfOperationsStatus();
            headers.put(O1_STATUS_HEADER_PROPERTY, operationsStatus);
            return operationsStatus;
        }
    }
}
