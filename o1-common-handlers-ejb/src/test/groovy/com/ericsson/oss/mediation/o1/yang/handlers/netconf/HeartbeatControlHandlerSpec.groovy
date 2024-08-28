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
package com.ericsson.oss.mediation.o1.yang.handlers.netconf

import static com.ericsson.oss.mediation.o1.common.handlers.netconf.O1NetconfOperationResult.OPERATION_ABORTED
import static com.ericsson.oss.mediation.o1.common.handlers.netconf.O1NetconfOperationResult.OPERATION_FAILED
import static com.ericsson.oss.mediation.o1.common.handlers.netconf.O1NetconfOperationResult.OPERATION_SUCCESS

import org.apache.commons.lang3.StringUtils

import com.ericsson.cds.cdi.support.configuration.InjectionProperties
import com.ericsson.cds.cdi.support.providers.custom.model.ModelPattern
import com.ericsson.cds.cdi.support.providers.custom.model.local.LocalModelServiceProvider
import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.itpf.common.config.Configuration
import com.ericsson.oss.itpf.common.event.handler.EventHandlerContext
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject
import com.ericsson.oss.itpf.datalayer.dps.stub.RuntimeConfigurableDps
import com.ericsson.oss.mediation.adapter.netconf.jca.xa.yang.provider.YangNetconfOperationResult
import com.ericsson.oss.mediation.flow.events.MediationComponentEvent
import com.ericsson.oss.mediation.o1.common.handlers.netconf.O1NetconfOperationResult
import com.ericsson.oss.mediation.o1.common.handlers.netconf.O1NetconfOperationStatus
import com.ericsson.oss.mediation.o1.common.handlers.netconf.O1NetconfOperationsStatus
import com.ericsson.oss.mediation.o1.common.handlers.netconf.O1NetconfOperationsStatusHelper

class HeartbeatControlHandlerSpec extends CdiSpecification {

    private static final boolean SUPERVISION_ON = true;
    private static final boolean SUPERVISION_OFF = false;

    @ObjectUnderTest
    private HeartbeatControlHandler heartbeatControlHandler

    private final Map<String, Object> headers = new HashMap<>()

    @MockedImplementation
    private EventHandlerContext mockEventHandlerContext

    @MockedImplementation
    private NetconfManagerOperation netconfManagerOperation

    private static filteredModels = [
            new ModelPattern('.*', '.*', '.*', '.*')
    ]

    private static LocalModelServiceProvider localModelServiceProvider = new LocalModelServiceProvider(filteredModels)

    @Override
    def addAdditionalInjectionProperties(InjectionProperties injectionProperties) {
        super.addAdditionalInjectionProperties(injectionProperties)
        injectionProperties.addInjectionProvider(localModelServiceProvider)
    }

    def setup() {
        def mockConfiguration = mock(Configuration)
        mockEventHandlerContext.getEventHandlerConfiguration() >> mockConfiguration
        mockConfiguration.getAllProperties() >> headers
        createObjects("TestNode")
    }

    def "Test handler is aborted when previous handler has failed failed"() {
        given: "Prepare headers for event"
            prepareO1HandlerFailed()

        and: "a valid netconf manager provided"
            headers.put('netconfManager', netconfManagerOperation)

        and: "setup input event and initialise HeartbeatControlHandler"
            def inputEvent = new MediationComponentEvent(headers, null)
            heartbeatControlHandler.init(mockEventHandlerContext)

        when: "invoke onEvent with input"
            def outputEvent = heartbeatControlHandler.onEventWithResult(inputEvent)

        then: "Underlying netconf library is not called"
            0 * netconfManagerOperation.executeXAResourceOperation(_)

        and: "handler operation is set to operation aborted"
            verifyOperation(outputEvent.getHeaders(), O1NetconfOperationResult.OPERATION_ABORTED, null)
    }


    def "Test when supervision enabled then HeartbeatControl is created"() {
        given: "Prepare headers for event"
            prepareO1HandlersSuccess()
            prepareHeaders(SUPERVISION_ON)

        and: "a valid netconf manager provided"
            headers.put('netconfManager', netconfManagerOperation)

        and: "setup input event and initialise NtfSubscriptionControl"
            def inputEvent = new MediationComponentEvent(headers, null)
            heartbeatControlHandler.init(mockEventHandlerContext)

        when: "invoke onEvent with input"
            def outputEvent = heartbeatControlHandler.onEventWithResult(inputEvent)

        then: "Underlying netconf library was called with the expected request details"
            1 * netconfManagerOperation.executeXAResourceOperation({ it.toString().contains(getExpectedRequestDetails()) }) >>
                    YangNetconfOperationResult.OK

        and: "handler operation is set to operation success"
            verifyOperation(outputEvent.getHeaders(), O1NetconfOperationResult.OPERATION_SUCCESS, null)
    }

    def "Test when supervision disabled then HeartbeatControl creation is skipped"() {
        given: "Prepare headers for event with all required attributes"
            prepareO1HandlersSuccess()
            prepareHeaders(SUPERVISION_OFF)

        and: "a valid netconf manager provided"
            headers.put('netconfManager', netconfManagerOperation)

        and: "setup input event and initialise NtfSubscriptionControl"
            def inputEvent = new MediationComponentEvent(headers, null)
            heartbeatControlHandler.init(mockEventHandlerContext)

        when: "invoke onEvent with input"
            def outputEvent = heartbeatControlHandler.onEventWithResult(inputEvent)

        then: "Underlying netconf library was called with the expected request details"
            0 * netconfManagerOperation.executeXAResourceOperation({ it.toString().contains(getExpectedRequestDetails()) }) >>
                    YangNetconfOperationResult.OK

        and: "handler operation is set to operation success"
            verifyOperation(outputEvent.getHeaders(), O1NetconfOperationResult.OPERATION_ABORTED, null)
    }

    def "Test HeartbeatControl MO creation fails when NetConf connection manager throws exception"() {
        given: "Prepare headers for event with all required attributes"
            prepareO1HandlersSuccess()
            prepareHeaders(SUPERVISION_ON)

        and: "a valid netconf manager provided"
            headers.put('netconfManager', netconfManagerOperation)

        and: "setup input event and initialise NtfSubscriptionControl"
            def inputEvent = new MediationComponentEvent(headers, null)
            heartbeatControlHandler.init(mockEventHandlerContext)

        when: "invoke onEvent with input"
            def outputEvent = heartbeatControlHandler.onEventWithResult(inputEvent)

        then: "exception is thrown"
            netconfManagerOperation.executeXAResourceOperation(_) >> { throw new RuntimeException("Error") }

        and: "handler operation is set to operation failed"
            verifyOperation(outputEvent.getHeaders(), O1NetconfOperationResult.OPERATION_FAILED, "Error")
    }

    def prepareHeaders(boolean active) {
        def map = [active: active]
        headers.put('supervisionAttributes', map)
        headers.put('heartbeatinterval', 300)
        headers.put('fdn', 'NetworkElement=TestNode,FmAlarmSupervision=1')
        headers.put('ManagedElementId', 'TestNode')
        headers.put('NtfSubscriptionControlId', 'ENMFM')
    }

    def prepareO1HandlersSuccess() {
        O1NetconfOperationsStatusHelper.putHandlerStatusInHeaders(createOperation(NtfSubscriptionControlHandler.class, OPERATION_SUCCESS, null), headers);
        O1NetconfOperationsStatusHelper.putHandlerStatusInHeaders(createOperation(NtfSubscriptionControlHandler.class, OPERATION_SUCCESS, null), headers);
    }

    def createOperation(final Class handler, final O1NetconfOperationResult result, final Exception e) {
        final O1NetconfOperationStatus operationStatus = new O1NetconfOperationStatus(handler, "NetworkElement=TestNode,FmAlarmSupervision=1")
        operationStatus.setResult(result)
        operationStatus.setException(e)
        return operationStatus
    }

    def createObjects(String nodeName) {
        RuntimeConfigurableDps configurableDps = cdiInjectorRule.getService(RuntimeConfigurableDps)
        configurableDps.withTransactionBoundaries()
        def nodeAttribute = [
                fdn              : "NetworkElement=" + nodeName,
                ossPrefix        : "test_ossPrefix",
                platformType     : "test_platform",
                neType           : "O1Node",
                ossModelIdentity : "555-777-999",
                nodeModelIdentity: "444-333-222",
                version          : "1.1.0"]

        ManagedObject meContext = configurableDps.addManagedObject()
                .withFdn("SubNetwork=TestNode,MeContext=TestNode").addAttribute("id", 1).build();

        ManagedObject networkElement = configurableDps.addManagedObject().withFdn("NetworkElement=" + nodeName).addAttributes(nodeAttribute)
                .withAssociation("nodeRootRef", meContext)
                .build()

        meContext.addAssociation('networkElementRef', networkElement);
    }

    def String getExpectedRequestDetails() {
        return "operation=CREATE, " +
                "fdn=SubNetwork=TestNode,MeContext=TestNode,ManagedElement=TestNode,NtfSubscriptionControl=ENMFM,HeartbeatControl=1, " +
                "namespace=urn:3gpp:sa5:_3gpp-common-managed-element, " +
                "name=HeartbeatControl, nodeType=O1Node"
    }

    private void verifyOperation(final Map<String, Object> headers, final O1NetconfOperationResult result, final String exceptionMessage) {
        final O1NetconfOperationsStatus operationsStatus = O1NetconfOperationsStatusHelper.getO1NetconfOperationsStatus(headers)
        final O1NetconfOperationStatus operationStatus = operationsStatus.getOperation(HeartbeatControlHandler.class)
        assert operationStatus.getResult() == result
        if (!StringUtils.isBlank(exceptionMessage)) {
            assert operationStatus.getException().getMessage() == exceptionMessage
        }
    }

    def prepareO1HandlerFailed() {
        O1NetconfOperationsStatusHelper.putHandlerStatusInHeaders(createOperation(NtfSubscriptionControlHandler.class, OPERATION_SUCCESS, null), headers);
        O1NetconfOperationsStatusHelper.putHandlerStatusInHeaders(createOperation(NtfSubscriptionControlHandler.class, OPERATION_FAILED, null), headers);
        O1NetconfOperationsStatusHelper.putHandlerStatusInHeaders(createOperation(NtfSubscriptionControlHandler.class, OPERATION_ABORTED, null), headers);
    }
}
