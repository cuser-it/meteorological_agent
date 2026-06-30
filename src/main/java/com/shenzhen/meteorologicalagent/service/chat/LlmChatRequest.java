package com.shenzhen.meteorologicalagent.service.chat;

import com.shenzhen.meteorologicalagent.domain.ai.AIResponse;
import com.shenzhen.meteorologicalagent.domain.ai.IntentResult;
import com.shenzhen.meteorologicalagent.domain.ai.PromptSnapshot;
import com.shenzhen.meteorologicalagent.domain.weather.WeatherContext;

public record LlmChatRequest(
        ChatTaskType taskType,
        PromptSnapshot prompt,
        WeatherContext weatherContext,
        AIResponse previousResponse,
        IntentResult intentResult,
        String userInstruction,
        String workflowTraceId
) {

    public LlmChatRequest(
            ChatTaskType taskType,
            com.shenzhen.meteorologicalagent.domain.ai.PromptSnapshot prompt,
            com.shenzhen.meteorologicalagent.domain.weather.WeatherContext weatherContext,
            AIResponse previousResponse,
            IntentResult intentResult,
            String userInstruction
    ) {
        this(taskType, prompt, weatherContext, previousResponse, intentResult, userInstruction, null);
    }
}
