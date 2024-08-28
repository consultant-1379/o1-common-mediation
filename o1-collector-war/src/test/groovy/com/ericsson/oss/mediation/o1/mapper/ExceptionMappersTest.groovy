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

package com.ericsson.oss.mediation.o1.mapper

import com.ericsson.oss.mediation.o1.event.service.O1NotificationNormalizer

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue
import static org.mockito.Matchers.any
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.times
import static org.mockito.Mockito.verify
import static org.mockito.Mockito.when

import javax.servlet.http.HttpServletRequest
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.ext.ExceptionMapper

import org.jboss.resteasy.core.Dispatcher
import org.jboss.resteasy.mock.MockDispatcherFactory
import org.jboss.resteasy.mock.MockHttpRequest
import org.jboss.resteasy.mock.MockHttpResponse
import org.json.JSONException
import org.junit.Before
import org.junit.Test;
import org.junit.runner.RunWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.internal.util.reflection.Whitebox
import org.mockito.runners.MockitoJUnitRunner

import com.ericsson.oss.itpf.sdk.recording.ErrorSeverity
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder
import com.ericsson.oss.mediation.o1.event.exception.O1ValidationException
import com.ericsson.oss.mediation.o1.event.mapper.GenericExceptionMapper
import com.ericsson.oss.mediation.o1.event.mapper.JsonExceptionMapper
import com.ericsson.oss.mediation.o1.event.mapper.ValidatorExceptionMapper
import com.ericsson.oss.mediation.o1.event.model.O1Notification
import com.ericsson.oss.mediation.o1.event.resources.O1NotificationReceiver
import com.ericsson.oss.mediation.o1.event.service.O1ProcessingService

@RunWith(MockitoJUnitRunner.class)
public class ExceptionMappersTest {

    private static final String VES_ENDPOINT_WITH_IDENTIFIER = "fm/eventListener/v1";

    @InjectMocks
    private O1NotificationReceiver o1NotificationReceiver

    @Mock
    private SystemRecorder systemRecorderMock

    @Mock
    private O1ProcessingService o1ProcessingService

    @Mock
    private O1NotificationNormalizer o1NotificationNormalizer

    @Before
    public void setup() {
        System.properties.'med_service_protocol_info' = 'FM'
    }
    @Test
    public void GIVEN_runtimeException_WHEN_postHttpSent_THEN_exceptionHttpResponseCodeReturned() throws Exception {
        GenericExceptionMapper mapper = new GenericExceptionMapper()

        //GIVEN: Notification is processed
        when(o1ProcessingService.processNotification(any(O1Notification.class))).thenThrow(new RuntimeException(""));

        //WHEN: Exception is thrown due to runtime exception
        final MockHttpResponse response = executeHttp(VES_ENDPOINT_WITH_IDENTIFIER,mapper);

        //THEN: A 500 INTERNAL SERVER ERROR response is received
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());

        //AND: Error message with details is returned
        assertTrue(response.getContentAsString().contains("An error occurred"))

        //AND: Error is logged to System Recorder
        verify(systemRecorderMock, times(1)).recordError("GenericExceptionMapper", ErrorSeverity.ERROR, "", "",
                        "Internal Server Error.")
    }


    @Test
    public void GIVEN_o1Exception_WHEN_postHttpSent_THEN_validatorExceptionHttpResponseReturned() throws O1ValidationException {
        ValidatorExceptionMapper mapper = new ValidatorExceptionMapper()

        //GIVEN: Notification is processed
        when(o1NotificationNormalizer.normalize(any(Object.class))).thenThrow(new O1ValidationException(""))

        //WHEN: O1Exception is thrown due to Json validation failure
        final MockHttpResponse response = executeHttp(VES_ENDPOINT_WITH_IDENTIFIER,mapper)

        //THEN: A 400 BAD REQUEST response is returned
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus())

        //AND: Error message with details is returned
        assertTrue(response.getContentAsString().contains("JSON does not conform to schema. Notification will be discarded."))

        //AND: Error is logged to System Recorder
        verify(systemRecorderMock, times(1)).recordError("ValidatorExceptionMapper", ErrorSeverity.WARNING, "", "",
                        "Error validating O1 Notification. The notification will be discarded.")
    }

    @Test
    public void GIVEN_o1Exception_WHEN_postHttpSent_THEN_validatorExceptionHttpResponseReturnedForServiceIdentifierPath() throws O1ValidationException {
        ValidatorExceptionMapper mapper = new ValidatorExceptionMapper()

        //GIVEN: Notification is processed
        when(o1NotificationNormalizer.normalize(any(Object.class))).thenThrow(new O1ValidationException(""))

        //WHEN: O1Exception is thrown due to Json validation failure
        final MockHttpResponse response = executeHttp(VES_ENDPOINT_WITH_IDENTIFIER,mapper)

        //THEN: A 400 BAD REQUEST response is returned
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus())

        //AND: Error message with details is returned
        assertTrue(response.getContentAsString().contains("JSON does not conform to schema. Notification will be discarded."))

        //AND: Error is logged to System Recorder
        verify(systemRecorderMock, times(1)).recordError("ValidatorExceptionMapper", ErrorSeverity.WARNING, "", "",
                        "Error validating O1 Notification. The notification will be discarded.")
    }

    @Test
    public void GIVEN_jsonException_WHEN_postHttpSent_THEN_jsonExceptionHttpResponseCodeReturnedForServiceIdentifierPath() throws JSONException {
        JsonExceptionMapper mapper = new JsonExceptionMapper()

        //GIVEN: Notification is processed
        when(o1NotificationNormalizer.normalize(any(Object.class))).thenThrow(new JSONException(""))

        //WHEN: O1Exception is thrown due to Json validation failure
        final MockHttpResponse response = executeHttp(VES_ENDPOINT_WITH_IDENTIFIER,mapper)

        //THEN: A 400 BAD REQUEST response is returned
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());

        //AND: Error message with details is returned
        assertTrue(response.getContentAsString().contains("JSON does not conform to schema. Notification will be discarded."))

        //AND: Error is logged to System Recorder
        verify(systemRecorderMock, times(1)).recordError("JsonExceptionMapper", ErrorSeverity.WARNING, "", "",
                        "Error validating O1 Notification. The notification will be discarded.")
    }

    @Test
    public void GIVEN_wrongEndpoint_WHEN_postHttpSent_THEN_HttpForbiddenResponseCodeReturnedForServiceIdentifierPath() throws JSONException {
        JsonExceptionMapper mapper = new JsonExceptionMapper()

        //GIVEN: Unsupported service Identifier is received
        String WRONG_VES_ENDPOINT_WITH_IDENTIFIER="wrong/eventListener/v1";

        //WHEN: Request did not get processed
        final MockHttpResponse response = executeHttp(WRONG_VES_ENDPOINT_WITH_IDENTIFIER,mapper)

        //THEN: A 403 FORBIDDEN response is returned
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());

    }

    @Test
    public void GIVEN_jsonException_WHEN_postHttpSent_THEN_jsonExceptionHttpResponseCodeReturned() throws JSONException {
        JsonExceptionMapper mapper = new JsonExceptionMapper()

        //GIVEN: Notification is processed
        when(o1NotificationNormalizer.normalize(any(Object.class))).thenThrow(new JSONException(""))

        //WHEN: O1Exception is thrown due to Json validation failure
        final MockHttpResponse response = executeHttp(VES_ENDPOINT_WITH_IDENTIFIER,mapper)

        //THEN: A 400 BAD REQUEST response is returned
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());

        //AND: Error message with details is returned
        assertTrue(response.getContentAsString().contains("JSON does not conform to schema. Notification will be discarded."))

        //AND: Error is logged to System Recorder
        verify(systemRecorderMock, times(1)).recordError("JsonExceptionMapper", ErrorSeverity.WARNING, "", "",
                        "Error validating O1 Notification. The notification will be discarded.")
    }

    private MockHttpResponse executeHttp(String endpoint,ExceptionMapper mapper) {
        Whitebox.setInternalState(mapper, "systemRecorder", systemRecorderMock)

        final MockHttpRequest request = MockHttpRequest.post(endpoint).contentType(MediaType.APPLICATION_JSON) .content(getVesNotification().getBytes());
        final Dispatcher dispatcher = MockDispatcherFactory.createDispatcher();

        dispatcher.getRegistry().addSingletonResource(o1NotificationReceiver);
        dispatcher.getProviderFactory().registerProviderInstance(mapper);

        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        when(mockRequest.getHeader("X-Forwarded-For")).thenReturn("192.168.1.1");

        dispatcher.getProviderFactory().getContextDataMap().put(HttpServletRequest.class, mockRequest);

        final MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);

        return response;
    }


    private String getVesNotification() {
        return "{\n" +
                    "  \"event\": {\n" +
                    "    \"commonEventHeader\": {\n" +
                    "    }\n" +
                    "  }\n" +
                    "}";
    }
}
