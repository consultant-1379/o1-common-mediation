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

package com.ericsson.oss.mediation.o1.event.mapper;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import com.ericsson.oss.itpf.sdk.recording.ErrorSeverity;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;

@Provider
public class GenericExceptionMapper implements ExceptionMapper<Exception> {

    @Inject
    SystemRecorder systemRecorder;

    @Override
    public Response toResponse(final Exception ex) {

        systemRecorder.recordError(getClass().getSimpleName(), ErrorSeverity.ERROR, "", "",
                "Internal Server Error.");

        return Response.status(Status.INTERNAL_SERVER_ERROR)
                .entity("An error occurred: " + ex.getMessage())
                .build();
    }
}
