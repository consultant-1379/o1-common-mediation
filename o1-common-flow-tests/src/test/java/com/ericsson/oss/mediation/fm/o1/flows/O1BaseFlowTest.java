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

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static com.ericsson.oss.mediation.fm.o1.flows.NetconfManagerOperationMockHelper.mockNetconfManagerOperation;
import static com.ericsson.oss.mediation.o1.heartbeat.service.O1HeartbeatAgentImpl.HEARTBEAT_CACHE_NAME;
import static com.ericsson.oss.mediation.o1.heartbeat.service.O1HeartbeatAgentImpl.REGISTRATION_CACHE_NAME;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.exceptions.base.MockitoException;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

import com.ericsson.cds.cdi.support.providers.custom.model.ModelPattern;
import com.ericsson.cds.cdi.support.providers.stubs.InMemoryCache;
import com.ericsson.cds.cdi.support.rule.ImplementationClasses;
import com.ericsson.cds.cdi.support.rule.ImplementationInstance;
import com.ericsson.cds.cdi.support.rule.MockedImplementation;
import com.ericsson.oss.itpf.common.event.ComponentEvent;
import com.ericsson.oss.itpf.datalayer.dps.stub.RuntimeConfigurableDps;
import com.ericsson.oss.itpf.sdk.cache.classic.CacheProviderBean;
import com.ericsson.oss.itpf.sdk.core.retry.classic.RetryManagerNonCDIImpl;
import com.ericsson.oss.mediation.adapter.netconf.jca.api.operation.NetconfOperationRequest;
import com.ericsson.oss.mediation.handlers.TlsConfigurationListener;
import com.ericsson.oss.mediation.netconf.handlers.NetconfSessionBuilderHandler;
import com.ericsson.oss.mediation.netconf.session.api.operation.NetconfSessionOperationsStatus;
import com.ericsson.oss.mediation.o1.heartbeat.service.O1HeartbeatAgent;
import com.ericsson.oss.mediation.o1.heartbeat.service.O1HeartbeatAgentImpl;
import com.ericsson.oss.mediation.o1.yang.handlers.netconf.util.GlobalPropUtils;
import com.ericsson.oss.mediation.test.BaseFlowTest;
import com.ericsson.oss.mediation.util.netconf.api.exception.NetconfManagerException;

/**
 * This is the base test class for O1 mediation flows. It implements flow unit tests as described at:
 * https://confluence-oss.seli.wh.rnd.internal.ericsson.com/pages/viewpage.action?spaceKey=EPT&title=Unit+Testing+Mediation+Flows
 * This abstract base class should be used to keep verification methods common to multiple tests.
 */
public abstract class O1BaseFlowTest extends BaseFlowTest {

    public static final boolean ACTIVE_TRUE = true;
    public static final boolean ACTIVE_FALSE = false;
    // Prevents strictness errors from JUnit. These occur when unnecessary mocks are created
    // by the Mediation Flow Test Framework
    @Rule
    public MockitoRule mockito = MockitoJUnit.rule().strictness(Strictness.LENIENT);

    @MockedImplementation
    TlsConfigurationListener tlsConfigurationListener;

    RuntimeConfigurableDps runtimeConfigurableDps = getCdiInjectorRule().getService(RuntimeConfigurableDps.class);

    DpsNodeCreator dpsNodeCreator;

    @Inject
    O1HeartbeatAgent o1HeartbeatAgent;

    @MockedImplementation
    CacheProviderBean cacheProviderBean;

    protected NetconfManagerOperationMockHelper.NetconfManagerOperation netconfManagerOperation = mockNetconfManagerOperation();

    @Captor
    ArgumentCaptor<NetconfOperationRequest> netconfOperationRequestCaptor;

    @ImplementationInstance
    NetconfSessionBuilderHandler netconfSessionBuilderHandler = new NetconfSessionBuilderHandler() {
        @Override
        public ComponentEvent onEvent(ComponentEvent inputEvent) {
            NetconfSessionOperationsStatus operationsStatus = new NetconfSessionOperationsStatus();
            inputEvent.getHeaders().put("netconfManager", netconfManagerOperation);
            inputEvent.getHeaders().put("transportManager", netconfManagerOperation);
            inputEvent.getHeaders().put("netconfSessionOperationsStatus", operationsStatus);
            return inputEvent;
        }
    };

    @ImplementationClasses
    protected final Class<?>[] definedImplementations = {
        RetryManagerNonCDIImpl.class, O1HeartbeatAgentImpl.class
    };

    @Before
    public void setupHeartbeatCacheMocks() {
        when(cacheProviderBean.createOrGetModeledCache(eq(HEARTBEAT_CACHE_NAME))).thenReturn(new InMemoryCache(HEARTBEAT_CACHE_NAME));
        when(cacheProviderBean.createOrGetModeledCache(eq(REGISTRATION_CACHE_NAME))).thenReturn(new InMemoryCache(REGISTRATION_CACHE_NAME));
        ((O1HeartbeatAgentImpl) o1HeartbeatAgent).initializeCacheName();
    }

    // Load non-flow models
    @Override
    protected Collection<ModelPattern> getAdditionalModelPatterns() {
        final Collection<ModelPattern> modelPatterns = new ArrayList<>();
        modelPatterns.addAll(super.getAdditionalModelPatterns());
        modelPatterns.add(new ModelPattern(".*", "NODE", "O1Node", ".*"));
        modelPatterns.add(new ModelPattern(".*", "urn%3a3gpp%3asa5%3a_3gpp-common-managed-element", ".*", ".*"));
        modelPatterns.add(new ModelPattern(".*", "OSS_TOP", ".*", ".*"));
        return modelPatterns;
    }

    {
        this.dpsNodeCreator = new DpsNodeCreator(runtimeConfigurableDps);
    }

    @BeforeClass
    public static void setVesCollectorIp() {
        try {
            final MockedStatic<GlobalPropUtils> mockedGlobalPropUtil = Mockito.mockStatic(GlobalPropUtils.class);
            mockedGlobalPropUtil.when(() -> GlobalPropUtils.getGlobalValue("ves_collector_ip", String.class)).thenReturn("1.2.3.4");
        } catch (MockitoException e) {
            // Ignore already mocked
        }
    }

    void assertYangOperations(SimpleNetconfOperation... expectedOperations) throws NetconfManagerException {
        verify(netconfManagerOperation, times(expectedOperations.length)).executeXAResourceOperation(netconfOperationRequestCaptor.capture());
        List<SimpleNetconfOperation> actualOperations = netconfOperationRequestCaptor.getAllValues().stream()
                .map(r -> SimpleNetconfOperation.createFrom(r))
                .collect(Collectors.toList());
        assertEquals(Arrays.asList(expectedOperations), actualOperations);
        verify(netconfManagerOperation, times(1)).closeSessionHandle();
    }
}
