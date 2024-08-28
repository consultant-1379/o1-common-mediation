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

package com.ericsson.oss.mediation.o1.event.validator;

import java.io.InputStream;
import java.util.Set;

import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import org.json.JSONObject;

import com.ericsson.oss.mediation.o1.event.exception.O1ValidationException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.ValidationMessage;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class O1NotificationValidator {

    public static final String ONAP_VES_JSON_SCHEMA = "CommonEventFormat_30.2.1_ONAP.json";

    public void validate(final JSONObject jsonNotification) {
        log.debug("Validating notification: {}", jsonNotification);
        conformsToSchema(jsonNotification, getJsonSchema());
    }

    private void conformsToSchema(final JSONObject payload, final JsonSchema schema) {

        Set<ValidationMessage> messageSet;
        try {
            final ObjectMapper mapper = new ObjectMapper();
            final String content = payload.toString();
            final JsonNode node = mapper.readTree(content);
            messageSet = schema.validate(node);
        } catch (final Exception e) {
            throw new O1ValidationException("Failed to validate the payload using schema", e);
        }

        if (!messageSet.isEmpty()) {
            messageSet.forEach(it -> log.trace("Schema validation error: {}", it.getMessage()));
            throw new O1ValidationException("Schema validation failed: " + messageSet);
        }
    }

    private JsonSchema getJsonSchema() {
        log.debug("creating CommonEvent JsonSchema");

        final InputStream inputStream = getResource(ONAP_VES_JSON_SCHEMA);
        final JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);
        final JsonSchema schema = factory.getSchema(inputStream);

        log.debug("CommonEvent JsonSchema created successfully");
        return schema;
    }

    private InputStream getResource(final String fileName) {
        return getClass().getClassLoader().getResourceAsStream(fileName);
    }
}
