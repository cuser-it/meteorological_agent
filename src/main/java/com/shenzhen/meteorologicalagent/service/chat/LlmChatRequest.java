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
        String userInstruction
) {
}
