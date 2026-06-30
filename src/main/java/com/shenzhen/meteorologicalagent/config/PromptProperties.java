package com.shenzhen.meteorologicalagent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.prompt")
public record PromptProperties(
        String basePath,
        String activeVersion
) {
}

