package com.securebiz.document.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "securebiz.storage")
public record StorageProperties(String endpoint, String accessKey, String secretKey,
                                String documentBucket, String productBucket, long maxDocumentBytes) {}
