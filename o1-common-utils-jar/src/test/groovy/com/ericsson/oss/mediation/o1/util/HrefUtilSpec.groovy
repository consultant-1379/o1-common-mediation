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

package com.ericsson.oss.mediation.o1.util

import com.ericsson.cds.cdi.support.spock.CdiSpecification
class HrefUtilSpec extends CdiSpecification {

        def "Test HrefUtil with null value"() {

            when: "href value is null"
                HrefUtil.extractLdn(null)

            then: "Exception is thrown"
                def e = thrown(IllegalArgumentException.class)
                assert e.message.contains("Alarm is missing mandatory 'href' field.")
        }

        def "Test HrefUtil to extract ldn from href"() {

            when: "href value is present with http"
                String result = HrefUtil.extractLdn("http://cucp.MeContext.skylight.SubNetwork/ManagedElement=1/GNBCUCPFunction=1")

            then: "ldn has been created from href"
                assert result == "ManagedElement=1,GNBCUCPFunction=1"
        }

        def "Test HrefUtil to extract Dnprefix from href"() {

            when: "href value is present with https"
                String result = HrefUtil.extractDnPrefix("https://cucp.MeContext.skylight.SubNetwork/ManagedElement=1/GNBCUCPFunction=1")

            then: "Dnprefix has been created from href"
                assert result == "SubNetwork=skylight,MeContext=cucp"
        }
}
