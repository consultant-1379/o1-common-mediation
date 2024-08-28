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

import com.ericsson.oss.mediation.util.netconf.api.Filter;

public class XPathFilter implements Filter {

    protected static final String TYPE = "xpath";
    protected final String filter;

    public XPathFilter(final String filter) {
        this.filter = filter;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public String asString() {
        return filter;
    }

    @Override
    public String toString() {
        return "XPathFilter{" +
                "filter='" + filter + '\'' +
                '}';
    }
}
