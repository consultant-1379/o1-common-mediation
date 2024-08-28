package com.ericsson.oss.mediation.o1.heartbeat.service.test

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.time.Duration

import static com.ericsson.oss.mediation.o1.heartbeat.service.FcapsType.FM
import static org.awaitility.Awaitility.await

class O1HeartbeatAgentWithThreadsTest extends O1HeartbeatAgentBase {

    private static final Logger log = LoggerFactory.getLogger(O1HeartbeatAgentWithThreadsTest.class);

    List<String> currentFailures = new ArrayList<>()

    def setup() {
        acknowledgeThread(Duration.ofMillis(500)).start()
    }

    def "Test single node heartbeat failure and recovery"() {
        given:
            Thread node1 = nodeHeartbeatThread("node1", Duration.ofMillis(300))
        when:
            o1HeartbeatAgent.register(FM, "node1", Duration.ofSeconds(1))
        then:
            assertFailures("node1")
        when:
            node1.start()
        then:
            assertFailures()
        when:
            node1.interrupt()
        then:
            assertFailures("node1")
    }

    def "Test multiple node heartbeat ok"() {
        given:
            o1HeartbeatAgent.register(FM, "node1", Duration.ofSeconds(1))
            nodeHeartbeatThread("node1", Duration.ofMillis(300)).start()
        and:
            o1HeartbeatAgent.register(FM, "node2", Duration.ofSeconds(2))
            nodeHeartbeatThread("node2", Duration.ofMillis(800)).start()
        and:
            o1HeartbeatAgent.register(FM, "node3", Duration.ofSeconds(3))
            nodeHeartbeatThread("node3", Duration.ofMillis(1500)).start()
        expect:
            for (int i = 0; i < 8; i++) {
                assert currentFailures.isEmpty()
                sleep(1000)
            }
    }

    void assertFailures(String... expected) {
        await().atMost(Duration.ofSeconds(10)).untilAsserted({ ->
            assert currentFailures == Arrays.asList(expected)
        })
    }

    private Thread acknowledgeThread(Duration interval) {
        return new Thread({ ->
            while (true) {
                def failures = o1HeartbeatAgent.getHbFailuresAndAcknowledge(FM)
                log.info("{} failures acknowledged: {}", failures.size(), failures)
                currentFailures.addAll(failures)

                def recoveries = o1HeartbeatAgent.getHbRecoveriesAndAcknowledge(FM)
                log.info("{} recoveries acknowledged: {}", recoveries.size(), recoveries)
                currentFailures.removeAll(recoveries)

                sleep(interval.toMillis());
            }
        })
    }

    private Thread nodeHeartbeatThread(String nodeName, Duration interval) {
        return new Thread() {
            boolean run

            @Override
            void run() {
                run = true
                while (run) {
                    o1HeartbeatAgent.notifyHb(nodeName)
                    sleep(interval.toMillis());
                }
            }

            @Override
            void interrupt() {
                run = false
            }
        }
    }
}

