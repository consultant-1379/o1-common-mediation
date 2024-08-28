package com.ericsson.oss.mediation.o1.yang.handlers.netconf.util
import static com.ericsson.oss.mediation.o1.common.handlers.netconf.O1NetconfOperationResult.OPERATION_SUCCESS

import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.itpf.common.config.Configuration
import com.ericsson.oss.itpf.common.event.handler.EventHandlerContext
import com.ericsson.oss.mediation.o1.common.handlers.netconf.O1NetconfOperationResult
import com.ericsson.oss.mediation.o1.common.handlers.netconf.O1NetconfOperationStatus
import com.ericsson.oss.mediation.o1.common.handlers.netconf.O1NetconfOperationsStatus
import com.ericsson.oss.mediation.o1.common.handlers.netconf.O1NetconfOperationsStatusHelper

class O1NetconfOperationStatusHelperSpec extends CdiSpecification {

    @MockedImplementation
    private EventHandlerContext mockEventHandlerContext

    final Map<String, Object> headers = new HashMap<>()


    def setup() {
        def mockConfiguration = mock(Configuration)
        mockEventHandlerContext.getEventHandlerConfiguration() >> mockConfiguration
        mockConfiguration.getAllProperties() >> headers
    }

    def "Test set suppress terminate flow"(){
        prepareHeaders(true)
        prepareO1Headers();

        given:
            O1NetconfOperationsStatus operationStatus = headers.get("O1NetconfOperationsStatus")
            assert  operationStatus.suppressTerminateFlow == false

        when : "suppressTerminateFlow is modified"
            O1NetconfOperationsStatusHelper.setSuppressTerminateFlow(true, headers)

        then :
            O1NetconfOperationsStatus updatedStatus = headers.get("O1NetconfOperationsStatus")
            assert  updatedStatus.suppressTerminateFlow == true
    }


    def prepareHeaders(boolean active) {
        def map = [active: active]
        headers.put('supervisionAttributes', map)
    }

    def prepareO1Headers() {
        O1NetconfOperationsStatusHelper.putHandlerStatusInHeaders(createOperation("CheckSupervisionMosHandler", OPERATION_SUCCESS, null), headers);
    }

    private O1NetconfOperationStatus createOperation(final String handler, final O1NetconfOperationResult result, final Exception e) {
        final O1NetconfOperationStatus operationStatus = new O1NetconfOperationStatus(handler, "NetworkElement=Node5G,FmAlarmSupervision=1")
        operationStatus.setResult(result)
        operationStatus.setException(e)
        return operationStatus
    }
}