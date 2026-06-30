package com.shenzhen.meteorologicalagent.domain.ai;

import java.util.Map;

public record AIResponse(
        String responseId,
        String content,
        Map<String, String> structuredSections,
        String modelName,
        String promptName,
        String promptVersion,
        int inputTokens,
        int outputTokens,
        long latencyMs
) {

    public AIResponse {
        structuredSections = structuredSections == null ? Map.of() : Map.copyOf(structuredSections);
    }
}
