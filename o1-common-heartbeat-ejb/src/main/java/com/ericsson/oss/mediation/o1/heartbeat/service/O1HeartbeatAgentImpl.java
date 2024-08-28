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

package com.ericsson.oss.mediation.o1.heartbeat.service;

import static com.ericsson.oss.mediation.o1.heartbeat.service.O1HeartbeatState.FAILURE;
import static com.ericsson.oss.mediation.o1.heartbeat.service.O1HeartbeatState.FAILURE_ACKNOWLEDGED;
import static com.ericsson.oss.mediation.o1.heartbeat.service.O1HeartbeatState.RECOVERED;
import static com.ericsson.oss.mediation.o1.heartbeat.service.Registration.getRegistrationKey;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Stateless
public class O1HeartbeatAgentImpl implements O1HeartbeatAgent {

    public static final String HEARTBEAT_CACHE_NAME = "O1HeartbeatNotificationCache";
    public static final String REGISTRATION_CACHE_NAME = "O1HeartbeatRegistrationCache";

    @Inject
    private CacheManager<Long> heartbeatCache;

    @Inject
    private CacheManager<Registration> registrationCache;

    @PostConstruct
    public void initializeCacheName() {
        heartbeatCache.initializeName(HEARTBEAT_CACHE_NAME);
        registrationCache.initializeName(REGISTRATION_CACHE_NAME);
    }

    @Override
    public void register(FcapsType fcapsType, String networkElementFdn, Duration heartbeatTimeout) {
        if (!isRegistered(fcapsType, networkElementFdn)) {
            Registration registration = Registration.builder()
                    .fcapsType(fcapsType)
                    .networkElementFdn(networkElementFdn)
                    .heartbeatTimeout(heartbeatTimeout).build();
            registrationCache.put(registration.getRegistrationKey(), registration);
            log.info("Registered [{}]", registration.getRegistrationKey());
        } else {
            log.warn("Already registered [{}], skipping", getRegistrationKey(fcapsType, networkElementFdn));
        }
    }

    @Override
    public void unregister(FcapsType fcapsType, String networkElementFdn) {
        String registrationKey = getRegistrationKey(fcapsType, networkElementFdn);
        if (isRegistered(fcapsType, networkElementFdn)) {
            registrationCache.remove(registrationKey);
            log.info("Unregistered [{}]", registrationKey);
        } else {
            log.warn("Not registered [{}], skipping", registrationKey);
        }
    }

    @Override
    public boolean isRegistered(FcapsType fcapsType, String networkElementFdn) {
        return registrationCache.contains(getRegistrationKey(fcapsType, networkElementFdn));
    }

    @Override
    public void notifyHb(String networkElementFdn) {
        setLastHeartbeatReceivedTimestamp(networkElementFdn, Instant.now().getEpochSecond());
        log.debug("Heartbeat received for node [{}]", networkElementFdn);
    }

    @Override
    public Set<String> getHbFailuresAndAcknowledge(FcapsType fcapsType) {
        Set<String> heartbeatFailures = getAllNetworkElements(fcapsType, FAILURE);
        if (heartbeatFailures.isEmpty()) {
            log.debug("No heartbeat failures to acknowledge for [{}]", fcapsType);
        } else {
            for (String networkElementFdn : heartbeatFailures) {
                setHeartbeatFailureAcknowledged(fcapsType, networkElementFdn, true);
            }
            log.info("Acknowledged heartbeat failures for [{}] are {}", fcapsType, heartbeatFailures);
        }
        return heartbeatFailures;
    }

    @Override
    public Set<String> getHbRecoveriesAndAcknowledge(FcapsType fcapsType) {
        Set<String> heartbeatRecoveries = getAllNetworkElements(fcapsType, RECOVERED);
        if (heartbeatRecoveries.isEmpty()) {
            log.debug("No heartbeat recoveries to acknowledge for [{}]", fcapsType);
        } else {
            for (String networkElementFdn : heartbeatRecoveries) {
                setHeartbeatFailureAcknowledged(fcapsType, networkElementFdn, false);
            }
            log.info("Acknowledged heartbeat recoveries for [{}] are {}", fcapsType, heartbeatRecoveries);
        }
        return heartbeatRecoveries;
    }

    @Override
    public Set<String> getHbFailures(FcapsType fcapsType) {
        return getAllNetworkElements(fcapsType, FAILURE, FAILURE_ACKNOWLEDGED);
    }

    protected Set<Registration> getAllRegistrations(FcapsType fcapsType) {
        return new HashSet<>(registrationCache.getAll(s -> fcapsType.equals(s.getValue().getFcapsType())).values());
    }

    protected Set<String> getAllNetworkElements(FcapsType fcapsType, O1HeartbeatState... heartbeatState) {
        Set<O1HeartbeatState> heartbeatStates = new HashSet<>(Arrays.asList(heartbeatState));
        Set<String> result = new HashSet<>();
        for (Registration registration : getAllRegistrations(fcapsType)) {
            String networkElementFdn = registration.getNetworkElementFdn();
            O1HeartbeatState state = registration.getHeartbeatState(heartbeatCache.get(networkElementFdn));
            if (heartbeatStates.contains(state)) {
                result.add(networkElementFdn);
            }
        }
        return result;
    }

    protected void setLastHeartbeatReceivedTimestamp(String networkElementFdn, long lastHeartbeatReceivedTimestamp) {
        heartbeatCache.put(networkElementFdn, lastHeartbeatReceivedTimestamp);
    }

    protected void setHeartbeatFailureAcknowledged(FcapsType fcapsType, String networkElementFdn, boolean heartbeatFailureAcknowledged) {
        registrationCache.update(getRegistrationKey(fcapsType, networkElementFdn), heartbeatDetails -> {
            heartbeatDetails.setHeartbeatFailureAcknowledged(heartbeatFailureAcknowledged);
            return null;
        });
        log.debug("Set heartbeatFailureAcknowledged [{}] for [{}]", heartbeatFailureAcknowledged,
                getRegistrationKey(fcapsType, networkElementFdn));
    }
}
