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

package com.ericsson.oss.mediation.o1.heartbeat.service;

import static com.ericsson.oss.mediation.o1.heartbeat.service.O1HeartbeatState.FAILURE;
import static com.ericsson.oss.mediation.o1.heartbeat.service.O1HeartbeatState.FAILURE_ACKNOWLEDGED;
import static com.ericsson.oss.mediation.o1.heartbeat.service.O1HeartbeatState.OK;
import static com.ericsson.oss.mediation.o1.heartbeat.service.O1HeartbeatState.RECOVERED;

import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;

import lombok.Builder;
import lombok.Data;

/**
 * Registration object represents a registration of a Network Element with the heartbeat agent.
 * NetworkElements can be registered per FCAPS type (for example FM).
 * The registration holds the information about configured timeout value and a record of acknowledgements.
 * Based on the data above and the lastHeartbeatReceivedTimestamp it can calculate the O1HeartbeatState.
 */
@Data
public class Registration implements Serializable {

    private static final long serialVersionUID = 1L;

    private final FcapsType fcapsType;
    private final String networkElementFdn;
    private final String registrationKey;
    private boolean heartbeatFailureAcknowledged;
    private Duration heartbeatTimeout;
    private final long subscriptionTimestamp = Instant.now().getEpochSecond();

    @Builder
    Registration(FcapsType fcapsType, String networkElementFdn, Duration heartbeatTimeout) {
        this.fcapsType = fcapsType;
        this.networkElementFdn = networkElementFdn;
        this.registrationKey = getRegistrationKey(fcapsType, networkElementFdn);
        this.heartbeatTimeout = heartbeatTimeout == null ? Duration.ofSeconds(300) : heartbeatTimeout;
    }

    public O1HeartbeatState getHeartbeatState(Long lastHeartbeatReceivedTimestamp) {
        if (isHeartbeatFailure(getHeartbeatTimeout(), lastHeartbeatReceivedTimestamp)) {
            if (isHeartbeatFailureAcknowledged()) {
                return FAILURE_ACKNOWLEDGED;
            } else {
                return FAILURE;
            }
        } else {
            if (isHeartbeatFailureAcknowledged()) {
                return RECOVERED;
            } else {
                return OK;
            }
        }
    }

    private boolean isHeartbeatFailure(Duration heartbeatTimeout, Long lastHeartbeatReceivedTimestamp) {
        if (lastHeartbeatReceivedTimestamp != null) {
            return Instant.now().getEpochSecond() - lastHeartbeatReceivedTimestamp > heartbeatTimeout.getSeconds();
        } else {
            return Instant.now().getEpochSecond() - subscriptionTimestamp > heartbeatTimeout.getSeconds();
        }
    }

    public static String getRegistrationKey(FcapsType fcapsType, String networkElementFdn) {
        return String.format("RegistrationId=%s,%s", fcapsType, networkElementFdn);
    }
}
