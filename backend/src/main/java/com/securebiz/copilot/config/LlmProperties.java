package com.securebiz.copilot.config;import org.springframework.boot.context.properties.ConfigurationProperties;
@ConfigurationProperties(prefix="securebiz.llm")public record LlmProperties(boolean enabled,String endpoint,String apiKey,String model,String provider){}
