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
package com.ericsson.oss.mediation.o1.heartbeat.service.test

import java.time.Duration

import static com.ericsson.oss.mediation.o1.heartbeat.service.FcapsType.FM
import static com.ericsson.oss.mediation.o1.heartbeat.service.O1HeartbeatState.FAILURE
import static com.ericsson.oss.mediation.o1.heartbeat.service.O1HeartbeatState.FAILURE_ACKNOWLEDGED
import static com.ericsson.oss.mediation.o1.heartbeat.service.O1HeartbeatState.OK
import static com.ericsson.oss.mediation.o1.heartbeat.service.O1HeartbeatState.RECOVERED

class O1HeartbeatAgentSpec extends O1HeartbeatAgentBase {

    def "Test register two nodes"() {
        given:
            !o1HeartbeatAgent.isRegistered(FM, "node1")
            !o1HeartbeatAgent.isRegistered(FM, "node1")
        when:
            o1HeartbeatAgent.register(FM, "node1", Duration.ofSeconds(5))
        then:
            o1HeartbeatAgent.isRegistered(FM, "node1")
        when:
            o1HeartbeatAgent.register(FM, "node2", Duration.ofSeconds(10))
        then:
            o1HeartbeatAgent.isRegistered(FM, "node2")
    }

    def "Test unregister a registered node"() {
        given:
            o1HeartbeatAgent.register(FM, "node1", Duration.ofSeconds(10))
            o1HeartbeatAgent.isRegistered(FM, "node1")
        when:
            o1HeartbeatAgent.unregister(FM, "node1")
        then:
            !o1HeartbeatAgent.isRegistered(FM, "node1")
    }

    def "Test unregister a non-registered node"() {
        when:
            o1HeartbeatAgent.unregister(FM, "node1")
        then:
            !o1HeartbeatAgent.isRegistered(FM, "node1")
    }

    def "Test heartbeat for a node with heartbeat failure"() {
        given:
            registerForTest(FM, "node1", FAILURE)
            o1HeartbeatAgent.getHbFailures(FM) == ["node"].toSet()
        when:
            o1HeartbeatAgent.notifyHb("node1")
        then:
            o1HeartbeatAgent.getHbFailures(FM).isEmpty()
    }

    def "Test heartbeat for a node without heartbeat failure"() {
        given:
            o1HeartbeatAgent.register(FM, "node1", Duration.ofSeconds(10))
            !o1HeartbeatAgent.getHbFailures(FM) == ["node1"].toSet()
        when:
            o1HeartbeatAgent.notifyHb("node1")
        then:
            o1HeartbeatAgent.getHbFailures(FM).isEmpty()
    }

    def "Test heartbeat non-registered node"() {
        when:
            o1HeartbeatAgent.notifyHb("non-registered")
        then:
            !o1HeartbeatAgent.isRegistered(FM, "non-registered")
    }

    def "Test heartbeat failure acknowledge"() {
        given:
            registerForTest(FM, "node1", FAILURE_ACKNOWLEDGED)
            registerForTest(FM, "node2", FAILURE)
            registerForTest(FM, "node3", RECOVERED)
            registerForTest(FM, "node4", OK)
        when:
            Set<String> failures = o1HeartbeatAgent.getHbFailuresAndAcknowledge(FM)
        then:
            failures == ["node2"].toSet()
    }

    def "Test heartbeat recovery acknowledge"() {
        given:
            registerForTest(FM, "node1", FAILURE_ACKNOWLEDGED)
            registerForTest(FM, "node2", FAILURE)
            registerForTest(FM, "node3", RECOVERED)
            registerForTest(FM, "node4", OK)
        when:
            Set<String> recoveries = o1HeartbeatAgent.getHbRecoveriesAndAcknowledge(FM)
        then:
            recoveries == ["node3"].toSet()
    }

    def "Test subsequent acknowledgements returning only new failures to acknowledge"() {
        given:
            registerForTest(FM, "node1", FAILURE_ACKNOWLEDGED)
            registerForTest(FM, "node2", FAILURE)
            registerForTest(FM, "node3", RECOVERED)
            registerForTest(FM, "node4", OK)
        and:
            Set<String> failures = o1HeartbeatAgent.getHbFailuresAndAcknowledge(FM)
            failures == ["node1"].toSet()
        and:
            registerForTest(FM, "node5", FAILURE)
        when:
            failures = o1HeartbeatAgent.getHbFailuresAndAcknowledge(FM)
        then:
            failures == ["node5"].toSet()
        when:
            failures = o1HeartbeatAgent.getHbFailuresAndAcknowledge(FM)
        then:
            failures.isEmpty()
    }

    def "Test subsequent acknowledgements returning only new recoveries to acknowledge"() {
        given:
            registerForTest(FM, "node1", FAILURE_ACKNOWLEDGED)
            registerForTest(FM, "node2", FAILURE)
            registerForTest(FM, "node3", RECOVERED)
            registerForTest(FM, "node4", OK)
        and:
            Set<String> recoveries = o1HeartbeatAgent.getHbRecoveriesAndAcknowledge(FM)
            recoveries == ["node3"].toSet()
        and:
            o1HeartbeatAgent.notifyHb("node1")
        when:
            recoveries = o1HeartbeatAgent.getHbRecoveriesAndAcknowledge(FM)
        then:
            recoveries == ["node1"].toSet()
        when:
            recoveries = o1HeartbeatAgent.getHbRecoveriesAndAcknowledge(FM)
        then:
            recoveries.isEmpty()
    }
}
