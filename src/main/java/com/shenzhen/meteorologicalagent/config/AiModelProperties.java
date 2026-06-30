package com.shenzhen.meteorologicalagent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ai")
public record AiModelProperties(
        String provider,
        String model,
        int timeoutSeconds,
        int maxRetries
) {
}

