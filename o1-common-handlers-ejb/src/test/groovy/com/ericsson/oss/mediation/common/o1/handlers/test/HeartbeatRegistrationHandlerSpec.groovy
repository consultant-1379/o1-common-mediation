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

package com.ericsson.oss.mediation.common.o1.handlers.test

import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.itpf.common.config.Configuration
import com.ericsson.oss.itpf.common.event.handler.EventHandlerContext
import com.ericsson.oss.mediation.common.o1.handlers.HeartbeatRegistrationHandler
import com.ericsson.oss.mediation.flow.events.MediationComponentEvent
import com.ericsson.oss.mediation.o1.heartbeat.service.O1HeartbeatAgent

import javax.inject.Inject

import static com.ericsson.oss.mediation.common.o1.handlers.HeartbeatRegistrationHandler.ATTRIBUTE_FCAPS_TYPE
import static com.ericsson.oss.mediation.common.o1.handlers.HeartbeatRegistrationHandler.ATTRIBUTE_HEARTBEAT_TIMEOUT
import static com.ericsson.oss.mediation.o1.heartbeat.service.FcapsType.FM

class HeartbeatRegistrationHandlerSpec extends CdiSpecification {

    private static final String NETWORK_ELEMENT_FDN = "NetworkElement=NodeA"
    private static final String FM_ALARM_SUPERVISION_FDN = NETWORK_ELEMENT_FDN + ",FmAlarmSupervision=1"

    @ObjectUnderTest
    private HeartbeatRegistrationHandler heartbeatRegistrationHandler

    @Inject
    private O1HeartbeatAgent o1HeartbeatAgent

    final Map<String, Object> headers = new HashMap<>()
    protected EventHandlerContext mockEventHandlerContext = Mock()

    def setup() {
        def mockConfiguration = mock(Configuration)
        mockEventHandlerContext.getEventHandlerConfiguration() >> mockConfiguration
        mockConfiguration.getAllProperties() >> headers
    }

    def "Test when supervision active sends a request to add heartbeat management for network element"() {
        given: "Headers for event with all required attributes"
            headers.put("supervisionAttributes", ["active": true])
            headers.put("fdn", FM_ALARM_SUPERVISION_FDN)
            headers.put(ATTRIBUTE_FCAPS_TYPE, FM.toString())
            headers.put(ATTRIBUTE_HEARTBEAT_TIMEOUT, 300)

        and: "Handler input contains the headers"
            def inputEvent = new MediationComponentEvent(headers, null)
            heartbeatRegistrationHandler.init(mockEventHandlerContext)

        when: "The handler is executed with the input event"
            heartbeatRegistrationHandler.onEventWithResult(inputEvent)

        then: "validate that the heartbeat management is registered succesfully"
            1 * heartbeatRegistrationHandler.o1HeartbeatAgent.register(FM, NETWORK_ELEMENT_FDN, _)
    }

    def "Test when supervision not active sends a request to remove heartbeat management for network element "() {
        given: "Headers for event with all required attributes"
            headers.put("supervisionAttributes", ["active": false])
            headers.put("fdn", FM_ALARM_SUPERVISION_FDN)
            headers.put(ATTRIBUTE_FCAPS_TYPE, FM.toString())
            headers.put(ATTRIBUTE_HEARTBEAT_TIMEOUT, 300)

        and: "Handler input contains the headers"
            def inputEvent = new MediationComponentEvent(headers, null)
            heartbeatRegistrationHandler.init(mockEventHandlerContext)

        when: "The handler is executed with the input event"
            heartbeatRegistrationHandler.onEventWithResult(inputEvent)

        then: "validate that heartbeat management is unregistered successfully"
            1 * o1HeartbeatAgent.unregister(FM, NETWORK_ELEMENT_FDN)
    }

}
