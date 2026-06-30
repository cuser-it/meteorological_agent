package com.shenzhen.meteorologicalagent.dto.response;

import java.util.List;

public record PromptCatalogResponse(
        List<PromptSummaryResponse> prompts
) {

    public PromptCatalogResponse {
        prompts = prompts == null ? List.of() : List.copyOf(prompts);
    }
}
