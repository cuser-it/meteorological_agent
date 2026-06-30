package com.shenzhen.meteorologicalagent.service.prompt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shenzhen.meteorologicalagent.common.ErrorCode;
import com.shenzhen.meteorologicalagent.common.exception.BusinessException;
import com.shenzhen.meteorologicalagent.domain.ai.IntentResult;
import com.shenzhen.meteorologicalagent.domain.ai.PromptSnapshot;
import com.shenzhen.meteorologicalagent.domain.conversation.Conversation;
import com.shenzhen.meteorologicalagent.dto.request.WeatherGenerateRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class PromptBuilder {

    private final PromptManager promptManager;
    private final ObjectMapper objectMapper;

    public PromptBuilder(PromptManager promptManager, ObjectMapper objectMapper) {
        this.promptManager = promptManager;
        this.objectMapper = objectMapper;
    }

    public PromptSnapshot buildGeneratePrompt(WeatherGenerateRequest request, IntentResult intentResult) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("task", "GENERATE_NOWCAST_FORECAST");
        payload.put("style", request.style());
        payload.put("outputFormat", request.outputFormat());
        payload.put("intent", intentResult);
        payload.put("weatherContext", request.weatherContext());
        return promptManager.snapshot(PromptName.GENERATE, renderPayload(payload));
    }

    public PromptSnapshot buildRewritePrompt(Conversation conversation, String userInstruction, IntentResult intentResult) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("task", "REWRITE_NOWCAST_FORECAST");
        payload.put("userInstruction", userInstruction);
        payload.put("intent", intentResult);
        payload.put("weatherContext", conversation.lastWeatherContext());
        payload.put("previousResponse", conversation.lastResponse());
        payload.put("currentVersion", conversation.currentVersion());
        return promptManager.snapshot(PromptName.REWRITE, renderPayload(payload));
    }

    private String renderPayload(Map<String, Object> payload) {
        try {
            return "## Runtime Payload" + System.lineSeparator()
                    + objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "failed to render prompt payload");
        }
    }
}
