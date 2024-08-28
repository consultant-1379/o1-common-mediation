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

/**
 * FcapsType to be used to register to the O1 Heartbeat Agent with.
 */
public enum FcapsType {
    /**
     * Configuration Management.
     */
    CM,
    /**
     * Fault Management.
     */
    FM,
    /**
     * Performance Management.
     */
    PM
}
