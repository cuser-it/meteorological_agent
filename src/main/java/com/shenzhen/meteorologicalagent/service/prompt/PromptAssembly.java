package com.shenzhen.meteorologicalagent.service.prompt;

import com.shenzhen.meteorologicalagent.domain.ai.PromptSnapshot;
import java.util.List;

public record PromptAssembly(
        PromptName promptName,
        String version,
        List<PromptModule> modules,
        PromptSnapshot snapshot
) {

    public PromptAssembly {
        modules = modules == null ? List.of() : List.copyOf(modules);
    }
}
