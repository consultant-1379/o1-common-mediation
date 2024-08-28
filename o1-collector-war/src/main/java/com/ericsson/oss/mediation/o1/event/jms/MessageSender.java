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

package com.ericsson.oss.mediation.o1.event.jms;

import static java.util.concurrent.TimeUnit.SECONDS;

import static com.ericsson.oss.itpf.sdk.recording.EventLevel.DETAILED;

import java.io.Serializable;
import java.util.HashMap;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import com.ericsson.oss.itpf.sdk.core.retry.RetryManager;
import com.ericsson.oss.itpf.sdk.core.retry.RetryPolicy;
import com.ericsson.oss.itpf.sdk.core.util.ServiceIdentity;
import com.ericsson.oss.itpf.sdk.eventbus.Channel;
import com.ericsson.oss.itpf.sdk.eventbus.ChannelConfiguration;
import com.ericsson.oss.itpf.sdk.eventbus.ChannelConfigurationBuilder;
import com.ericsson.oss.itpf.sdk.eventbus.ChannelLocator;
import com.ericsson.oss.itpf.sdk.eventbus.EventConfiguration;
import com.ericsson.oss.itpf.sdk.eventbus.EventConfigurationBuilder;
import com.ericsson.oss.itpf.sdk.eventbus.annotation.PersistenceType;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.mediation.o1.event.model.O1Notification;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MessageSender {

    private static final String JMS_QUEUE = "jms:/queue/";

    private static final String O1_NOTIFICATIONS_QUEUE = JMS_QUEUE + System.getProperty("o1_notifications_channelId");
    private EventConfiguration eventConfiguration;
    private String msInstanceId;

    @Inject
    private ServiceIdentity serviceIdentity;

    @Inject
    private SystemRecorder systemRecorder;

    private static final ChannelConfiguration o1NotificationsConfiguration =
            new ChannelConfigurationBuilder().persistence(PersistenceType.PERSISTENT).priority(Channel.NORMAL_PRIORITY).timeToLive(0)
                    .build();

    private static final RetryPolicy policy = RetryPolicy.builder()
            .attempts(5)
            .waitInterval(5, SECONDS)
            .retryOn(RuntimeException.class)
            .exponentialBackoff(2D)
            .build();

    @Inject
    private RetryManager retryManager;

    @Inject
    private ChannelLocator channelLocator;

    private Channel o1NotificationsChannel;

    @PostConstruct
    private void init() {
        msInstanceId = serviceIdentity.getNodeId();
        EventConfigurationBuilder eventConfigurationBuilder = new EventConfigurationBuilder();
        eventConfigurationBuilder.addEventProperty("__target_ms_instance", msInstanceId);
        eventConfiguration = eventConfigurationBuilder.build();
        o1NotificationsChannel = channelLocator.lookupAndConfigureChannel(O1_NOTIFICATIONS_QUEUE, o1NotificationsConfiguration);
    }

    public final void send(final O1Notification notification) {
        final HashMap<String, Object> normalisedNotification = notification.getNormalisedNotification();
        try {
            recordSendingNotification(notification, o1NotificationsChannel.getChannelURI());
            sendNotification(normalisedNotification);
        } catch (Exception ex) {
            log.error("Exception while sending notification to queue. Details: {}", ex.getMessage());
        }
    }

    private void recordSendingNotification(final O1Notification notification, String channelURI) {
        systemRecorder.recordEvent("O1_COLLECTOR", DETAILED,
                notification.getHref(),
                notification.getSystemDn(),
                "Sending notification to queue [" + channelURI + "] with msInstanceId [" + msInstanceId + "]");
    }

    private void sendNotification(final Serializable notification) {
        retryManager.executeCommand(policy, retryContext -> {
            o1NotificationsChannel.send(notification, eventConfiguration);
            return null;
        });
    }
}
