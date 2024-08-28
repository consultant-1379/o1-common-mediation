package com.ericsson.oss.mediation.o1.util.dps

import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.itpf.datalayer.dps.exception.general.ObjectNotFoundException
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject
import com.ericsson.oss.itpf.datalayer.dps.stub.RuntimeConfigurableDps
import spock.lang.Unroll

class DpsReadSpec extends CdiSpecification {

    @ObjectUnderTest
    DpsRead dpsRead

    def "Test getMeContextFdn"() {
        given:
            RuntimeConfigurableDps configurableDps = cdiInjectorRule.getService(RuntimeConfigurableDps)
            configurableDps.withTransactionBoundaries()

            ManagedObject meContext = configurableDps.addManagedObject()
                    .withFdn("MeContext=TestNode1")
                    .build()

            configurableDps.addManagedObject()
                    .withFdn("NetworkElement=TestNode1")
                    .withAssociation(DpsRead.NODE_ROOT_REF, meContext)
                    .build()
        expect:
            assert dpsRead.getMeContextFdn("NetworkElement=TestNode1") == "MeContext=TestNode1"
    }

    def "Test getNetworkElementFdn"() {
        given:
            RuntimeConfigurableDps configurableDps = cdiInjectorRule.getService(RuntimeConfigurableDps)
            configurableDps.withTransactionBoundaries()

            ManagedObject networkElement = configurableDps.addManagedObject()
                    .withFdn("NetworkElement=TestNode1")
                    .build()

            configurableDps.addManagedObject()
                    .withFdn("MeContext=TestNode1")
                    .withAssociation(DpsRead.NETWORK_ELEMENT_REF, networkElement)
                    .build()

        expect:
            assert dpsRead.getNetworkElementFdn("MeContext=TestNode1") == "NetworkElement=TestNode1"
    }

    @Unroll
    def "Test getNetworkElementFdn not found"(fdn, exception) {
        when:
            dpsRead.getNetworkElementFdn(fdn)
        then:
            thrown exception
        where: ""
            fdn                                           | exception
            "AlarmList=1"                                 | ObjectNotFoundException
            "MeContext=TestNodeDoesNotExists"             | ObjectNotFoundException
            "MeContext=TestNodeDoesNotExists,AlarmList=1" | ObjectNotFoundException
    }
}
