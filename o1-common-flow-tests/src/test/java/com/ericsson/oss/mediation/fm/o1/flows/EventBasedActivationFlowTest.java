package com.ericsson.oss.mediation.fm.o1.flows;

import com.ericsson.oss.mediation.sdk.event.MediationTaskRequest;

import static com.ericsson.oss.mediation.o1.heartbeat.service.FcapsType.PM;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import static com.ericsson.oss.mediation.o1.heartbeat.service.FcapsType.FM;
import static com.ericsson.oss.mediation.util.netconf.api.editconfig.Operation.CREATE;
import static com.ericsson.oss.mediation.util.netconf.api.editconfig.Operation.DELETE;
import static com.ericsson.oss.mediation.util.netconf.api.editconfig.Operation.MERGE;

import org.junit.Test;

import com.ericsson.oss.mediation.adapter.netconf.jca.api.operation.NetconfOperationRequest;
import com.ericsson.oss.mediation.engine.flow.FlowProcessingException;
import com.ericsson.oss.mediation.engine.test.flow.FlowRef;

import java.util.Arrays;

@FlowRef(flowName = "TestEventBasedActivationFlow", version = "1.0.0", namespace = "O1_MED")
public class EventBasedActivationFlowTest extends O1BaseFlowTest {

    @Test
    public void test_execute_flow_activate_success() throws Exception {
        // GIVEN
        dpsNodeCreator.createDpsMos("O1Node_Test", ACTIVE_TRUE);
        // WHEN
        invokeFlow(createMediationTaskRequest("O1Node_Test"));

        // THEN changes are mediated
        assertYangOperations(setDnPrefix(),
            createNtfSubscriptionControl(), createHeartbeatControl());

        // AND Heartbeat registered with HeartbeatAgent
        assertTrue(o1HeartbeatAgent.isRegistered(PM, "NetworkElement=O1Node_Test"));
    }

    public static SimpleNetconfOperation setDnPrefix() {
        return SimpleNetconfOperation.builder().operation(MERGE)
            .fdn("MeContext=O1Node_Test,ManagedElement=ocp83vcu03o1")
            .attribute("attributes")
            .attribute("dnPrefix", "MeContext=O1Node_Test").build();
    }

    public static SimpleNetconfOperation createNtfSubscriptionControl() {
        return SimpleNetconfOperation.builder().operation(CREATE)
            .fdn("MeContext=O1Node_Test,ManagedElement=ocp83vcu03o1,NtfSubscriptionControl=ENMPM")
            .attribute("attributes")
            .attribute("notificationRecipientAddress", "http://1.2.3.4:8099/pm/eventListener/v1")
            .attribute("notificationTypes", Arrays.asList("notifyFileReady", "notifyFilePreparationError"))
            .build();
    }

    public static MediationTaskRequest createMediationTaskRequest(final String nodeName) {
        final MediationTaskRequest request = new MediationTaskRequest();
        request.setClientType("EVENT_BASED");
        request.setNodeAddress("NetworkElement=" + nodeName);
        return request;
    }

    protected static SimpleNetconfOperation createHeartbeatControl() {
        return SimpleNetconfOperation.builder().operation(CREATE)
            .fdn("MeContext=O1Node_Test,ManagedElement=ocp83vcu03o1,NtfSubscriptionControl=ENMPM,HeartbeatControl=1")
            .attribute("attributes")
            .attribute("heartbeatNtfPeriod", "450").build();
    }
}
