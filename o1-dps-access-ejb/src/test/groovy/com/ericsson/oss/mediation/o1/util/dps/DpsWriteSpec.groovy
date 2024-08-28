package com.ericsson.oss.mediation.o1.util.dps

import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.itpf.datalayer.dps.stub.RuntimeConfigurableDps
import spock.lang.Unroll

import javax.inject.Inject

class DpsWriteSpec extends CdiSpecification {

    @Inject
    DpsRead dpsRead

    @ObjectUnderTest
    DpsWrite dpsWrite

    @Unroll
    def "Test setAttributeValues"() {
        given:
            RuntimeConfigurableDps configurableDps = cdiInjectorRule.getService(RuntimeConfigurableDps)
            configurableDps.withTransactionBoundaries()
            configurableDps.addManagedObject()
                    .withFdn("MeContext=TestNode1")
                    .build();
        when:
            dpsWrite.setAttributeValues("MeContext=TestNode1", [myAtrribute: "myValue"])
        then:
            dpsRead.getAttributeValue(fdn, attribute) == expected
        where: ""
            fdn                                 | attribute          | expected
            "MeContext=TestNode1"               | "myAtrribute"      | "myValue"
            "MeContext=TestNode1"               | "myOtherAtrribute" | null
            "MeContext=TestNode1,NonExistent=1" | "myAtrribute"      | null
    }
}
