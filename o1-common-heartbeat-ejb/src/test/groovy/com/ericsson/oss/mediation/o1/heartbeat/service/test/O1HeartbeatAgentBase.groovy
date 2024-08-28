package com.ericsson.oss.mediation.o1.heartbeat.service.test

import com.ericsson.cds.cdi.support.providers.stubs.InMemoryCache
import com.ericsson.cds.cdi.support.rule.ImplementationClasses
import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.itpf.sdk.cache.classic.CacheProviderBean
import com.ericsson.oss.mediation.o1.heartbeat.service.FcapsType
import com.ericsson.oss.mediation.o1.heartbeat.service.O1HeartbeatAgent
import com.ericsson.oss.mediation.o1.heartbeat.service.O1HeartbeatAgentImpl
import com.ericsson.oss.mediation.o1.heartbeat.service.O1HeartbeatState
import com.ericsson.oss.mediation.o1.heartbeat.service.Registration

import javax.inject.Inject
import java.time.Duration

import static com.ericsson.oss.mediation.o1.heartbeat.service.O1HeartbeatAgentImpl.HEARTBEAT_CACHE_NAME
import static com.ericsson.oss.mediation.o1.heartbeat.service.O1HeartbeatAgentImpl.REGISTRATION_CACHE_NAME
import static com.ericsson.oss.mediation.o1.heartbeat.service.O1HeartbeatState.FAILURE
import static com.ericsson.oss.mediation.o1.heartbeat.service.O1HeartbeatState.FAILURE_ACKNOWLEDGED
import static com.ericsson.oss.mediation.o1.heartbeat.service.O1HeartbeatState.RECOVERED

class O1HeartbeatAgentBase extends CdiSpecification {

    @ImplementationClasses
    def classes = [O1HeartbeatAgentImpl]

    @Inject
    O1HeartbeatAgent o1HeartbeatAgent

    @MockedImplementation
    CacheProviderBean cacheProviderBean

    def setup() {
        cacheProviderBean.createOrGetModeledCache(HEARTBEAT_CACHE_NAME) >> new InMemoryCache<String, Long>(HEARTBEAT_CACHE_NAME)
        cacheProviderBean.createOrGetModeledCache(REGISTRATION_CACHE_NAME) >> new InMemoryCache<String, Registration>(REGISTRATION_CACHE_NAME)
        ((O1HeartbeatAgentImpl) o1HeartbeatAgent).initializeCacheName()
    }

    protected registerForTest(FcapsType fcapsType, String networkElementFdn, boolean heartbeatFailureAcknowledged, long timeout, long heartbeatReceivedTimestamp) {
        o1HeartbeatAgent.register(fcapsType, networkElementFdn, Duration.ofSeconds(timeout))
        ((O1HeartbeatAgentImpl) o1HeartbeatAgent).setLastHeartbeatReceivedTimestamp(networkElementFdn, heartbeatReceivedTimestamp)
        ((O1HeartbeatAgentImpl) o1HeartbeatAgent).setHeartbeatFailureAcknowledged(fcapsType, networkElementFdn, heartbeatFailureAcknowledged)
    }

    protected registerForTest(FcapsType fcapsType, String networkElementFdn, O1HeartbeatState heartbeatState) {
        o1HeartbeatAgent.register(fcapsType, networkElementFdn, Duration.ofSeconds(10))
        switch (heartbeatState) {
            case FAILURE_ACKNOWLEDGED:
                ((O1HeartbeatAgentImpl) o1HeartbeatAgent).setHeartbeatFailureAcknowledged(fcapsType, networkElementFdn, true)
                ((O1HeartbeatAgentImpl) o1HeartbeatAgent).setLastHeartbeatReceivedTimestamp(networkElementFdn, 0)
                break
            case RECOVERED:
                ((O1HeartbeatAgentImpl) o1HeartbeatAgent).setHeartbeatFailureAcknowledged(fcapsType, networkElementFdn, true)
                break
            case FAILURE:
                ((O1HeartbeatAgentImpl) o1HeartbeatAgent).setLastHeartbeatReceivedTimestamp(networkElementFdn, 0)
                break
        }
    }
}
