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

/**
 * Heartbeat states a node can be in.
 * Heartbeat state cycle example:
 * <li>OK - Node gets registered with the agent</li>
 * <li>OK - Node sends heartbeats according to the configured interval</li>
 * <li>FAILURE - Node stopped sending heartbeats, time elapsed since last heartbeat is more then configured timeout</li>
 * <li>FAILURE_ACKNOWLEDGED - Heartbeat failure is acknowledged by the client</li>
 * <li>RECOVERED - Node recovered, and resumed sending heartbeats according to the configured interval</li>
 * <li>OK - Heartbeat recovery is acknowledged by the client</li>
 */
public enum O1HeartbeatState {
    /**
     * Heartbeat OK.
     * Time since last heartbeat received is LESS than the configured timeout
     * and if there was a recovery earlier, it is already acknowledged.
     */
    OK,
    /**
     * Heartbeat failure.
     * Time since last heartbeat received is MORE than the configured timeout
     * and the failure is not acknowledged yet.
     */
    FAILURE,
    /**
     * Heartbeat failure acknowledged.
     * Time since last heartbeat received is MORE than the configured timeout
     * and the failure is already acknowledged.
     */
    FAILURE_ACKNOWLEDGED,
    /**
     * Heartbeat recovery to acknowledge.
     * There was a heartbeat failure acknowledged earlier
     * and time since last heartbeat received is LESS than the configured timeout.
     */
    RECOVERED
}
