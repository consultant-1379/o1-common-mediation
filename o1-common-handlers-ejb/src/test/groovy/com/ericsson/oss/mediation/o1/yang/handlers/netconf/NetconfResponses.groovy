package com.ericsson.oss.mediation.o1.yang.handlers.netconf

import com.ericsson.oss.mediation.util.netconf.api.NetconfResponse

class NetconfResponses {

    static NetconfResponse okResponseSingleManagedElement() {
        NetconfResponse response = new NetconfResponse()
        response.setData("<ManagedElement\n" +
                "\txmlns=\"urn:3gpp:sa5:_3gpp-common-managed-element\">\n" +
                "\t<id>ocp83vcu03o1</id>\n" +
                "\t<ExternalDomain\n" +
                "\t\txmlns=\"urn:rdns:com:ericsson:oammodel:ericsson-external-domain-cr\">\n" +
                "\t\t<id>1</id>\n" +
                "\t</ExternalDomain>\n" +
                "</ManagedElement>\n")
        return response
    }

    static NetconfResponse okResponseMultipleManagedElement() {
        NetconfResponse response = new NetconfResponse()
        response.setData("<ManagedElement\n" +
                "\txmlns=\"urn:3gpp:sa5:_3gpp-common-managed-element\" xc:operation=\"merge\">\n" +
                "\t<id>ocp83vcu03o1</id>\n" +
                "\t<attributes>\n" +
                "\t\t<dnPrefix>MeContext=ocp83vcu03o1</dnPrefix>\n" +
                "\t</attributes>\n" +
                "</ManagedElement>\n" +
                "<ManagedElement\n" +
                "\txmlns=\"urn:3gpp:sa5:_3gpp-common-managed-element\" xc:operation=\"merge\">\n" +
                "\t<id>ocp83vcu03o2</id>\n" +
                "\t<attributes>\n" +
                "\t\t<dnPrefix>MeContext=ocp83vcu03o2</dnPrefix>\n" +
                "\t</attributes>\n" +
                "</ManagedElement>")
        return response
    }

    static NetconfResponse errorResponse() {
        NetconfResponse response = new NetconfResponse()
        response.setError(Boolean.TRUE)
        response.setErrorMessage("Error no data for query")
        return response
    }
}
