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

package com.ericsson.oss.mediation.o1.event.service;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.ericsson.oss.itpf.sdk.core.annotation.EServiceRef;
import com.ericsson.oss.mediation.o1.event.jms.MessageSender;
import com.ericsson.oss.mediation.o1.event.model.O1Notification;
import com.ericsson.oss.mediation.o1.event.validator.O1NotificationValidator;
import com.ericsson.oss.mediation.o1.heartbeat.service.O1HeartbeatAgent;
import com.ericsson.oss.mediation.o1.util.FdnUtil;
import com.ericsson.oss.mediation.o1.util.HrefUtil;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class O1ProcessingService {

    public static final String NOTIFICATION_TYPE_HEARTBEAT = "notifyHeartbeat";

    @Inject
    private MessageSender messageSender;

    @EServiceRef
    O1HeartbeatAgent o1HeartbeatAgent;

    public void processNotification(final O1Notification notification) {
        if (NOTIFICATION_TYPE_HEARTBEAT.equals(notification.getNotificationType())) {
            final String networkElementFdn = FdnUtil.getNetworkElementFdn(HrefUtil.extractDnPrefix(notification.getHref()));
            o1HeartbeatAgent.notifyHb(networkElementFdn);
        } else {
            messageSender.send(notification);
        }
        log.debug("Notification processed successfully");
    }
}
