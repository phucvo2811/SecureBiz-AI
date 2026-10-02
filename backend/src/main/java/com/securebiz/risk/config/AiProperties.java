package com.securebiz.risk.config;
import org.springframework.boot.context.properties.ConfigurationProperties;
@ConfigurationProperties(prefix="securebiz.ai")
public record AiProperties(String serviceUrl,String apiKey){}
