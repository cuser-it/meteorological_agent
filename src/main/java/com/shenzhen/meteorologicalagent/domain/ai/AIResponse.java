package com.shenzhen.meteorologicalagent.domain.ai;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AIResponse(
        String responseId,
        String content,
        Map<String, String> structuredSections,
        String modelName,
        String promptName,
        String promptVersion,
        int inputTokens,
        int outputTokens,
        long latencyMs,
        StructuredForecast structuredOutput
) {

    public AIResponse {
        structuredSections = structuredSections == null ? Map.of() : Map.copyOf(structuredSections);
    }

    public AIResponse(
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
        this(
                responseId,
                content,
                structuredSections,
                modelName,
                promptName,
                promptVersion,
                inputTokens,
                outputTokens,
                latencyMs,
                null
        );
    }
}
