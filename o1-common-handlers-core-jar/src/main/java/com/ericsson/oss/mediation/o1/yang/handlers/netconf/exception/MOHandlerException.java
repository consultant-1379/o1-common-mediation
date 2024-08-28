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

package com.ericsson.oss.mediation.o1.yang.handlers.netconf.exception;

import lombok.experimental.StandardException;

/**
 * Generic Exception for MO Handlers.
 */
@StandardException
public class MOHandlerException extends RuntimeException {

    private static final long serialVersionUID = -258148023981853346L;

}
