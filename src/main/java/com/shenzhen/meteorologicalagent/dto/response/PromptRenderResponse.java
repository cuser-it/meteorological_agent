package com.shenzhen.meteorologicalagent.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PromptRenderResponse(
        String promptName,
        String promptVersion,
        String contentHash,
        int promptLength,
        String systemPrompt,
        String userPrompt,
        List<String> moduleNames,
        Map<String, Integer> moduleLengths,
        Map<String, Object> renderMetadata
) {

    public PromptRenderResponse {
        moduleNames = moduleNames == null ? List.of() : List.copyOf(moduleNames);
        moduleLengths = moduleLengths == null ? Map.of() : Map.copyOf(moduleLengths);
        renderMetadata = renderMetadata == null ? Map.of() : Map.copyOf(renderMetadata);
    }
}
