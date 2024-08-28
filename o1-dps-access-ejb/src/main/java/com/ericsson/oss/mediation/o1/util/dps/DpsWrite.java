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

package com.ericsson.oss.mediation.o1.util.dps;

import java.util.Map;

import javax.ejb.Stateless;

import com.ericsson.oss.itpf.datalayer.dps.BucketProperties;
import com.ericsson.oss.itpf.datalayer.dps.DataBucket;
import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService;
import com.ericsson.oss.itpf.sdk.core.annotation.EServiceRef;

/**
 * Class for write only access to DPS.
 * <p>
 * Each method requires an existing transaction i.e. the flow transaction.
 * <p>
 * Mediation is suppressed.
 */
@Stateless
public class DpsWrite {

    @EServiceRef
    private DataPersistenceService dps;

    /**
     * Sets a map of attributes for an MO of the FDN provided.
     *
     * @param fdn
     *            The FDN of the MO.
     * @param attributes
     *            the map of attributes to set.
     */
    public void setAttributeValues(final String fdn, final Map<String, Object> attributes) {
        getLiveBucket().findMoByFdn(fdn).setAttributes(attributes);
    }

    private DataBucket getLiveBucket() {
        dps.setWriteAccess(true);
        return dps.getDataBucket("live", BucketProperties.SUPPRESS_MEDIATION);
    }
}
