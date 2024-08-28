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

package com.ericsson.oss.mediation.o1.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import lombok.experimental.UtilityClass;

/**
 * Contains utility methods that can be used to manipulate the value of the alarms 'href' field.
 */
@UtilityClass
public class HrefUtil {
    public static final String ME_CONTEXT = "MeContext";

    /**
     * Extracts the LDN of the managed object that generated the alarm.
     *
     * @return the LDN of the managed object or an empty string if none was found.
     */
    public static String extractLdn(final String href) {
        final String hrefWithoutHttpPrefix = removeHttpPrefix(href);

        if (hrefWithoutHttpPrefix.indexOf("/") >= 0) {
            return hrefWithoutHttpPrefix.substring(hrefWithoutHttpPrefix.indexOf("/") + 1, hrefWithoutHttpPrefix.length()).replace("/", ",");
        }
        return StringUtils.EMPTY;
    }

    /**
     * Extracts the dnPrefix (i.e. MeContext FDN) from the 'href' value.
     *
     * @return the FDN of the dnPrefix or an empty string if it's not found.
     */
    public static String extractDnPrefix(final String href) {

        if (href == null || !href.contains(ME_CONTEXT)) {
            return "";
        }
        final String hrefWithoutHttpPrefix = removeHttpPrefix(href);

        final String dnPrefixReversed = hrefWithoutHttpPrefix.substring(0,
                hrefWithoutHttpPrefix.indexOf("/") >= 0 ? hrefWithoutHttpPrefix.indexOf("/") : hrefWithoutHttpPrefix.length());

        final List<String> dnPrefixParts = Arrays.asList(dnPrefixReversed.split("\\."));
        Collections.reverse(dnPrefixParts);

        final StringBuilder dnPrefix = new StringBuilder();
        for (int i = 0; i < dnPrefixParts.size(); i++) {
            dnPrefix.append(dnPrefixParts.get(i));

            if (i == dnPrefixParts.size() - 1) {
                break;
            }
            if (i % 2 == 0) {
                dnPrefix.append("=");
            } else {
                dnPrefix.append(",");
            }
        }
        return dnPrefix.toString();
    }

    private String removeHttpPrefix(final String href) {
        if (StringUtils.isBlank(href)) {
            throw new IllegalArgumentException("Alarm is missing mandatory 'href' field.");
        }
        return href.replaceFirst("https?://", "");
    }
}
