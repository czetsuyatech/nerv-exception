package com.czetsuyatech.nerv.exception.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@AutoConfiguration
@EnableConfigurationProperties(NervExceptionProperties.class)
public class NervExceptionPropertiesAutoConfiguration {

}
