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

package com.ericsson.oss.mediation.fm.o1.common.util;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility class for formatting event headers and payloads.
 */
@Slf4j
@UtilityClass
public class EventFormatter {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .excludeFieldsWithoutExposeAnnotation()
            .create();

    private static final List<String> FILTER = Arrays.asList(
            "tlsClientKey",
            "tlsClientCertificate",
            "tlsCertificateToTrustServerCA",
            "supportedTLSCipherSuites");

    private static final String NEW_LINE = System.getProperty("line.separator");
    private static final String EMPTY = "{}";

    /**
     * Formats the headers into a readable JSON string with the keys defined by
     * removing the keys defined in {@link #FILTER} removed.
     */
    public static String format(Map<String, Object> headers) {
        return headers == null || headers.isEmpty()
                ? EMPTY
                : NEW_LINE + GSON.toJson(reduce(headers));
    }

    /**
     * Formats the payload into a readable JSON string.
     */
    public static String format(Object payload) {
        return payload == null ? EMPTY : NEW_LINE + GSON.toJson(payload);
    }

    /**
     * Reduces headers by removing keys defined in {@link #FILTER}.
     */
    private static Map<String, Object> reduce(Map<String, Object> headers) {
        try {
            JsonNode node = new JsonMapper().readTree(GSON.toJson(headers));
            for (String key : FILTER) {
                List<JsonNode> parents = node.findParents(key);
                for (JsonNode parent : parents) {
                    ((ObjectNode) parent).remove(key);
                }
            }
            return GSON.fromJson(node.toString(), Map.class);
        } catch (JsonProcessingException e) {
            log.warn("Error processing headers", e);
            return headers;
        }
    }

}
