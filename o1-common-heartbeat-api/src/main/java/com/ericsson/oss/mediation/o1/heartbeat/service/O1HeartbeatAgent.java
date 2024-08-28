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

import java.time.Duration;
import java.util.Set;

import javax.ejb.Remote;

import com.ericsson.oss.itpf.sdk.core.annotation.EService;

/**
 * O1HeartbeatAgent is a stateful agent that can be used to monitor heartbeat.
 * Network Elements can be registered per {@code FcapsType} (eg "FM") with the agent, and the agent can be notified for heartbeat.
 * Then the heartbeat failures and recoveries can be queried and acknowledged for each FCAPS Type, so they can act on those.
 */
@Remote
@EService
public interface O1HeartbeatAgent {

    /**
     * Registers a node {@code networkElementFdn} with the heartbeat agent.
     *
     * @param fcapsType
     *            Fcaps Type to register the Network Element with, for example FM.
     * @param networkElementFdn
     *            The FDN of the nodes NetworkElement MO
     * @param heartbeatTimeout
     *            heartbeatTimeout duration for particular Network element
     */
    void register(FcapsType fcapsType, String networkElementFdn, Duration heartbeatTimeout);

    /**
     * Unregisters a node {@code networkElementFdn} with the heartbeat agent.
     *
     * @param fcapsType
     *            Fcaps Type to unregister the Network Element with, for example FM.
     * @param networkElementFdn
     *            The FDN of the nodes NetworkElement MO
     */
    void unregister(FcapsType fcapsType, String networkElementFdn);

    /**
     * Checks if a node {@code networkElementFdn} is registered with the heartbeat agent.
     *
     * @param fcapsType
     *            The {@code FcapsType}
     * @param networkElementFdn
     *            The FDN of the nodes NetworkElement MO
     */
    boolean isRegistered(FcapsType fcapsType, String networkElementFdn);

    /**
     * Notifies the agent, that a heartbeat was received for a node {@code networkElementFdn}.
     *
     * @param networkElementFdn
     *            The FDN of the nodes NetworkElement MO
     */
    void notifyHb(String networkElementFdn);

    /**
     * Get unacknowledged Heartbeat failures and acknowledge them.
     *
     * @param fcapsType
     *            The {@code FcapsType}
     * @return list of nodes {@code networkElementFdn} with heartbeat failures acknowledged.
     */
    Set<String> getHbFailuresAndAcknowledge(FcapsType fcapsType);

    /**
     * Get unacknowledged Heartbeat recoveries and acknowledge them.
     *
     * @param fcapsType
     *            The {@code FcapsType}
     * @return list of nodes {@code networkElementFdn} with heartbeat recovery acknowledged.
     */
    Set<String> getHbRecoveriesAndAcknowledge(FcapsType fcapsType);

    /**
     * Get heartbeat failures.
     *
     * @param fcapsType
     *            The {@code FcapsType}
     * @return list of nodes {@code networkElementFdn} with heartbeat recovery acknowledged.
     */
    Set<String> getHbFailures(FcapsType fcapsType);

}
