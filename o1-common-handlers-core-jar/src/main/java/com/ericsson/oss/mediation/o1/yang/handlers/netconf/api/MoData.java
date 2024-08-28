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

import java.util.Collection;
import java.util.Map;

import com.ericsson.oss.mediation.engine.api.MediationEngineConstants;
import com.ericsson.oss.mediation.util.netconf.api.editconfig.Operation;

import lombok.Data;
import lombok.ToString;

/**
 * Contains all information required to make the netconf call. Some obtained from flow header.
 */
@Data
public class MoData implements MediationEngineConstants {
    private String action;
    private String fdn;
    private String nameSpace;
    private String type;
    private String version;
    private Map<String, Object> actionAttributes;
    private Map<String, Object> originalAttributes;
    private Map<String, Object> createAttributes;
    @ToString.Exclude
    private Map<String, Object> headers;
    private Map<String, Object> modifyAttributes;
    private Collection<String> readAttributes;
    private String name;
    private Operation operation;
    private String parentFdn;
    private String nodeNamespace;
    private String nodeName;
    private boolean includeNsPrefix;
}
