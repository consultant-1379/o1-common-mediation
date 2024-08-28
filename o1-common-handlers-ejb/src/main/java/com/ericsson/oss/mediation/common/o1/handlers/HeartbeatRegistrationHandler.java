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

package com.ericsson.oss.mediation.common.o1.handlers;

import java.time.Duration;

import org.apache.commons.lang3.StringUtils;

import org.slf4j.Logger;

import com.ericsson.oss.itpf.common.event.handler.annotation.EventHandler;
import com.ericsson.oss.itpf.sdk.core.annotation.EServiceRef;
import com.ericsson.oss.mediation.flow.events.MediationComponentEvent;
import com.ericsson.oss.mediation.o1.heartbeat.service.FcapsType;
import com.ericsson.oss.mediation.o1.heartbeat.service.O1HeartbeatAgent;
import com.ericsson.oss.mediation.o1.util.FdnUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * Handler responsible for managing heartbeat of the NetworkElement.
 * <p>
 * Turning on the supervision for the O1Node, will start monitoring the heartbeat and turning off the supervision will remove the monitoring of the
 * heartbeat.
 * The handler requires the following parameters:
 * <ul>
 * <li>fdn (String)</li>
 * <li>active (boolean)</li>
 * <li>heartbeatTimeout (integer)</li>
 * </ul>
 * For details please refer to the handler model.
 */
@Slf4j
@EventHandler
public class HeartbeatRegistrationHandler extends AbstractMediationHandler {

    public static final String ATTRIBUTE_HEARTBEAT_TIMEOUT = "heartbeatTimeout";
    public static final String ATTRIBUTE_FCAPS_TYPE = "fcapsType";

    @EServiceRef
    private O1HeartbeatAgent o1HeartbeatAgent;

    @Override
    public Object onEventWithResult(final Object inputEvent) {

        super.onEventWithResult(inputEvent);

        final String networkElementFdn = FdnUtil.getNetworkElementFdn(getHeaderFdn());
        final FcapsType fcapsType = FcapsType.valueOf(getHeader(ATTRIBUTE_FCAPS_TYPE));

        if (isSupervisionActive()) {
            log.debug("Registering network element [{}] with heartbeat agent", networkElementFdn);
            final int heartbeatTimeout = getHeader(ATTRIBUTE_HEARTBEAT_TIMEOUT);
            o1HeartbeatAgent.register(fcapsType, networkElementFdn, Duration.ofSeconds(heartbeatTimeout));
        } else {
            log.debug("Unregistering network element [{}] with heartbeat agent", networkElementFdn);
            o1HeartbeatAgent.unregister(fcapsType, networkElementFdn);
        }
        return new MediationComponentEvent(headers, StringUtils.EMPTY);
    }

    @Override
    protected Logger getLogger() {
        return log;
    }

}
