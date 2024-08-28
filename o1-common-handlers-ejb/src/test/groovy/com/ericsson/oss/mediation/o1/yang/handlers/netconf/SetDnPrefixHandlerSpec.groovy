package com.ericsson.oss.mediation.o1.yang.handlers.netconf

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
import com.ericsson.oss.mediation.o1.common.handlers.netconf.O1AbstractHandler
import com.ericsson.oss.mediation.o1.common.handlers.netconf.O1AbstractYangHandler
import com.ericsson.oss.mediation.o1.common.handlers.netconf.O1NetconfOperationResult
import com.ericsson.oss.mediation.o1.common.handlers.netconf.O1NetconfOperationStatus
import com.ericsson.oss.mediation.o1.common.handlers.netconf.O1NetconfOperationsStatus
import com.ericsson.oss.mediation.o1.common.handlers.netconf.O1NetconfOperationsStatusHelper
import com.ericsson.oss.mediation.util.netconf.api.Datastore
import com.ericsson.oss.mediation.util.netconf.api.editconfig.Operation
import org.apache.commons.lang3.StringUtils
import spock.lang.Unroll

import javax.jms.ObjectMessage

import java.lang.reflect.Field

import static com.ericsson.oss.mediation.o1.common.handlers.netconf.O1NetconfOperationResult.OPERATION_SUCCESS

class SetDnPrefixHandlerSpec extends CdiSpecification {

    final Map<String, Object> headers = new HashMap<>()

    private static filteredModels = [
            new ModelPattern('.*', '.*', '.*', '.*')
    ]

    private static LocalModelServiceProvider localModelServiceProvider = new LocalModelServiceProvider(filteredModels)

    @ObjectUnderTest
    private SetDnPrefixHandler setDnPrefixHandler

    @MockedImplementation
    private EventHandlerContext mockEventHandlerContext

    @MockedImplementation
    private NetconfManagerOperation netconfManagerOperation

    @MockedImplementation
    private ObjectMessage mockObjectMessage

    @Override
    def addAdditionalInjectionProperties(InjectionProperties injectionProperties) {
        super.addAdditionalInjectionProperties(injectionProperties)
        injectionProperties.addInjectionProvider(localModelServiceProvider)
    }

    def setup() {
        def mockConfiguration = mock(Configuration)
        mockEventHandlerContext.getEventHandlerConfiguration() >> mockConfiguration
        mockConfiguration.getAllProperties() >> headers
        createO1NodeInDps()
    }

    @Unroll
    def "Test successful update of ManagedElement MO when supervision is enabled"() {
        given: "Prepare headers for event with all required attributes"
            prepareO1Headers()
            prepareHeaders(active, hasSupervisionAttributes)

        and: "provide a valid netconf manager"
            headers.put('netconfManager', netconfManagerOperation)

        and: "setup input event and initialise SetDnPrefixHandler"
            setDnPrefixHandler.init(mockEventHandlerContext)

        when: "invoke onEvent with input"
            def inputEvent = new MediationComponentEvent(headers, null)
            def outputEvent = setDnPrefixHandler.onEventWithResult(inputEvent)

        then: "Underlying netconf library was called with the expected request details"
            1 * netconfManagerOperation.executeXAResourceOperation({
                String operationString = it.toString()
                operationString.contains(getExpectedRequestDetails(Operation.MERGE)) &&
                        operationString.contains("Attribute name=dnPrefix Attribute value=SubNetwork=TestNode,MeContext=TestNode")
            }) >> YangNetconfOperationResult.OK

        and: "operation is success"
            verifyHandlerOperation(outputEvent.getHeaders(), O1NetconfOperationResult.OPERATION_SUCCESS, null)

        where:
            active | hasSupervisionAttributes
            true   | true
            true   | false
    }

    @Unroll
    def "Test no update of ManagedElement MO when supervision is disabled"() {
        given: "Prepare headers for event with all required attributes"
            prepareO1Headers()
            prepareHeaders(active, hasSupervisionAttributes)

        and: "provide a valid netconf manager"
            headers.put('netconfManager', netconfManagerOperation)

        and: "setup input event and initialise SetDnPrefixHandler"
            setDnPrefixHandler.init(mockEventHandlerContext)

        when: "invoke onEvent with input"
            def inputEvent = new MediationComponentEvent(headers, null)
            def outputEvent = setDnPrefixHandler.onEventWithResult(inputEvent)

        then: "Underlying netconf library was not called"
            0 * netconfManagerOperation.executeXAResourceOperation(_)

        and: "operation is success"
            verifyHandlerOperation(outputEvent.getHeaders(), O1NetconfOperationResult.OPERATION_ABORTED, null)

        where:
            active | hasSupervisionAttributes
            false  | true
            false  | false
    }

    def "Test when yang error result"() {
        given: "Prepare headers for event with all required attributes"
            prepareO1Headers()
            prepareHeaders(true, true)

        and: "provide a valid netconf manager"
            headers.put('netconfManager', netconfManagerOperation)

        and: "setup input event and initialise SetDnPrefixHandler"
            setDnPrefixHandler.init(mockEventHandlerContext)

        when: "invoke onEvent with input"
            def inputEvent = new MediationComponentEvent(headers, null)
            def outputEvent = setDnPrefixHandler.onEventWithResult(inputEvent)

        then: "Underlying netconf library was called that returns error response"
            1 * netconfManagerOperation.executeXAResourceOperation({
                String operationString = it.toString()
                operationString.contains(getExpectedRequestDetails(Operation.MERGE)) &&
                        operationString.contains("Attribute name=dnPrefix Attribute value=SubNetwork=TestNode,MeContext=TestNode")
            }) >> YangNetconfOperationResult.ERROR

        and: "operation is success"
            verifyHandlerOperation(outputEvent.getHeaders(), O1NetconfOperationResult.OPERATION_FAILED, null)
    }

    def "When destroy is called then field data is nullified"() {
        given: "Prepare headers for event with all required attributes"
            prepareO1Headers()

        and: "handler is initialised and onEventWithResult is called"
            setDnPrefixHandler.init(mockEventHandlerContext)

        and: "invoke onEventWithResult with input and headers"
            def inputEvent = new MediationComponentEvent(headers, mockObjectMessage)
            setDnPrefixHandler.onEventWithResult(inputEvent)

        when: "destroy is called"
            setDnPrefixHandler.destroy()

        then: "variables are nullified"
            assertFieldIsNull(setDnPrefixHandler, O1AbstractHandler.class, "headers")
            assertFieldIsNull(setDnPrefixHandler, O1AbstractHandler.class, "payload")
            assertFieldIsNull(setDnPrefixHandler, O1AbstractHandler.class, "inputMediationComponentEvent")
            assertFieldIsNull(setDnPrefixHandler, O1AbstractHandler.class, "objectMessage")
            assertFieldIsNull(setDnPrefixHandler, O1AbstractYangHandler.class, "moData")
            assertFieldIsNull(setDnPrefixHandler, O1AbstractYangHandler.class, "dpsRead")
    }

    private void assertFieldIsNull(Object object, Class clazz, String fieldName) {
        Field field = clazz.getDeclaredField(fieldName)
        field.setAccessible(true)
        assert field.get(object) == null
    }

    def createO1NodeInDps() {
        RuntimeConfigurableDps configurableDps = cdiInjectorRule.getService(RuntimeConfigurableDps)
        configurableDps.withTransactionBoundaries()
        def nodeAttribute = [
                fdn   : "NetworkElement=TestNode",
                neType: "O1Node"]

        ManagedObject meContext = configurableDps.addManagedObject()
                .withFdn("SubNetwork=TestNode,MeContext=TestNode").build();

        ManagedObject networkElement = configurableDps.addManagedObject()
                .withFdn("NetworkElement=TestNode")
                .addAttributes(nodeAttribute)
                .withAssociation("nodeRootRef", meContext)
                .build()

        meContext.addAssociation('networkElementRef', networkElement);
    }

    def prepareHeaders(boolean active, boolean hasSupervisionAttributes) {
        def map = [active: active]
        if (hasSupervisionAttributes) {
            headers.put('supervisionAttributes', map)
        }
        headers.put('active', active)
        headers.put('fdn', 'NetworkElement=TestNode,FmAlarmSupervision=1')
        headers.put('ManagedElementId', 'TestNode')
    }

    String getExpectedRequestDetails(Operation operation) {
        return "operation=" + operation.name() + ", " +
                "fdn=SubNetwork=TestNode,MeContext=TestNode,ManagedElement=TestNode, " +
                "namespace=urn:3gpp:sa5:_3gpp-common-managed-element, " +
                "name=ManagedElement, nodeType=O1Node"
    }

    private void verifyHandlerOperation(final Map<String, Object> headers, final O1NetconfOperationResult result, final String exceptionMessage) {
        final O1NetconfOperationsStatus operationsStatus = O1NetconfOperationsStatusHelper.getO1NetconfOperationsStatus(headers)
        final O1NetconfOperationStatus operationStatus = operationsStatus.getOperation(SetDnPrefixHandler.class)
        assert operationStatus.getResult() == result
        if (!StringUtils.isBlank(exceptionMessage)) {
            assert operationStatus.getException().getMessage() == exceptionMessage
        }
    }

    def prepareO1Headers() {
        O1NetconfOperationsStatusHelper.putHandlerStatusInHeaders(createOperation(ReadManagedElementIdHandler.class, OPERATION_SUCCESS, null), headers);
    }

    private O1NetconfOperationStatus createOperation(final Class handler, final O1NetconfOperationResult result, final Exception e) {
        final O1NetconfOperationStatus operationStatus = new O1NetconfOperationStatus(handler, "SubNetwork=TestNode,MeContext=TestNode,ManagedElement=TestNode")
        operationStatus.setResult(result)
        operationStatus.setException(e)
        return operationStatus
    }
}