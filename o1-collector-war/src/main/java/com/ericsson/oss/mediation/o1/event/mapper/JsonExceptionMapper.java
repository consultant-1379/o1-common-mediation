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

import org.json.JSONException;

import com.ericsson.oss.itpf.sdk.recording.ErrorSeverity;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;

@Provider
public class JsonExceptionMapper implements ExceptionMapper<JSONException> {

    @Inject
    SystemRecorder systemRecorder;

    @Override
    public Response toResponse(final JSONException jsonException) {

        systemRecorder.recordError(getClass().getSimpleName(), ErrorSeverity.WARNING, "", "",
                "Error validating O1 Notification. The notification will be discarded.");

        return Response.status(Status.BAD_REQUEST)
                .entity("JSON does not conform to schema. Notification will be discarded." + jsonException.getMessage())
                .build();
    }
}
