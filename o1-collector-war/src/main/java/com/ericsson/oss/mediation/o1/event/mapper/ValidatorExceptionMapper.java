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
import com.ericsson.oss.mediation.o1.event.exception.O1ValidationException;

@Provider
public class ValidatorExceptionMapper implements ExceptionMapper<O1ValidationException> {

    @Inject
    private SystemRecorder systemRecorder;

    @Override
    public Response toResponse(final O1ValidationException o1ValidationException) {

        systemRecorder.recordError(getClass().getSimpleName(), ErrorSeverity.WARNING, "", "",
                "Error validating O1 Notification. The notification will be discarded.");

        return Response.status(Status.BAD_REQUEST)
                .entity("JSON does not conform to schema. Notification will be discarded." + o1ValidationException.getMessage())
                .build();
    }
}
