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
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import lombok.experimental.UtilityClass;

@UtilityClass
public class FdnUtil {

    public static final String NETWORK_ELEMENT = "NetworkElement";
    public static final String ME_CONTEXT = "MeContext";

    /**
     * Gets the parent FDN of a FDN.
     *
     * @param fdn
     *            the FDN
     *            e.g. SubNetwork=A,SubNetwork=B,MeContext=O1,GNBFunction=1
     * @return The parent FDN.
     *         e.g. SubNetwork=A,SubNetwork=B,MeContext=O1
     */
    public static String getParentFdn(final String fdn) {
        final int lastIndex = fdn.lastIndexOf(',');
        return fdn.substring(0, lastIndex);
    }

    /**
     * Gets the NetworkElement FDN from a mirror or topology FDN.
     *
     * @param fdn
     *            The MO FDN.
     *            e.g. SubNetwork=A,SubNetwork=B,MeContext=O1,GNBFunction=1 OR
     *                 NetworkElement=01,FmSupervision=1
     * @return The NetworkElement FDN.
     *         e.g. NetworkElement=O1
     */
    public static String getNetworkElementFdn(final String fdn) {
        if (fdn.startsWith(NETWORK_ELEMENT)) {
            return fdn.split(",")[0];
        }
        final String networkElementFdn = Arrays.stream(fdn.split(","))
                .filter(e -> e.contains(ME_CONTEXT))
                .collect(Collectors.joining());
        return networkElementFdn.replace(ME_CONTEXT, NETWORK_ELEMENT);
    }

    /**
     * Gets the FDN of the MeContext from the mirror FDN (left hand side FDN).
     *
     * @param mirrorFdn
     *            The mirror MO FDN.
     *            e.g. SubNetwork=A,SubNetwork=B,MeContext=O1,ManagedElement=1,AlarmList=1
     * @return The MeContext FDN.
     *         e.g. SubNetwork=A,SubNetwork=B,MeContext=O1
     */
    public static String getMeContextFdn(final String mirrorFdn) {
        if (mirrorFdn.contains("ManagedElement")) {
            return mirrorFdn.substring(0, mirrorFdn.indexOf(",ManagedElement"));
        } else if (mirrorFdn.contains("MeContext=")) {
            return mirrorFdn;
        } else {
            return "";
        }
    }

    /**
     * Returns an FDN String by joining the RDNs together using a comma.
     */
    public static String createFdn(final String... rdns) {
        return Arrays.stream(rdns)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.joining(","));
    }

    /**
     * Gets the id of the MeContext from the dnPrefix provided.
     *
     * @param dnPrefix
     *            Any dnPrefix.
     *            e.g. MeContext=O1
     * @return The MeContext ID.
     *         e.g. O1
     */
    public static String getMeContextId(String dnPrefix) {
        String key = "MeContext=";
        int indexOfMeContext = dnPrefix.indexOf(key);
        return dnPrefix.substring(indexOfMeContext + key.length()).trim();
    }

    /**
     * Generates the Managed Element FDN using the provided MeContext FDN and Managed Element ID.
     *
     * @param meContextFdn
     *            The MeContext FDN of the element.
     *            e.g. Subnetwork=A,MeContext=01
     * @param managedElementId
     *            The ID of the Managed Element.
     *            e.g. 1
     * @return A String representing the fully constructed Managed Element FDN.
     *         e.g. Subnetwork=A,MeContext=01,ManagedElement=1
     */
    public static String getManagedElementFdn(final String meContextFdn, final String managedElementId) {
        return String.join(",", meContextFdn, getManangedElementRdn(managedElementId));
    }

    /**
     * Extracts the MeContext FDN from the provided string containing the MeContext information.
     *
     * @param fdnContainingMeContext
     *            The string containing MeContext information.
     *            e.g. SubNetwork=A,SubNetwork=B,MeContext=O1,ManagedElement=1,AlarmList=1
     * @return The extracted MeContext FDN if found; otherwise, an empty string.
     *         e.g. SubNetwork=A,SubNetwork=B,MeContext=O1
     */
    public static String extractMeContextFdn(final String fdnContainingMeContext) {
        final int mecontextStartIndex = fdnContainingMeContext.indexOf(ME_CONTEXT);
        final int mecontextEndIndex = fdnContainingMeContext.indexOf(',', mecontextStartIndex);
        return fdnContainingMeContext.substring(0, mecontextEndIndex);
    }

    private static String getManangedElementRdn(String managedElementId) {
        return "ManagedElement=" + managedElementId;
    }
}
