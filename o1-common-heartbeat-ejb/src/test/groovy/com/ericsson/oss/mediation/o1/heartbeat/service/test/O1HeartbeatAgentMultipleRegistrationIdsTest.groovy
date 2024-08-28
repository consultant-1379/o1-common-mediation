package com.ericsson.oss.mediation.o1.heartbeat.service.test

import com.ericsson.oss.mediation.o1.heartbeat.service.FcapsType
import com.ericsson.oss.mediation.o1.heartbeat.service.O1HeartbeatAgentImpl
import com.ericsson.oss.mediation.o1.heartbeat.service.O1HeartbeatState

import java.time.Duration
import java.time.Instant

import static com.ericsson.oss.mediation.o1.heartbeat.service.FcapsType.CM
import static com.ericsson.oss.mediation.o1.heartbeat.service.FcapsType.FM
import static com.ericsson.oss.mediation.o1.heartbeat.service.O1HeartbeatState.FAILURE
import static com.ericsson.oss.mediation.o1.heartbeat.service.O1HeartbeatState.FAILURE_ACKNOWLEDGED
import static com.ericsson.oss.mediation.o1.heartbeat.service.O1HeartbeatState.OK
import static com.ericsson.oss.mediation.o1.heartbeat.service.O1HeartbeatState.RECOVERED

class O1HeartbeatAgentMultipleRegistrationIdsTest extends O1HeartbeatAgentBase {

    def "Test register multiple registrations"() {
        when:
            o1HeartbeatAgent.register(FM, "NetworkElement=node1", Duration.ofSeconds(1))
            o1HeartbeatAgent.register(FM, "NetworkElement=node2", Duration.ofSeconds(2))
            o1HeartbeatAgent.register(CM, "NetworkElement=node1", Duration.ofSeconds(3))
        then:
            o1HeartbeatAgent.isRegistered(FM, "NetworkElement=node1")
            o1HeartbeatAgent.isRegistered(FM, "NetworkElement=node2")
            o1HeartbeatAgent.isRegistered(CM, "NetworkElement=node1")
            !o1HeartbeatAgent.isRegistered(CM, "NetworkElement=node2")
    }

    def "Test unregister multiple registrations"() {
        given:
            o1HeartbeatAgent.register(FM, "NetworkElement=node1", Duration.ofSeconds(1))
            o1HeartbeatAgent.register(CM, "NetworkElement=node1", Duration.ofSeconds(2))
        when:
            o1HeartbeatAgent.unregister(FM, "NetworkElement=node1")
        then:
            !o1HeartbeatAgent.isRegistered(FM, "NetworkElement=node1")
            o1HeartbeatAgent.isRegistered(CM, "NetworkElement=node1")
        when:
            o1HeartbeatAgent.unregister(CM, "NetworkElement=node1")
        then:
            !o1HeartbeatAgent.isRegistered(FM, "NetworkElement=node1")
            !o1HeartbeatAgent.isRegistered(CM, "NetworkElement=node1")
    }

    def "Test heartbeat failure for a node with 2 registrations and different timeouts"() {
        given:
            registerForTest(FM, "NetworkElement=node1", false, 10, Instant.now().getEpochSecond() - 11)
            registerForTest(CM, "NetworkElement=node1", false, 12, Instant.now().getEpochSecond() - 11)
        expect:
            o1HeartbeatAgent.getHbFailuresAndAcknowledge(FM) == ["NetworkElement=node1"].toSet()
            o1HeartbeatAgent.getHbFailuresAndAcknowledge(CM).isEmpty()
        when:
            sleep(2000)
        then:
            o1HeartbeatAgent.getHbFailuresAndAcknowledge(FM).isEmpty()
            o1HeartbeatAgent.getHbFailuresAndAcknowledge(CM) == ["NetworkElement=node1"].toSet()
    }

    def "Test heartbeat failure acknowledge per registrations"() {
        given:
            registerForTest(FM, "NetworkElement=node1", FAILURE)
            registerForTest(FM, "NetworkElement=node2", FAILURE)
            registerForTest(FM, "NetworkElement=node3", OK)
        and:
            registerForTest(CM, "NetworkElement=node1", FAILURE)
            registerForTest(CM, "NetworkElement=node4", RECOVERED)
            registerForTest(CM, "NetworkElement=node5", FAILURE_ACKNOWLEDGED)
        expect:
            o1HeartbeatAgent.getHbFailuresAndAcknowledge(FM) == ["NetworkElement=node1", "NetworkElement=node2"].toSet()
            o1HeartbeatAgent.getHbFailuresAndAcknowledge(CM) == ["NetworkElement=node1"].toSet()
        and:
            getAllNetworkElements(FM, FAILURE_ACKNOWLEDGED) == ["NetworkElement=node1", "NetworkElement=node2"].toSet()
            getAllNetworkElements(CM, FAILURE_ACKNOWLEDGED) == ["NetworkElement=node1", "NetworkElement=node5"].toSet()
    }

    def "Test heartbeat recovery acknowledge per registrations"() {
        given:
            registerForTest(FM, "NetworkElement=node1", RECOVERED)
            registerForTest(FM, "NetworkElement=node2", RECOVERED)
            registerForTest(FM, "NetworkElement=node3", OK)
        and:
            registerForTest(CM, "NetworkElement=node1", RECOVERED)
            registerForTest(CM, "NetworkElement=node4", FAILURE)
            registerForTest(CM, "NetworkElement=node5", FAILURE_ACKNOWLEDGED)
        expect:
            o1HeartbeatAgent.getHbRecoveriesAndAcknowledge(FM) == ["NetworkElement=node1", "NetworkElement=node2"].toSet()
            o1HeartbeatAgent.getHbRecoveriesAndAcknowledge(CM) == ["NetworkElement=node1"].toSet()
        and:
            getAllNetworkElements(FM, OK) == ["NetworkElement=node1", "NetworkElement=node2", "NetworkElement=node3"].toSet()
            getAllNetworkElements(CM, OK) == ["NetworkElement=node1"].toSet()
    }

    def "Test multiple subscriptions with failures and recoveries"() {
        given:
            registerForTest(FM, "NetworkElement=node1", false, 10, Instant.now().getEpochSecond() - 20)
            registerForTest(FM, "NetworkElement=node2", false, 10, Instant.now().getEpochSecond() - 20)
            registerForTest(CM, "NetworkElement=node1", false, 15, Instant.now().getEpochSecond() - 20)
            registerForTest(CM, "NetworkElement=node2", false, 30, Instant.now().getEpochSecond() - 20)
        expect:
            o1HeartbeatAgent.getHbFailuresAndAcknowledge(FM) == ["NetworkElement=node1", "NetworkElement=node2"].toSet()
            o1HeartbeatAgent.getHbFailuresAndAcknowledge(CM) == ["NetworkElement=node1"].toSet()
        when:
            o1HeartbeatAgent.notifyHb("NetworkElement=node1")
        then:
            o1HeartbeatAgent.getHbRecoveriesAndAcknowledge(FM) == ["NetworkElement=node1"].toSet()
            o1HeartbeatAgent.getHbRecoveriesAndAcknowledge(CM) == ["NetworkElement=node1"].toSet()
        when:
            o1HeartbeatAgent.notifyHb("NetworkElement=node2")
        then:
            o1HeartbeatAgent.getHbRecoveriesAndAcknowledge(FM) == ["NetworkElement=node2"].toSet()
            o1HeartbeatAgent.getHbRecoveriesAndAcknowledge(CM).isEmpty()
    }

    private Set<String> getAllNetworkElements(FcapsType fcapsType, O1HeartbeatState... heartbeatStates) {
        return ((O1HeartbeatAgentImpl) o1HeartbeatAgent).getAllNetworkElements(fcapsType, heartbeatStates)
    }
}
