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

package com.ericsson.oss.mediation.fm.o1.flows;

import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ericsson.oss.mediation.adapter.netconf.jca.api.NetconfSession;
import com.ericsson.oss.mediation.adapter.netconf.jca.api.operation.NetconfOperationConnection;
import com.ericsson.oss.mediation.adapter.netconf.jca.api.operation.NetconfOperationRequest;
import com.ericsson.oss.mediation.adapter.netconf.jca.xa.yang.provider.YangNetconfOperationResult;
import com.ericsson.oss.mediation.util.netconf.api.Filter;
import com.ericsson.oss.mediation.util.netconf.api.NetconfConnectionStatus;
import com.ericsson.oss.mediation.util.netconf.api.NetconfManager;
import com.ericsson.oss.mediation.util.netconf.api.NetconfResponse;
import com.ericsson.oss.mediation.util.netconf.api.exception.NetconfManagerException;

public class NetconfManagerOperationMockHelper {

    public interface NetconfManagerOperation extends NetconfOperationConnection, NetconfManager, NetconfSession {

    }

    public static NetconfManagerOperation mockNetconfManagerOperation() {
        NetconfManagerOperation netconfManagerOperation = mock(NetconfManagerOperation.class);
        try {
            when(netconfManagerOperation.get(any(Filter.class))).thenReturn(okResponseSingleManagedElement());
        } catch (NetconfManagerException e) {
            fail(e.getMessage());
        }
        when(netconfManagerOperation.executeXAResourceOperation(any(NetconfOperationRequest.class)))
                .thenReturn(YangNetconfOperationResult.OK);
        when(netconfManagerOperation.getStatus())
                .thenReturn(NetconfConnectionStatus.NEVER_CONNECTED)
                .thenReturn(NetconfConnectionStatus.CONNECTED);
        return netconfManagerOperation;
    }

    public static NetconfResponse okResponseSingleManagedElement() {
        String testString = "<ManagedElement " +
                "xmlns=\"urn:3gpp:sa5:_3gpp-common-managed-element\">" +
                "<id>ocp83vcu03o1</id>" +
                "<ExternalDomain " +
                "xmlns=\"urn:rdns:com:ericsson:oammodel:ericsson-external-domain-cr\">" +
                "<id>1</id>" +
                "</ExternalDomain>" +
                "</ManagedElement>";
        NetconfResponse response = new NetconfResponse();
        response.setData(testString);
        return response;
    }
}
