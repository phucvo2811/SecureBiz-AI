package com.securebiz.copilot.config;import org.springframework.boot.context.properties.EnableConfigurationProperties;import org.springframework.context.annotation.Configuration;
@Configuration@EnableConfigurationProperties(LlmProperties.class)public class LlmConfig{}
