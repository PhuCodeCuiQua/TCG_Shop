package com.tcgshop.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(OtpProperties.class)
public class OtpPropertiesConfiguration {
}
