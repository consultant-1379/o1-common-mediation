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

package com.ericsson.oss.mediation.o1.event.resources;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.ericsson.oss.mediation.o1.event.service.O1NotificationNormalizer;
import org.json.JSONObject;

import com.ericsson.oss.mediation.o1.event.model.O1Notification;
import com.ericsson.oss.mediation.o1.event.service.O1ProcessingService;

import lombok.extern.slf4j.Slf4j;

@Path("/")
@Slf4j
public class O1NotificationReceiver {

    @Inject
    private O1NotificationNormalizer o1NotificationNormalizer;

    @Inject
    private O1ProcessingService o1ProcessingService;

    @Path("/{serviceIdentifier}/eventListener/v{version}")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response processNotification(@PathParam("serviceIdentifier") String serviceIdentifier, final String notification) {
        log.debug("Received Notification {}", notification);
        if (serviceIdentifier.equalsIgnoreCase(System.getProperty("med_service_protocol_info"))) {
            final O1Notification o1Notification = o1NotificationNormalizer.normalize(notification);
            o1ProcessingService.processNotification(o1Notification);

            return Response.accepted("Successfully processed the notification").build();
        } else {
            return Response.status(Status.FORBIDDEN).build();
        }
    }

}
