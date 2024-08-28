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

package com.ericsson.oss.mediation.fm.o1.flows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ericsson.oss.mediation.adapter.netconf.jca.api.operation.NetconfOperationRequest;
import com.ericsson.oss.mediation.adapter.netconf.jca.xa.yang.provider.NetconfAttribute;
import com.ericsson.oss.mediation.adapter.netconf.jca.xa.yang.provider.YangDataNodeId;
import com.ericsson.oss.mediation.adapter.netconf.jca.xa.yang.provider.YangEditConfigOperation;
import com.ericsson.oss.mediation.adapter.netconf.jca.xa.yang_read.provider.YangGetConfigOperation;
import com.ericsson.oss.mediation.util.netconf.api.editconfig.Operation;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Builder
@Slf4j
public class SimpleNetconfOperation {
    Operation operation;
    String fdn;

    @Builder.Default
    String namespace = "urn:3gpp:sa5:_3gpp-common-managed-element";

    @Builder.Default
    String nodeType = "O1Node";

    Map<String, Object> attributes;

    public static SimpleNetconfOperation createFrom(final NetconfOperationRequest netconfOperationRequest) {
        SimpleNetconfOperation simpleNetconfOperation;
        if (netconfOperationRequest instanceof YangEditConfigOperation) {
            YangEditConfigOperation yangOperation = (YangEditConfigOperation) netconfOperationRequest;
            simpleNetconfOperation = SimpleNetconfOperation.builder()
                    .fdn(yangOperation.getFdn())
                    .namespace(yangOperation.getNamespace())
                    .nodeType(yangOperation.getNodeType())
                    .operation(yangOperation.getOperation())
                    .attributes(attributesFrom(yangOperation.getChildMOs()))
                    .build();
        } else {
            YangGetConfigOperation yangOperation = (YangGetConfigOperation) netconfOperationRequest;
            simpleNetconfOperation = SimpleNetconfOperation.builder()
                    .fdn(yangOperation.getFdn())
                    .namespace(yangOperation.getNamespace())
                    .nodeType(yangOperation.getNodeType())
                    .attributes(attributesFrom(yangOperation.getChildMOs()))
                    .build();
        }
        log.info(simpleNetconfOperation.toString());
        return simpleNetconfOperation;
    }

    private static Map<String, Object> attributesFrom(final List<YangDataNodeId> attributes) {
        Map<String, Object> mappedAttributes = new HashMap<>();
        for (YangDataNodeId yangDataNodeId : attributes) {
            mappedAttributes.put(yangDataNodeId.getName(), null);
            for (NetconfAttribute netconfAttribute : yangDataNodeId.getAttributes()) {
                String name = netconfAttribute.getName();
                String value = netconfAttribute.getAttributeInfo().getAttributeValue();
                if (mappedAttributes.get(name) == null) {
                    mappedAttributes.put(name, value);
                } else {
                    Object existingValue = mappedAttributes.get(name);
                    if (existingValue instanceof List) {
                        ((List) existingValue).add(value);
                    } else {
                        mappedAttributes.put(name, new ArrayList(Arrays.asList(existingValue, value)));
                    }
                }
            }
        }
        return mappedAttributes;
    }

    public static class SimpleNetconfOperationBuilder {
        Map<String, Object> attributes = new HashMap<>();

        public SimpleNetconfOperationBuilder attribute(String name) {
            return attribute(name, null);
        }

        public SimpleNetconfOperationBuilder attribute(String name, Object value) {
            attributes.put(name, value);
            return this;
        }
    }
}
