
package com.ericsson.oss.mediation.fm.o1.flows;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import static com.ericsson.oss.mediation.o1.heartbeat.service.FcapsType.FM;
import static com.ericsson.oss.mediation.util.netconf.api.editconfig.Operation.CREATE;
import static com.ericsson.oss.mediation.util.netconf.api.editconfig.Operation.DELETE;
import static com.ericsson.oss.mediation.util.netconf.api.editconfig.Operation.MERGE;

import java.time.Duration;
import java.util.Arrays;

import org.junit.Test;

import com.ericsson.oss.mediation.adapter.netconf.jca.api.operation.NetconfOperationRequest;
import com.ericsson.oss.mediation.engine.flow.FlowProcessingException;
import com.ericsson.oss.mediation.engine.test.flow.FlowRef;
import com.ericsson.oss.mediation.sdk.event.SupervisionMediationTaskRequest;
import com.google.common.collect.ImmutableMap;

@FlowRef(flowName = "TestSupervisionFlow", version = "1.0.0", namespace = "O1_MED")
public class SupervisionFlowTest extends O1BaseFlowTest {

    @Test
    public void test_execute_flow_activate_success() throws Exception {
        // GIVEN
        dpsNodeCreator.createDpsMos("O1Node_Test", ACTIVE_TRUE);
        // WHEN
        invokeFlow(createSupervisionMediationTaskRequest("O1Node_Test", ACTIVE_TRUE));

        // THEN changes are mediated
        assertYangOperations(setDnPrefix(),
                createNtfSubscriptionControl(), createHeartbeatControl());

        // AND Heartbeat registered with HeartbeatAgent
        assertTrue(o1HeartbeatAgent.isRegistered(FM, "NetworkElement=O1Node_Test"));
    }

    @Test
    public void test_execute_flow_deactivate_success() throws Exception {
        // GIVEN
        dpsNodeCreator.createDpsMos("O1Node_Test", ACTIVE_FALSE);
        o1HeartbeatAgent.register(FM, "NetworkElement=O1Node_Test", Duration.ofSeconds(5));
        // WHEN
        invokeFlow(createSupervisionMediationTaskRequest("O1Node_Test", ACTIVE_FALSE));

        // THEN changes are mediated
        assertYangOperations(deleteNtfSubscriptionControl());

        // AND Heartbeat unregistered with HeartbeatAgent
        assertFalse(o1HeartbeatAgent.isRegistered(FM, "NetworkElement=O1Node_Test"));
    }

    @Test(expected = FlowProcessingException.class)
    public void test_activate_flow_netconf_error() throws Exception {
        // GIVEN
        dpsNodeCreator.createDpsMos("O1Node_Test", ACTIVE_TRUE);
        when(netconfManagerOperation.executeXAResourceOperation(any(NetconfOperationRequest.class))).thenReturn(null);
        // WHEN
        try {
            invokeFlow(createSupervisionMediationTaskRequest("O1Node_Test", ACTIVE_TRUE));
        } finally {
            // THEN
            assertYangOperations(setDnPrefix());
        }
    }

    public static SimpleNetconfOperation setDnPrefix() {
        return SimpleNetconfOperation.builder().operation(MERGE)
                .fdn("MeContext=O1Node_Test,ManagedElement=ocp83vcu03o1")
                .attribute("attributes")
                .attribute("dnPrefix", "MeContext=O1Node_Test").build();
    }

    public static SimpleNetconfOperation createNtfSubscriptionControl() {
        return SimpleNetconfOperation.builder().operation(CREATE)
                .fdn("MeContext=O1Node_Test,ManagedElement=ocp83vcu03o1,NtfSubscriptionControl=ENMFM")
                .attribute("attributes")
                .attribute("notificationRecipientAddress", "http://1.2.3.4:8099/FM/eventListener/v1")
                .attribute("notificationTypes", Arrays.asList(
                        "notifyChangedAlarm", "notifyNewAlarm", "notifyChangedAlarmGeneral",
                        "notifyClearedAlarm", "notifyAlarmListRebuilt"))
                .build();
    }

    public static SimpleNetconfOperation deleteNtfSubscriptionControl() {
        return SimpleNetconfOperation.builder().operation(DELETE)
                .fdn("MeContext=O1Node_Test,ManagedElement=ocp83vcu03o1,NtfSubscriptionControl=ENMFM")
                .build();
    }

    public static SupervisionMediationTaskRequest createSupervisionMediationTaskRequest(final String nodeName, boolean active) {
        final SupervisionMediationTaskRequest request = new SupervisionMediationTaskRequest();
        request.setJobId("JOB_ID_1");
        request.setClientType("SUPERVISION");
        request.setNodeAddress("NetworkElement=" + nodeName + ",FmAlarmSupervision=1");
        request.setSupervisionAttributes(ImmutableMap.of("active", active));
        return request;
    }

    protected static SimpleNetconfOperation createHeartbeatControl() {
        return SimpleNetconfOperation.builder().operation(CREATE)
                .fdn("MeContext=O1Node_Test,ManagedElement=ocp83vcu03o1,NtfSubscriptionControl=ENMFM,HeartbeatControl=1")
                .attribute("attributes")
                .attribute("heartbeatNtfPeriod", "100").build();
    }
}
