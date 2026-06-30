package com.shenzhen.meteorologicalagent.domain.ai;

import com.shenzhen.meteorologicalagent.service.prompt.PromptName;
import java.util.List;
import java.util.Map;

public record PromptSnapshot(
        PromptName promptName,
        String promptVersion,
        String systemPrompt,
        String userPrompt,
        String contentHash,
        int promptLength,
        List<String> moduleNames,
        Map<String, Integer> moduleLengths,
        Map<String, Object> renderMetadata
) {

    public PromptSnapshot {
        moduleNames = moduleNames == null ? List.of() : List.copyOf(moduleNames);
        moduleLengths = moduleLengths == null ? Map.of() : Map.copyOf(moduleLengths);
        renderMetadata = renderMetadata == null ? Map.of() : Map.copyOf(renderMetadata);
    }

    public PromptSnapshot(
            PromptName promptName,
            String promptVersion,
            String systemPrompt,
            String userPrompt,
            String contentHash,
            int promptLength
    ) {
        this(promptName, promptVersion, systemPrompt, userPrompt, contentHash, promptLength, List.of(), Map.of(), Map.of());
    }
}
