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

package com.ericsson.oss.mediation.o1.event.model;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class O1Notification {

    public static final String FIELD_HREF = "href";
    public static final String FIELD_NOTIFICATION_TYPE = "notificationType";
    public static final String FIELD_SYSTEM_DN = "systemDN";

    private final HashMap<String, Object> normalisedNotification;

    public O1Notification(Map<String, Object> normalisedNotification) {
        this.normalisedNotification = new HashMap<>(normalisedNotification);
    }

    public String getNotificationType() {
        return (String) getNormalisedNotification().get(FIELD_NOTIFICATION_TYPE);
    }

    public String getHref() {
        return (String) getNormalisedNotification().get(FIELD_HREF);
    }

    public String getSystemDn() {
        return (String) getNormalisedNotification().get(FIELD_SYSTEM_DN);
    }
}
