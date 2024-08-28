package com.ericsson.oss.mediation.o1.yang.handlers.netconf.util

import com.ericsson.cds.cdi.support.spock.CdiSpecification

class GlobalPropUtilsSpec extends CdiSpecification {

    def 'Exception test for when global.properties file does not exist'() {
        given: 'Method is called and no properties files exists'
            GlobalPropUtils.globalPropertiesFile = 'src/test/resources/empty.properties'
        when:
            GlobalPropUtils.initGlobalProperties();
        then: 'Will return empty properties object'
            assert GlobalPropUtils.getGlobalProperties().isEmpty()
    }

    def "Test readProperties reads properties correctly"() {
        given: "Mocked FileInputStream"
            GlobalPropUtils.globalPropertiesFile = 'src/test/resources/global.properties'
            def mockInputStream = new ByteArrayInputStream("svc_FM_VIP_ipaddress=1.2.3.4".getBytes())
            def fileInputStream = Mock(FileInputStream)
            fileInputStream._constructor(GlobalPropUtils.globalPropertiesFile) >> { mockInputStream }

        when:
            Properties props = GlobalPropUtils.readProperties(GlobalPropUtils.globalPropertiesFile)

        then:
            props.getProperty("svc_FM_VIP_ipaddress") == "1.2.3.4"
    }

    def "Test getGlobalValue returns value from global properties"() {
        given:
            GlobalPropUtils.globalProperties.setProperty("svc_FM_VIP_ipaddress", "1.2.3.4")

        expect:
            GlobalPropUtils.getGlobalValue("svc_FM_VIP_ipaddress", String.class) == "1.2.3.4"
    }
}
