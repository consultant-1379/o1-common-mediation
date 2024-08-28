package com.ericsson.oss.mediation.o1.yang.handlers.netconf

import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.itpf.common.config.Configuration
import com.ericsson.oss.itpf.common.event.ComponentEvent
import com.ericsson.oss.itpf.common.event.handler.EventHandlerContext
import com.ericsson.oss.itpf.sdk.core.retry.RetriableCommand
import com.ericsson.oss.itpf.sdk.core.retry.RetryManager
import com.ericsson.oss.itpf.sdk.core.retry.RetryPolicy
import com.ericsson.oss.mediation.flow.events.MediationComponentEvent
import com.ericsson.oss.mediation.o1.common.handlers.netconf.O1NetconfOperationResult
import com.ericsson.oss.mediation.o1.common.handlers.netconf.O1NetconfOperationStatus
import com.ericsson.oss.mediation.o1.common.handlers.netconf.O1NetconfOperationsStatus
import com.ericsson.oss.mediation.o1.common.handlers.netconf.O1NetconfOperationsStatusHelper
import com.ericsson.oss.mediation.util.netconf.api.NetconfManager
import com.ericsson.oss.mediation.util.netconf.api.NetconfResponse
import com.ericsson.oss.mediation.util.netconf.api.exception.NetconfManagerException
import org.apache.commons.lang3.StringUtils

import javax.inject.Inject

import static com.ericsson.oss.mediation.util.netconf.api.NetconfConnectionStatus.CONNECTED
import static com.ericsson.oss.mediation.util.netconf.api.NetconfConnectionStatus.NEVER_CONNECTED
import static com.ericsson.oss.mediation.util.netconf.api.NetconfConnectionStatus.NOT_CONNECTED

class ReadManagedElementIdHandlerSpec extends CdiSpecification {

    private final Map<String, Object> headers = new HashMap<>()

    @ObjectUnderTest
    private ReadManagedElementIdHandler readManagedElementIdHandler

    @MockedImplementation
    private EventHandlerContext mockEventHandlerContext

    @Inject
    private NetconfManager netconfManager

    @Inject
    private NetconfResponse netconfResponse

    @Inject
    private RetryManager retryManager

    def setup() {
        def mockConfiguration = mock(Configuration)
        mockEventHandlerContext.getEventHandlerConfiguration() >> mockConfiguration
        mockConfiguration.getAllProperties() >> headers
        netconfManager.getStatus() >>> [NEVER_CONNECTED, CONNECTED]
    }

    def "Test successful read of Managed element id"() {
        given: "A valid netconf manager"
            headers.put('netconfManager', netconfManager)

        and: "A valid input event"
            def inputEvent = new MediationComponentEvent(headers, null)
            readManagedElementIdHandler.init(mockEventHandlerContext)

        and: "A valid response from the node"
            netconfResponse.setData(getManagedElementResponse_ok())
            retryManager.executeCommand(_ as RetryPolicy, _ as RetriableCommand) >> netconfResponse

        when: "ReadManagedElementIdHandler is invoked"
            ComponentEvent outputEvent = readManagedElementIdHandler.onEventWithResult(inputEvent)

        then: "ManagedElementId is successfully read from the node"
            outputEvent.getHeaders().get("ManagedElementId") == "ocp83vcu03o1"

        and: "the ReadManagedElementIdHandler operation is success"
            verifyHandlerOperation(outputEvent.getHeaders(), O1NetconfOperationResult.OPERATION_SUCCESS, null)
    }

    def "Test successful read of Managed element id when id is 1"() {
        given: "A valid netconf manager"
            headers.put('netconfManager', netconfManager)

        and: "A valid input event"
            def inputEvent = new MediationComponentEvent(headers, null)
            readManagedElementIdHandler.init(mockEventHandlerContext)

        and: "A valid response from the node"
            netconfResponse.setData(getManagedElementResponse_idIsOne())
            retryManager.executeCommand(_ as RetryPolicy, _ as RetriableCommand) >> netconfResponse

        when: "ReadManagedElementIdHandler is invoked"
            ComponentEvent outputEvent = readManagedElementIdHandler.onEventWithResult(inputEvent)

        then: "ManagedElementId is successfully read from the node"
            outputEvent.getHeaders().get("ManagedElementId") == "1"

        and: "the ReadManagedElementIdHandler operation is success"
            verifyHandlerOperation(outputEvent.getHeaders(), O1NetconfOperationResult.OPERATION_SUCCESS, null)
    }

    def "Test successful read of Managed element id when already connected"() {
        given: "A valid netconf manager"
            headers.put('netconfManager', netconfManager)

        and: "A valid input event"
            def inputEvent = new MediationComponentEvent(headers, null)
            readManagedElementIdHandler.init(mockEventHandlerContext)

        and: "A valid response from the node"
            netconfResponse.setData(getManagedElementResponse_ok())
            retryManager.executeCommand(_ as RetryPolicy, _ as RetriableCommand) >> netconfResponse

        when: "ReadManagedElementIdHandler is invoked"
            ComponentEvent outputEvent = readManagedElementIdHandler.onEventWithResult(inputEvent)

        then: "ManagedElementId is successfully read from the node"
            netconfManager.getStatus() >>> [CONNECTED, CONNECTED]
            outputEvent.getHeaders().get("ManagedElementId") == "ocp83vcu03o1"

        and: "the ReadManagedElementIdHandler operation is success"
            verifyHandlerOperation(outputEvent.getHeaders(), O1NetconfOperationResult.OPERATION_SUCCESS, null)
    }

    def "Test successful read of Managed element id when disconnect fails"() {
        given: "A valid netconf manager and successful session is created"
            headers.put('netconfManager', netconfManager)

        and: "A valid input event"
            def inputEvent = new MediationComponentEvent(headers, null)
            readManagedElementIdHandler.init(mockEventHandlerContext)

        and: "A valid response from the node"
            netconfResponse.setData(getManagedElementResponse_ok())
            retryManager.executeCommand(_ as RetryPolicy, _ as RetriableCommand) >>> [netconfResponse,
                                                                                      { throw new NetconfManagerException("disconnect exception") }]
        when: "ReadManagedElementIdHandler is invoked"
            ComponentEvent outputEvent = readManagedElementIdHandler.onEventWithResult(inputEvent)

        then: "ManagedElementId is successfully read from the node"
            netconfManager.getStatus() >>> [CONNECTED, NOT_CONNECTED]
            outputEvent.getHeaders().get("ManagedElementId") == "ocp83vcu03o1"

        and: "the ReadManagedElementIdHandler operation is success"
            verifyHandlerOperation(outputEvent.getHeaders(), O1NetconfOperationResult.OPERATION_SUCCESS, null)
    }

    def "Managed element id missing in netconf response throws internal error"() {

        given: "A valid netconf manager"
            headers.put('netconfManager', netconfManager)

        and: "A valid input event"
            def inputEvent = new MediationComponentEvent(headers, null)
            readManagedElementIdHandler.init(mockEventHandlerContext)

        and: "A response with missing managed element id"
            netconfResponse.setData(getManagedElementResponse_missingId())
            retryManager.executeCommand(_ as RetryPolicy, _ as RetriableCommand) >> netconfResponse

        when: "ReadManagedElementIdHandler is invoked"
            def outputEvent = readManagedElementIdHandler.onEventWithResult(inputEvent)

        then: "the ReadManagedElementIdHandler operation is aborted"
            verifyHandlerOperation(outputEvent.getHeaders(), O1NetconfOperationResult.OPERATION_FAILED, "No managedElementId found on the node")
    }

    def "Test when invoke handler with a null event exception operation result is failed"() {
        given: "A valid netconf manager"
            headers.put('netconfManager', netconfManager)

        when: "ReadManagedElementIdHandler is invoked with a null input event"
            ComponentEvent outputEvent = readManagedElementIdHandler.onEventWithResult(null)

        then: "ReadManagedElementIdHandler operation is failed"
            verifyHandlerOperation(outputEvent.getHeaders(), O1NetconfOperationResult.OPERATION_FAILED, "Internal error - input event received is null")
    }

    def "Test when netconf manager is null in the headers then operation is failed"() {
        given: "A netconf connection is null"
            headers.put('netconfManager', null)

        and: "A valid input event"
            def inputEvent = new MediationComponentEvent(headers, null)
            readManagedElementIdHandler.init(mockEventHandlerContext)

        when: "ReadManagedElementIdHandler is invoked"
            def outputEvent = readManagedElementIdHandler.onEventWithResult(inputEvent)

        then: "ReadManagedElementIdHandler operation is failed with netconf manager not found message"
            verifyHandlerOperation(outputEvent.getHeaders(), O1NetconfOperationResult.OPERATION_FAILED, "Netconf manager not found")
    }

    def "Test when netconf manager throws exception when get request invoked the operation is failed"() {
        given: "A valid netconf manager"
            headers.put('netconfManager', netconfManager)

        and: "A valid input event"
            def inputEvent = new MediationComponentEvent(headers, null)
            readManagedElementIdHandler.init(mockEventHandlerContext)

        and: "Netconf GET call that will throw an exception"
            retryManager.executeCommand(_ as RetryPolicy, _ as RetriableCommand) >>> [netconfResponse,
                                                                                      { throw new NetconfManagerException("read exception") }]
        when: "ReadManagedElementIdHandler is invoked"
            def outputEvent = readManagedElementIdHandler.onEventWithResult(inputEvent)

        then: "ReadManagedElementIdHandler operation is failed"
            verifyHandlerOperation(outputEvent.getHeaders(), O1NetconfOperationResult.OPERATION_FAILED,
                    "Failed to read MO from node using filter XPathFilter{filter='/ManagedElement/attributes'}")
    }

    def "Test when data returned in response from node is empty then operation is failed"() {
        given: "A valid netconf manager"
            headers.put('netconfManager', netconfManager)

        and: "A valid input event"
            def inputEvent = new MediationComponentEvent(headers, null)
            readManagedElementIdHandler.init(mockEventHandlerContext)

        and: "Netconf GET response with empty data"
            netconfResponse.setData("")
            retryManager.executeCommand(_ as RetryPolicy, _ as RetriableCommand) >> netconfResponse

        when: "ReadManagedElementIdHandler is invoked"
            def outputEvent = readManagedElementIdHandler.onEventWithResult(inputEvent)

        then: "headers contains O1NetconfOperationsStatus with a single operation error"
            O1NetconfOperationsStatus operationsStatus = O1NetconfOperationsStatusHelper.getO1NetconfOperationsStatus(outputEvent.getHeaders())
            assert operationsStatus.getFailedOperation() != null
            assert operationsStatus.getAllOperations().size() == 1

        and: "ReadManagedElementIdHandler operation is failed"
            verifyHandlerOperation(outputEvent.getHeaders(), O1NetconfOperationResult.OPERATION_FAILED,
                    "Error - netconf GET response was: NetconfResponse [messageId=0, isError=false, errorMessage=null, errorCode=0, data=, errors=null]")
    }

    def "Test when netconf response from node is null"() {
        given: "A valid netconf manager"
            headers.put('netconfManager', netconfManager)

        and: "A valid input event"
            def inputEvent = new MediationComponentEvent(headers, null)
            readManagedElementIdHandler.init(mockEventHandlerContext)

        and: "Netconf GET response is null"
            retryManager.executeCommand(_ as RetryPolicy, _ as RetriableCommand) >> null

        when: "ReadManagedElementIdHandler is invoked"
            def outputEvent = readManagedElementIdHandler.onEventWithResult(inputEvent)

        then: "headers contains O1NetconfOperationsStatus with a single operation error"
            O1NetconfOperationsStatus operationsStatus = O1NetconfOperationsStatusHelper.getO1NetconfOperationsStatus(outputEvent.getHeaders())
            assert operationsStatus.getFailedOperation() != null
            assert operationsStatus.getAllOperations().size() == 1

        and: "ReadManagedElementIdHandler operation is failed"
            verifyHandlerOperation(outputEvent.getHeaders(), O1NetconfOperationResult.OPERATION_FAILED, "Netconf GET response was null")
    }

    def "Test when response from node indicates an error then operation is failed"() {
        given: "A valid netconf manager"
            headers.put('netconfManager', netconfManager)

        and: "A valid input event"
            def inputEvent = new MediationComponentEvent(headers, null)
            readManagedElementIdHandler.init(mockEventHandlerContext)

        and: "Netconf GET response that indicates an error occurred"
            netconfResponse.setError(true)
            retryManager.executeCommand(_ as RetryPolicy, _ as RetriableCommand) >> netconfResponse

        when: "ReadManagedElementIdHandler is invoked"
            def outputEvent = readManagedElementIdHandler.onEventWithResult(inputEvent)

        then: "headers contains O1NetconfOperationsStatus with a single operation error"
            O1NetconfOperationsStatus operationsStatus = O1NetconfOperationsStatusHelper.getO1NetconfOperationsStatus(outputEvent.getHeaders())
            assert operationsStatus.getFailedOperation() != null
            assert operationsStatus.getAllOperations().size() == 1

        and: "ReadManagedElementIdHandler operation is failed"
            verifyHandlerOperation(outputEvent.getHeaders(), O1NetconfOperationResult.OPERATION_FAILED, "Error - netconf GET response was: NetconfResponse [messageId=0, isError=true, errorMessage=null, errorCode=0, data=, errors=null]")
    }

    private String getManagedElementResponse_missingId() {
        return "<ManagedElement xmlns=\"urn:3gpp:sa5:_3gpp-common-managed-element\">\n" +
                "</ManagedElement>\n";
    }

    private String getManagedElementResponse_ok() {
        return " <ManagedElement xmlns=\"urn:3gpp:sa5:_3gpp-common-managed-element\">\n" +
                "        <id>ocp83vcu03o1</id>\n" +
                "        <attributes>\n" +
                "            <priorityLabel>1</priorityLabel>\n" +
                "            <vendorName>Ericsson AB</vendorName>\n" +
                "            <managedElementTypeList>GNBCUCP</managedElementTypeList>\n" +
                "            <locationName>Linkoping,SWE</locationName>\n" +
                "        </attributes>\n" +
                "    </ManagedElement>";
    }

    private String getManagedElementResponse_idIsOne() {
        return " <ManagedElement xmlns=\"urn:3gpp:sa5:_3gpp-common-managed-element\">\n" +
                "        <id>1</id>\n" +
                "        <attributes>\n" +
                "            <priorityLabel>1</priorityLabel>\n" +
                "            <vendorName>Ericsson AB</vendorName>\n" +
                "            <managedElementTypeList>GNBCUCP</managedElementTypeList>\n" +
                "            <locationName>Linkoping,SWE</locationName>\n" +
                "        </attributes>\n" +
                "    </ManagedElement>";
    }

    private void verifyHandlerOperation(final Map<String, Object> headers, final O1NetconfOperationResult result, final String exceptionMessage) {
        final O1NetconfOperationsStatus operationsStatus = O1NetconfOperationsStatusHelper.getO1NetconfOperationsStatus(headers)
        final O1NetconfOperationStatus operationStatus = operationsStatus.getOperation(ReadManagedElementIdHandler.class)
        assert operationStatus.getResult() == result
        if (!StringUtils.isBlank(exceptionMessage)) {
            assert operationStatus.getException().getMessage() == exceptionMessage
        }
    }
}