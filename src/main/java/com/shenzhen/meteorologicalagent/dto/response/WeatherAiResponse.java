package com.shenzhen.meteorologicalagent.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.shenzhen.meteorologicalagent.domain.ai.AIResponse;
import com.shenzhen.meteorologicalagent.domain.ai.EvaluationResult;
import com.shenzhen.meteorologicalagent.domain.ai.IntentResult;
import com.shenzhen.meteorologicalagent.domain.ai.WorkflowTrace;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record WeatherAiResponse(
        String conversationId,
        String sessionId,
        Integer previousVersion,
        int version,
        IntentResult intent,
        AIResponse aiResponse,
        List<String> changes,
        EvaluationResult evaluation,
        WorkflowTrace trace
) {

    public WeatherAiResponse {
        changes = changes == null ? List.of() : List.copyOf(changes);
    }

    public WeatherAiResponse(
            String conversationId,
            String sessionId,
            Integer previousVersion,
            int version,
            IntentResult intent,
            AIResponse aiResponse,
            List<String> changes
    ) {
        this(conversationId, sessionId, previousVersion, version, intent, aiResponse, changes, null, null);
    }
}
