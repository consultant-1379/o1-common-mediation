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
package com.ericsson.oss.mediation.o1.event.service;

import com.ericsson.oss.mediation.o1.event.exception.O1ValidationException;
import com.ericsson.oss.mediation.o1.event.model.O1Notification;
import com.ericsson.oss.mediation.o1.event.validator.O1NotificationValidator;
import org.json.JSONObject;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class O1NotificationNormalizer {

    private static final String VES_TOP_LEVEL_KEY = "event";
    private static final List<String> VES_SPECIFIC_KEYS = Arrays.asList(VES_TOP_LEVEL_KEY, "commonEventHeader", "stndDefinedFields", "data");

    @Inject
    O1NotificationValidator o1NotificationValidator;

    /***
     * Normalizes the notification.
     *
     * @param notification the notification as JSON string
     * @return the normalized notification
     * @throws O1ValidationException if the notification invalid and cannot be normalized
     */
    public O1Notification normalize(String notification) throws O1ValidationException {
        JSONObject jsonNotification = new JSONObject(notification);
        o1NotificationValidator.validate(jsonNotification);

        Map<String, Object> normalisedNotification = normaliseO1Notification(jsonNotification);
        return new O1Notification(normalisedNotification);
    }

    /**
     * Normalises an a notification based on JSONObject and maps the contents of the {@code VES_SPECIFIC_KEYS} into a flattened map.
     *
     * @param jsonNotification
     *            the notification as a JSON object
     */
    public static Map<String, Object> normaliseO1Notification(final JSONObject jsonNotification) {
        Map<String, Object> normalized = new HashMap<>();
        normalise(VES_TOP_LEVEL_KEY, jsonNotification.toMap(), normalized);
        return normalized;
    }

    @SuppressWarnings("unchecked")
    private static void normalise(final String key, final Object value, final Map<String, Object> normalisedEvent) {
        if (value instanceof Map<?, ?>) {
            if (VES_SPECIFIC_KEYS.contains(key)) {
                ((Map<String, ?>) value).entrySet().forEach(e -> normalise(e.getKey(), e.getValue(), normalisedEvent));
                return;
            }
            normalisedEvent.put(key, value);
            return;
        }
        normalisedEvent.put(key, value);
    }
}
