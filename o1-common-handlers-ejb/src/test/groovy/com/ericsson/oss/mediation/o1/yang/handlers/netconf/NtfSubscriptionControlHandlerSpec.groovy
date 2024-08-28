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
package com.ericsson.oss.mediation.o1.yang.handlers.netconf

import com.ericsson.cds.cdi.support.configuration.InjectionProperties
import com.ericsson.cds.cdi.support.providers.custom.model.ModelPattern
import com.ericsson.cds.cdi.support.providers.custom.model.local.LocalModelServiceProvider
import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.itpf.common.config.Configuration
import com.ericsson.oss.itpf.common.event.ComponentEvent
import com.ericsson.oss.itpf.common.event.handler.EventHandlerContext
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject
import com.ericsson.oss.itpf.datalayer.dps.stub.RuntimeConfigurableDps
import com.ericsson.oss.mediation.adapter.netconf.jca.xa.yang.provider.YangNetconfOperationResult
import com.ericsson.oss.mediation.flow.events.MediationComponentEvent
import com.ericsson.oss.mediation.o1.common.handlers.netconf.O1NetconfOperationResult
import com.ericsson.oss.mediation.o1.common.handlers.netconf.O1NetconfOperationStatus
import com.ericsson.oss.mediation.o1.common.handlers.netconf.O1NetconfOperationsStatus
import com.ericsson.oss.mediation.o1.common.handlers.netconf.O1NetconfOperationsStatusHelper
import com.ericsson.oss.mediation.o1.yang.handlers.netconf.util.GlobalPropUtils
import com.ericsson.oss.mediation.util.netconf.api.Datastore
import com.ericsson.oss.mediation.util.netconf.api.editconfig.Operation
import org.apache.commons.lang3.StringUtils

import javax.inject.Inject

import java.lang.reflect.Field

import static com.ericsson.oss.mediation.o1.common.handlers.netconf.O1NetconfOperationResult.OPERATION_FAILED
import static com.ericsson.oss.mediation.o1.common.handlers.netconf.O1NetconfOperationResult.OPERATION_SUCCESS

class NtfSubscriptionControlHandlerSpec extends CdiSpecification {

    private static final String FM_ALARM_SUPERVISION_FDN = "NetworkElement=Node5G,FmAlarmSupervision=1"
    private static final boolean NOT_ACTIVE = false
    private static final boolean ACTIVE = true
    private static filteredModels = [
            new ModelPattern('.*', '.*', '.*', '.*')
    ]
    private static LocalModelServiceProvider localModelServiceProvider = new LocalModelServiceProvider(filteredModels)

    final Map<String, Object> headers = new HashMap<>()

    @ObjectUnderTest
    private NtfSubscriptionControlHandler ntfSubscriptionControlHandler

    @MockedImplementation
    private EventHandlerContext mockEventHandlerContext

    @Inject
    private NetconfManagerOperation mockNetconfOperationConnection;


    @Override
    def addAdditionalInjectionProperties(InjectionProperties injectionProperties) {
        super.addAdditionalInjectionProperties(injectionProperties)
        injectionProperties.addInjectionProvider(localModelServiceProvider)
    }

    def setup() {
        def mockConfiguration = mock(Configuration)
        mockEventHandlerContext.getEventHandlerConfiguration() >> mockConfiguration
        mockConfiguration.getAllProperties() >> headers
        setStaticField(GlobalPropUtils, "globalPropertiesFile", this.getClass().getClassLoader().getResource("global.properties").getPath())

        GlobalPropUtils.initGlobalProperties()
        createObjects("TestNode")
    }

    private void setStaticField(Class clazz, String fieldName, Object newValue) {
        Field field = clazz.getDeclaredField(fieldName)
        field.setAccessible(true)
        field.set(null, newValue)
    }


    def "Test when input event type is an unexpected type"() {

        given: "An input event of type String"
            Object unExpectedInputType = "Unexpected";

        when: "invoke onEvent with input"
            ComponentEvent outputEvent = ntfSubscriptionControlHandler.onEventWithResult(unExpectedInputType)

        then: "operation is failed"
            verifyOperation(outputEvent.getHeaders(), O1NetconfOperationResult.OPERATION_FAILED,
                    "Internal error - input event received was an unexpected type [Unexpected]")
    }

    def "Test successful creation of the NtfSubscriptionControl MO when supervision is enabled"() {
        given: "Prepare headers for event with all required attributes"
            prepareHeaders(ACTIVE)
            prepareO1Headers()

        and: "provide a valid netconf manager"
            headers.put('netconfManager', mockNetconfOperationConnection)

        and: "setup input event and initialise NtfSubscriptionControl"
            ntfSubscriptionControlHandler.init(mockEventHandlerContext)

        when: "invoke onEvent with input"
            def inputEvent = new MediationComponentEvent(headers, null)
            ComponentEvent outputEvent = ntfSubscriptionControlHandler.onEventWithResult(inputEvent)

        then: "Underlying netconf library was called with the expected request details"
            1 * mockNetconfOperationConnection.executeXAResourceOperation({
                String operationString = it.toString()
                operationString.contains(getExpectedRequestDetails(Operation.CREATE)) &&
                        operationString.contains("Attribute name=notificationRecipientAddress Attribute value=http://1.2.3.4:8099/fm/eventListener/v1") &&
                        operationString.contains("Attribute name=notificationTypes Attribute value=notifyChangedAlarm") &&
                        operationString.contains("Attribute name=notificationTypes Attribute value=notifyNewAlarm") &&
                        operationString.contains("Attribute name=notificationTypes Attribute value=notifyChangedAlarmGeneral") &&
                        operationString.contains("Attribute name=notificationTypes Attribute value=notifyClearedAlarm") &&
                        operationString.contains("Attribute name=notificationTypes Attribute value=notifyAlarmListRebuilt") &&
                        !operationString.contains("Attribute name=scope Attribute value=BASE_ALL")
            }) >> YangNetconfOperationResult.OK

        and: "operation is success"
            verifyOperation(outputEvent.getHeaders(), O1NetconfOperationResult.OPERATION_SUCCESS, null)
    }

    def "Test NtfSubscriptionControl MO creating fails when no notification type is set in the headers"() {
        given: "Prepare headers for event with all required attributes"
            prepareHeaders(ACTIVE)
            prepareO1Headers()

        and: "there are no notification types"
            headers.remove("NtfSubscriptionControlO1MessageEventPath")

        and: "initialise NtfSubscriptionControl"
            ntfSubscriptionControlHandler.init(mockEventHandlerContext)

        when: "invoke onEvent with input"
            def inputEvent = new MediationComponentEvent(headers, null)
            ComponentEvent outputEvent = ntfSubscriptionControlHandler.onEventWithResult(inputEvent)

        then: "Exception is thrown"
            noExceptionThrown()

        and: "operation fails"
            verifyOperation(outputEvent.getHeaders(), O1NetconfOperationResult.OPERATION_FAILED, "Cannot determine 'NTF_SUBSCRIPTION_CONTROL_O1_MESSAGE_EVENT_PATH' header has not been sent" )
    }


    def "Test successful deletion of the NtfSubscriptionControl MO when supervision is disabled"() {
        given: "Prepare headers for event with all required attributes"
            prepareHeaders(NOT_ACTIVE)
            prepareO1Headers()

        and: "provide a valid netconf manager"
            headers.put('netconfManager', mockNetconfOperationConnection)

        and: "setup input event and initialise NtfSubscriptionControl"
            ntfSubscriptionControlHandler.init(mockEventHandlerContext)

        when: "invoke onEvent with input"
            def inputEvent = new MediationComponentEvent(headers, null)
            ComponentEvent outputEvent = ntfSubscriptionControlHandler.onEventWithResult(inputEvent)

        then: "Underlying netconf library was called with the expected request details"
            1 * mockNetconfOperationConnection.executeXAResourceOperation({ it.toString().contains(getExpectedRequestDetails(Operation.DELETE)) }) >> YangNetconfOperationResult.OK

        and: "operation is success"
            verifyOperation(outputEvent.getHeaders(), O1NetconfOperationResult.OPERATION_SUCCESS, null)
    }

    def "Test NtfSubscriptionControl MO creation fails when VIP property not found"() {
        given: "Prepare headers for event with all required attributes"
            prepareHeaders(ACTIVE)
            headers.remove("VIP")
            prepareO1Headers()

        and: "provide a valid netconf manager"
            headers.put('netconfManager', mockNetconfOperationConnection)
            mockNetconfOperationConnection.executeXAResourceOperation(_) >> YangNetconfOperationResult.OK

        and: "setup Global Properties for testing"
            setStaticField(GlobalPropUtils, "globalPropertiesFile", this.getClass().getClassLoader().getResource("noVIP_global.properties").getPath())
            GlobalPropUtils.initGlobalProperties()
            GlobalPropUtils.globalPropertiesFile = "src/test/resources/noVIP_global.properties"

        and: "handler is initialized"
            ntfSubscriptionControlHandler.init(mockEventHandlerContext)

        when: "invoke onEvent with input"
            def inputEvent = new MediationComponentEvent(headers, null)
            def outputEvent = ntfSubscriptionControlHandler.onEventWithResult(inputEvent)

        then: "Exception is not thrown"
            noExceptionThrown()

        and: "operation is failed"
            verifyOperation(outputEvent.getHeaders(), O1NetconfOperationResult.OPERATION_FAILED, "Cannot determine VES collector ipaddress as \'VIP\' header has not been set")
    }

    def "Test NtfSubscriptionControl MO creation fails when no notification types set in headers"() {
        given: "Prepare headers for event with all required attributes"
            prepareHeaders(ACTIVE)
            prepareO1Headers()

        and: "but actually, there are no notification types"
            headers.remove('NtfSubscriptionControlNotificationTypes')

        and: "NtfSubscriptionControl is initialized"
            ntfSubscriptionControlHandler.init(mockEventHandlerContext)

        when: "handler is invoked"
            def inputEvent = new MediationComponentEvent(headers, null)
            def outputEvent = ntfSubscriptionControlHandler.onEventWithResult(inputEvent)

        then: "Exception is not thrown"
            noExceptionThrown()

        and: "operation is failed"
            verifyOperation(outputEvent.getHeaders(), O1NetconfOperationResult.OPERATION_FAILED,
                    "Cannot determine NtfSubscriptionControl.notificationTypes as 'NtfSubscriptionControlNotificationTypes' header has not been set")
    }

    def "Test NtfSubscriptionControl MO creation fails when no NtfSubscriptionControlId set in headers"() {
        given: "Prepare headers for event with all required attributes"
            prepareHeaders(ACTIVE)
            prepareO1Headers()

        and: "but actually, there is no NtfSubscriptionControlId"
            headers.remove('NtfSubscriptionControlId')

        and: "NtfSubscriptionControl is initialized"
            ntfSubscriptionControlHandler.init(mockEventHandlerContext)

        when: "handler is invoked"
            def inputEvent = new MediationComponentEvent(headers, null)
            def outputEvent = ntfSubscriptionControlHandler.onEventWithResult(inputEvent)

        then: "Exception is not thrown"
            noExceptionThrown()

        and: "operation is failed"
            verifyOperation(outputEvent.getHeaders(), O1NetconfOperationResult.OPERATION_FAILED,
                    "Cannot determine NtfSubscriptionControl.id as 'NtfSubscriptionControlId' header has not been set")
    }

    def "Test NtfSubscriptionControl MO creation fails when NetConf connection manager throws exception"() {
        given: "Prepare headers for event with all required attributes"
            prepareHeaders(ACTIVE)
            prepareO1Headers()

        and: "the netconf manager provided"
            headers.put('netconfManager', mockNetconfOperationConnection)
            mockNetconfOperationConnection.executeXAResourceOperation(_) >> { throw new IllegalArgumentException("Test Exception") }

        and: "setup Global Properties for testing"
            GlobalPropUtils.globalPropertiesFile = "src/test/resources/global.properties"

        and: "setup input event and initialise NtfSubscriptionControl"
            ntfSubscriptionControlHandler.init(mockEventHandlerContext)

        when: "invoke onEvent with input"
            def inputEvent = new MediationComponentEvent(headers, null)
            def outputEvent = ntfSubscriptionControlHandler.onEventWithResult(inputEvent)

        then: "The call to netconf manager with valid request throws an exception"
            mockNetconfOperationConnection.executeXAResourceOperation(
                    { it.toString().contains(getExpectedRequestDetails(Operation.CREATE)) }) >> { throw new IllegalArgumentException("NetconfManager problem") }

        and: "the exception is caught by the handler"
            notThrown(Exception)

        and: "operation is failed"
            verifyOperation(outputEvent.getHeaders(), O1NetconfOperationResult.OPERATION_FAILED, "NetconfManager problem")
    }

    def "Test NtfSubscriptionControl MO creation fails when VIP header not set"() {
        given: "Prepare headers for event with all required attributes except 'VIP'"
            prepareHeaders(ACTIVE)
            headers.remove("VIP")
            prepareO1Headers()

        and: "handler is initialized"
            ntfSubscriptionControlHandler.init(mockEventHandlerContext)

        when: "invoke onEvent with input"
            def inputEvent = new MediationComponentEvent(headers, null)
            def outputEvent = ntfSubscriptionControlHandler.onEventWithResult(inputEvent)

        then: "Exception was thrown"
            noExceptionThrown()

        and: "operation is failed"
            verifyOperation(outputEvent.getHeaders(), O1NetconfOperationResult.OPERATION_FAILED,
                    "Cannot determine VES collector ipaddress as 'VIP' header has not been set")
    }

    def "Test NtfSubscriptionControl creation is skipped when previous handler has failed"() {
        given: "Prepare headers for event with all required attributes except 'VIP'"
            prepareHeaders(ACTIVE)
            addO1OperationInHeader("UnlockAlarmListHandler", OPERATION_FAILED, new Exception("Unlock failed!"))

        and: "handler is initialized"
            def inputEvent = new MediationComponentEvent(headers, null)
            ntfSubscriptionControlHandler.init(mockEventHandlerContext)

        when: "invoke onEvent with input"
            def outputEvent = ntfSubscriptionControlHandler.onEventWithResult(inputEvent)

        then: "header is updated to indicate that the subscription operation was aborted"
            O1NetconfOperationsStatus operationsStatus = O1NetconfOperationsStatusHelper.getO1NetconfOperationsStatus(outputEvent.getHeaders())
            assert operationsStatus.getAllOperations().size() == 2
            assert operationsStatus.getFailedOperation() != null

        and: "operation is aborted"
            verifyOperation(outputEvent.getHeaders(), O1NetconfOperationResult.OPERATION_ABORTED, null)
    }

    def createObjects(String nodeName) {
        RuntimeConfigurableDps configurableDps = cdiInjectorRule.getService(RuntimeConfigurableDps)
        configurableDps.withTransactionBoundaries()
        def nodeAttribute = [
                fdn         : "NetworkElement=" + nodeName,
                platformType: "O1",
                neType      : "O1Node",
                version     : "1.1.0"]

        ManagedObject meContext = configurableDps.addManagedObject()
                .withFdn("SubNetwork=TestNode,MeContext=TestNode").addAttribute("id", 1).build();

        ManagedObject networkElement = configurableDps.addManagedObject().withFdn("NetworkElement=" + nodeName).addAttributes(nodeAttribute)
                .withAssociation("nodeRootRef", meContext)
                .build()

        meContext.addAssociation('networkElementRef', networkElement);
    }

    def prepareHeaders(boolean active) {
        def map = [active: active]
        headers.put('supervisionAttributes', map)
        headers.put('fdn', 'NetworkElement=TestNode,FmAlarmSupervision=1')
        headers.put('VIP', 'svc_FM_VIP_ipaddress')
        headers.put('ManagedElementId', 'TestNode')
        headers.put('NtfSubscriptionControlId', 'ENMFM')
        headers.put('NtfSubscriptionControlNotificationTypes', ['notifyChangedAlarm', 'notifyNewAlarm',
                                                                'notifyChangedAlarmGeneral', 'notifyClearedAlarm', 'notifyAlarmListRebuilt'])
        headers.put('NtfSubscriptionControlO1MessageEventPath', 'fm')

    }

    def prepareO1Headers() {
        O1NetconfOperationsStatusHelper.putHandlerStatusInHeaders(createOperation("ReadManagedElementIdHandler", OPERATION_SUCCESS, null), headers);
        O1NetconfOperationsStatusHelper.putHandlerStatusInHeaders(createOperation("SetDnPrefixHandler", OPERATION_SUCCESS, null), headers);
        O1NetconfOperationsStatusHelper.putHandlerStatusInHeaders(createOperation("UnlockAlarmListHandler", OPERATION_SUCCESS, null), headers);
        O1NetconfOperationsStatusHelper.putHandlerStatusInHeaders(createOperation("CheckSupervisionMosHandler", OPERATION_SUCCESS, null), headers);
    }

    def addO1OperationInHeader(final String handler, final O1NetconfOperationResult result, final Exception e) {
        O1NetconfOperationsStatusHelper.putHandlerStatusInHeaders(createOperation(handler, result, e), headers);
    }

    private O1NetconfOperationStatus createOperation(final String handler, final O1NetconfOperationResult result, final Exception e) {
        final O1NetconfOperationStatus operationStatus = new O1NetconfOperationStatus(handler, FM_ALARM_SUPERVISION_FDN)
        operationStatus.setResult(result)
        operationStatus.setException(e)
        return operationStatus
    }

    String getExpectedRequestDetails(final Operation operation) {
        return "operation=" + operation + ", " +
                "fdn=SubNetwork=TestNode,MeContext=TestNode,ManagedElement=TestNode,NtfSubscriptionControl=ENMFM, " +
                "namespace=urn:3gpp:sa5:_3gpp-common-managed-element, " +
                "name=NtfSubscriptionControl, nodeType=O1Node"
    }

    private void verifyOperation(final Map<String, Object> headers, final O1NetconfOperationResult result, final String exceptionMessage) {
        final O1NetconfOperationsStatus operationsStatus = O1NetconfOperationsStatusHelper.getO1NetconfOperationsStatus(headers)
        final O1NetconfOperationStatus operationStatus = operationsStatus.getOperation(NtfSubscriptionControlHandler.class)
        assert operationStatus.getResult() == result
        if (!StringUtils.isBlank(exceptionMessage)) {
            assert operationStatus.getException().getMessage() == exceptionMessage
        }
    }
}
