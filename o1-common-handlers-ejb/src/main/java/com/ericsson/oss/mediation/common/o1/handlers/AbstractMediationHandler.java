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

package com.ericsson.oss.mediation.common.o1.handlers;

import java.util.HashMap;
import java.util.Map;

import javax.jms.ObjectMessage;

import org.slf4j.Logger;

import com.ericsson.oss.itpf.common.event.handler.EventHandlerContext;
import com.ericsson.oss.itpf.common.event.handler.ResultEventInputHandler;
import com.ericsson.oss.itpf.common.event.handler.exception.EventHandlerException;
import com.ericsson.oss.mediation.flow.events.MediationComponentEvent;
import com.ericsson.oss.mediation.fm.o1.common.util.EventFormatter;
import com.ericsson.oss.mediation.o1.yang.handlers.netconf.api.CommonConstants;

/**
 * Abstract Handler which should be extended by each Mediation Handler (for handling typed events and sending events to other event handlers)
 */
public abstract class AbstractMediationHandler implements ResultEventInputHandler {

    private static final String SUPERVISION_FAILED_MSG = " Supervision Request Failed ";

    private static final String SUPERVISION_ATTRIBUTES = "supervisionAttributes";

    final Map<String, Object> supervisionHeaders = new HashMap<>();
    protected Map<String, Object> headers = new HashMap<>();
    protected Object payload = null;

    private ObjectMessage objectMessage;

    /**
     * @param ctx
     *            the parameter is passed by the mediation framework and is used to extract the information contained into the event such as FDN, and
     *            eventually others.
     */
    @Override
    public void init(final EventHandlerContext ctx) {
        headers = ctx.getEventHandlerConfiguration().getAllProperties();
    }

    @Override
    public void destroy() {}

    @Override
    public Object onEventWithResult(final Object inputEvent) {
        if (inputEvent == null) {
            throw new EventHandlerException("Internal error - input event received is null");
        }
        if (inputEvent instanceof MediationComponentEvent) {
            final MediationComponentEvent mediationComponentEvent = (MediationComponentEvent) inputEvent;

            headers.putAll(mediationComponentEvent.getHeaders());
            payload = mediationComponentEvent.getPayload();

            if (getLogger().isTraceEnabled()) {
                getLogger().trace("with headers: {}", EventFormatter.format(headers));
                getLogger().trace("with payload: {}", EventFormatter.format(payload));
            }
        }
        if (inputEvent instanceof ObjectMessage) {
            objectMessage = (ObjectMessage) inputEvent;
        }
        final Map<String, ? extends Object> supervisionParams = getHeader(SUPERVISION_ATTRIBUTES);
        if (supervisionParams != null) {
            supervisionHeaders.putAll(supervisionParams);
        }
        return new MediationComponentEvent(headers, payload);
    }

    @Override
    public void onEvent(final Object o) {
        throw new EventHandlerException("Not supported");
    }

    protected abstract Logger getLogger();

    @SuppressWarnings("unchecked")
    protected <T> T getSupervisionHeader(final String key) {
        return (T) supervisionHeaders.get(key);
    }

    protected <T> T getHeader(final String key) {
        return (T) headers.get(key);
    }

    protected String getHeaderFdn() {
        return (String) headers.get(CommonConstants.FDN);
    }

    protected boolean isSupervisionActive() {
        if (supervisionHeaders.containsKey(CommonConstants.ACTIVE)) {
            return getSupervisionHeader(CommonConstants.ACTIVE);
        } else if (headers.containsKey(CommonConstants.ACTIVE)) {
            return getHeader(CommonConstants.ACTIVE);
        }
        throw new EventHandlerException(SUPERVISION_FAILED_MSG + ": supervision status not available");
    }
}
