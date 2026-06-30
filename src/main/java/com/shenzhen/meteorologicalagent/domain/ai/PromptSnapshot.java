package com.shenzhen.meteorologicalagent.domain.ai;

import com.shenzhen.meteorologicalagent.service.prompt.PromptName;

public record PromptSnapshot(
        PromptName promptName,
        String promptVersion,
        String systemPrompt,
        String userPrompt,
        String contentHash,
        int promptLength
) {
}
