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

package com.ericsson.oss.mediation.o1.yang.handlers.netconf.api;

import lombok.experimental.UtilityClass;

@UtilityClass
public final class CommonConstants {

    public static final String CREATE = "CREATE";
    public static final String FDN = "fdn";

    public static final String ID = "id";

    // MediationTaskRequest constants
    public static final String ACTIVE = "active";
    public static final String INCLUDE_NS_PREFIX = "setIncludeNsPrefix";

    // Model Related
    public static final String MANAGED_ELEMENT = "ManagedElement";
    public static final String MANAGED_ELEMENT_ID = "ManagedElementId";
    public static final String NAMESPACE = "urn:3gpp:sa5:_3gpp-common-managed-element";
    public static final String NE_TYPE = "neType";
    public static final String NTF_SUBSCRIPTION_CONTROL = "NtfSubscriptionControl";
    public static final String VERSION = "2023.2.14";

    // Netconf Related
    public static final String MERGE = "MERGE";
}
