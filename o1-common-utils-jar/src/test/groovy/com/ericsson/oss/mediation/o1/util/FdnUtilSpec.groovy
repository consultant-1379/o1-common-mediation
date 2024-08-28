package com.ericsson.oss.mediation.o1.util

import com.ericsson.cds.cdi.support.spock.CdiSpecification
import spock.lang.Unroll

class FdnUtilSpec extends CdiSpecification {

    def 'test getParentFdn'() {
        given: 'an fdn with at least two MOs in the hierarchy'
            String networkElementFdn = FdnUtil.getParentFdn("NetworkElement=TestNode,FmAlarmSupervision=1")

        expect: "the parent fdn is returned"
            networkElementFdn == "NetworkElement=TestNode"
    }

    def 'test getParentFdn with no parent'() {
        when: 'an fdn has no parent'
            FdnUtil.getParentFdn("NetworkElement=TestNode")

        then: 'exception is thrown'
            thrown StringIndexOutOfBoundsException
    }

    def 'test getNetworkElementFdn'() {
        given: 'an MeContext fdn'
            String networkElementFdn = FdnUtil.getNetworkElementFdn("SubNetwork=A,SubNetwork=B,MeContext=O1")

        expect: 'the associated NetworkElement fdn is returned'
            networkElementFdn == "NetworkElement=O1"
    }

    @Unroll
    def "test getMeContextFdn scenarios"(fdn, expected) {
        expect:
            assert FdnUtil.getMeContextFdn(fdn) == expected
        where: ""
            fdn                                                                   | expected
            "SubNetwork=A,SubNetwork=B,MeContext=O1,ManagedElement=1,AlarmList=1" | "SubNetwork=A,SubNetwork=B,MeContext=O1"
            "SubNetwork=A,SubNetwork=B,MeContext=O1,ManagedElement=1"             | "SubNetwork=A,SubNetwork=B,MeContext=O1"
            "MeContext=O1,ManagedElement=1,AlarmList=1"                           | "MeContext=O1"
            "MeContext=O1,ManagedElement=1"                                       | "MeContext=O1"
            "MeContext=O1"                                                        | "MeContext=O1"
            "SomeNotValidFdn=O1"                                                  | ""
    }

    @Unroll
    def "test extractMeContextFdn scenarios"(fdn, expected) {
        expect:
            assert FdnUtil.extractMeContextFdn(fdn) == expected
        where: ""
            fdn                                                                   | expected
            "SubNetwork=A,SubNetwork=B,MeContext=O1,ManagedElement=1,AlarmList=1" | "SubNetwork=A,SubNetwork=B,MeContext=O1"
            "SubNetwork=A,SubNetwork=B,MeContext=O1,ManagedElement=1"             | "SubNetwork=A,SubNetwork=B,MeContext=O1"
            "MeContext=O1,ManagedElement=1,AlarmList=1"                           | "MeContext=O1"
            "MeContext=O1,ManagedElement=1"                                       | "MeContext=O1"
    }

    @Unroll
    def "test getMeContextId scenarios"(fdn, expected) {
        expect:
            assert FdnUtil.getMeContextId(fdn) == expected
        where: ""
            fdn                                      | expected
            "SubNetwork=A,SubNetwork=B,MeContext=O1" | "O1"
            "MeContext=O1"                           | "O1"
    }

    @Unroll
    def "test createFdn scenarios"(rdns, expected) {
        expect:
            assert FdnUtil.createFdn(rdns.toArray(new String[0])) == expected
        where: ""
            rdns                                                        | expected
            ["NetworkElement=nodeName"]                                 | "NetworkElement=nodeName"
            ["NetworkElement=nodeName", "ConnectivityInfo=1"]           | "NetworkElement=nodeName,ConnectivityInfo=1"
            ["NetworkElement=nodeName", "ConnectivityInfo=1", "B=beta"] | "NetworkElement=nodeName,ConnectivityInfo=1,B=beta"
            ["SubNetwork=A,SubNetwork=B", "MeContext=O1"]               | "SubNetwork=A,SubNetwork=B,MeContext=O1"
            ["SubNetwork=A,SubNetwork=B", "MeContext=O1,AlarmList=1"]   | "SubNetwork=A,SubNetwork=B,MeContext=O1,AlarmList=1"
            ["SubNetwork=A,SubNetwork=B", ""]                           | "SubNetwork=A,SubNetwork=B"
            ["", "MeContext=01"]                                        | "MeContext=01"
            ["SubNetwork=A,SubNetwork=B"]                               | "SubNetwork=A,SubNetwork=B"
            []                                                          | ""
    }

    def 'test getManagedElementFdn'() {
        given: 'the MeContext fdn and the ManagedElement ldn'
            String managedElementFdn = FdnUtil.getManagedElementFdn("SubNetwork=A,MeContext=O1", "TestNode")

        expect: 'the ManagedElement fdn is returned'
            managedElementFdn == "SubNetwork=A,MeContext=O1,ManagedElement=TestNode"
    }
}