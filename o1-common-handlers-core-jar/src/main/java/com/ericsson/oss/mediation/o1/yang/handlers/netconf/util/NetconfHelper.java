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

package com.ericsson.oss.mediation.o1.yang.handlers.netconf.util;

import static com.ericsson.oss.mediation.util.netconf.api.NetconManagerConstants.NETCONF_MANAGER_ATTR;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import com.ericsson.oss.itpf.common.event.handler.exception.EventHandlerException;
import com.ericsson.oss.itpf.sdk.core.retry.RetriableCommand;
import com.ericsson.oss.itpf.sdk.core.retry.RetryManager;
import com.ericsson.oss.itpf.sdk.core.retry.RetryPolicy;
import com.ericsson.oss.mediation.util.netconf.api.Filter;
import com.ericsson.oss.mediation.util.netconf.api.NetconfConnectionStatus;
import com.ericsson.oss.mediation.util.netconf.api.NetconfManager;
import com.ericsson.oss.mediation.util.netconf.api.NetconfResponse;

import lombok.extern.slf4j.Slf4j;

/**
 * Netconf helper class for common netconf methods.
 */
@Slf4j
public class NetconfHelper {

    @Inject
    private RetryManager retryManager;

    public NetconfManager getNetconfManager(final Map<String, Object> headers) {
        NetconfManager netconfManager = (NetconfManager) headers.get(NETCONF_MANAGER_ATTR);
        if (netconfManager == null) {
            throw new EventHandlerException("Netconf manager not found");
        }
        return netconfManager;
    }

    public void connect(final NetconfManager netconfManager) {

        final NetconfConnectionStatus netconfConnectionStatus = netconfManager.getStatus();
        if (netconfConnectionStatus != NetconfConnectionStatus.CONNECTED) {
            retryManager.executeCommand(getRetryPolicy(2), (RetriableCommand<Void>) retryContext -> {
                log.info("NetconfManager trying to connect - retryCommandID: {} retryAttempt: {}", retryContext.getCommandId(),
                        retryContext.getCurrentAttempt());
                netconfManager.connect();
                return null;
            });
            if (!netconfManager.getStatus().equals(NetconfConnectionStatus.CONNECTED)) {
                throw new EventHandlerException("NetconfManager connect() failed after a number of attempts");
            }
            log.info("NetconfManager connect() success");
        } else {
            log.info("NetconfManager connect() was skipped as connection status was: {}", netconfConnectionStatus);
        }
    }

    public void disconnect(final NetconfManager netconfManager) {
        if (netconfManager != null && netconfManager.getStatus() == NetconfConnectionStatus.CONNECTED) {
            retryManager.executeCommand(getRetryPolicy(2), (RetriableCommand<Void>) retryContext -> {
                log.info("NetconfManager trying to disconnect - retryCommandID: {} retryAttempt: {}", retryContext.getCommandId(),
                        retryContext.getCurrentAttempt());
                netconfManager.disconnect();
                log.info("NetconfManager disconnect() success");
                return null;
            });
            if (netconfManager.getStatus() != NetconfConnectionStatus.NOT_CONNECTED) {
                log.warn("Failed to disconnect the netconf connection.");
            }
        } else {
            log.info("Disconnection operation skipped.");
        }
    }

    public NetconfResponse readMo(final NetconfManager netconfManager, final Filter filter) {
        NetconfResponse netconfResponse = null;
        try {
            netconfResponse = retryManager.executeCommand(getRetryPolicy(2), retryContext -> {
                log.info("NetconfManager trying get command - retryCommandID: {} retryAttempt: {}", retryContext.getCommandId(),
                        retryContext.getCurrentAttempt());
                return netconfManager.get(filter);
            });
        } catch (final Exception e) {
            throw new EventHandlerException("Failed to read MO from node using filter " + filter, e);
        }
        log.trace("NetconfResponse is [{}]", netconfResponse != null ? netconfResponse.toString() : null);
        return netconfResponse;
    }

    private RetryPolicy getRetryPolicy(final int retryAttempts) {
        return RetryPolicy.builder()
                .attempts(retryAttempts)
                .waitInterval(500, TimeUnit.MILLISECONDS)
                .exponentialBackoff(1.5)
                .retryOn(Exception.class)
                .build();
    }
}
