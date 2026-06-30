package com.shenzhen.meteorologicalagent.domain.ai;

import java.util.Map;

public record IntentResult(
        IntentType intent,
        double confidence,
        Map<String, Object> parameters,
        String reason,
        boolean requiresExistingConversation
) {

    public IntentResult {
        parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
    }
}
