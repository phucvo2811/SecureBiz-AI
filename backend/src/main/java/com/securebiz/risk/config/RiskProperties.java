package com.securebiz.risk.config;import org.springframework.boot.context.properties.ConfigurationProperties;
@ConfigurationProperties(prefix="securebiz.risk")public record RiskProperties(String policyVersion,double mlWeight,double contextWeight){}
