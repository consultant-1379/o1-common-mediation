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

import static com.ericsson.oss.mediation.o1.util.FdnUtil.ME_CONTEXT;
import static com.ericsson.oss.mediation.o1.util.FdnUtil.NETWORK_ELEMENT;

import java.util.Collection;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import org.apache.commons.lang3.StringUtils;

import com.ericsson.oss.itpf.datalayer.dps.BucketProperties;
import com.ericsson.oss.itpf.datalayer.dps.DataBucket;
import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService;
import com.ericsson.oss.itpf.datalayer.dps.exception.general.DpsIllegalStateException;
import com.ericsson.oss.itpf.datalayer.dps.exception.general.ObjectNotFoundException;
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.itpf.sdk.core.annotation.EServiceRef;
import com.ericsson.oss.mediation.o1.util.FdnUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * Class for read only access to DPS.
 * <p>
 * A new transaction is used to prevent it running as part of the flow transaction and terminating the supervision flow prematurely.
 * <p>
 * Read only transactions do not need to be rolled back.
 * <p>
 * Mediation is suppressed.
 */
@Slf4j
@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
public class DpsRead {

    public static final String NODE_ROOT_REF = "nodeRootRef";
    public static final String NETWORK_ELEMENT_REF = "networkElementRef";
    @EServiceRef
    private DataPersistenceService dps;

    /**
     * Gets the attribute value for the attribute name specified.
     *
     * @param fdn
     *            the FDN of the MO.
     * @param attributeName
     *            the MO attribute name.
     * @return the attribute value or else null.
     * @param <T>
     */
    public <T> T getAttributeValue(final String fdn, final String attributeName) {
        final ManagedObject mo = getLiveBucket().findMoByFdn(fdn);
        if (mo != null) {
            return mo.getAttribute(attributeName);
        } else {
            log.error("MO not found for fdn [{}]", fdn);
            return null;
        }
    }

    /**
     * Retrieves the FDN of the Network Element associated with the MeContext contained in the {@code fdn} provided.
     *
     * @param fdnContainingMeContext
     *            Any FDN from the left hand side that contains the MeContext MO.
     * @return The FDN of the Network Element associated with the MeContext, if found.
     * @throws ObjectNotFoundException
     *             If the FDN of the Network Element for the MeContext is not found.
     */
    public String getNetworkElementFdn(final String fdnContainingMeContext) {
        final String meContextFdn = FdnUtil.getMeContextFdn(fdnContainingMeContext);
        if (StringUtils.isNotBlank(meContextFdn)) {
            final ManagedObject meContextMO = getLiveBucket().findMoByFdn(meContextFdn);
            if (meContextMO != null) {
                final Collection<PersistenceObject> associatedPos = meContextMO.getAssociations(NETWORK_ELEMENT_REF);
                for (final PersistenceObject po : associatedPos) {
                    if (po.getType().equals(NETWORK_ELEMENT)) {
                        return ((ManagedObject) po).getFdn();
                    }
                }
            }
        }
        throw new ObjectNotFoundException("Failed to find NetworkElement FDN for MeContext: " + meContextFdn);
    }

    /**
     * Retrieves the MeContext FDN via association for the provided Network Element FDN.
     *
     * @param networkElementFdn
     *            The FDN of the Network Element.
     * @return The MeContext FDN associated with the given Network Element FDN.
     */
    public String getMeContextFdn(final String networkElementFdn) {
        try {
            final ManagedObject networkElement = getLiveBucket().findMoByFdn(networkElementFdn);
            log.debug("Node model NetworkElement: {}", networkElement);
            final ManagedObject nodeRootRef = (ManagedObject) networkElement.getAssociations(NODE_ROOT_REF).iterator().next();
            log.debug("Node model NodeRoot: {}", nodeRootRef);
            if (ME_CONTEXT.equals(nodeRootRef.getType())) {
                return nodeRootRef.getFdn();
            }
        } catch (final Exception e) {
            log.error("Error finding node root reference MO for Network Element FDN {}", networkElementFdn, e);
            throw new DpsIllegalStateException("Failed to find node root reference MO for Network Element with FDN " + networkElementFdn);
        }
        return null;
    }

    private DataBucket getLiveBucket() {
        dps.setWriteAccess(false);
        return dps.getDataBucket("live", BucketProperties.SUPPRESS_MEDIATION);
    }
}
