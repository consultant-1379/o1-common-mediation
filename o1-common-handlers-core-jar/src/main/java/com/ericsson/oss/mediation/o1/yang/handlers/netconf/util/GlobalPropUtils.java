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

package com.ericsson.oss.mediation.o1.yang.handlers.netconf.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public class GlobalPropUtils {

    private static String globalPropertiesFile = "/ericsson/tor/data/global.properties";

    static {
        initGlobalProperties();
    }
    private static Properties globalProperties;

    public static Properties getGlobalProperties() {
        return globalProperties;
    }

    public static void initGlobalProperties() {
        globalProperties = readProperties(globalPropertiesFile);
    }

    public static Properties readProperties(final String path) {
        final Properties prop = new Properties();
        try (InputStream input = new FileInputStream(path)) {
            prop.load(input);
        } catch (final IOException ex) {
            log.error("Error reading Global Properties File: {}", ex.getMessage());
        }
        return prop;
    }

    public static <T> T getGlobalValue(final String value, final Class<T> expectedType) {
        final T propertyValue = expectedType.cast(globalProperties.getProperty(value));
        if (propertyValue == null) {
            throw new IllegalArgumentException(String.format("Global Properties file value %s was not found", value));
        }
        if (expectedType == String.class && ((String) propertyValue).isEmpty()) {
            throw new IllegalArgumentException(String.format("Global Properties file value %s is empty", value));
        }
        return propertyValue;
    }
}
