package com.shenzhen.meteorologicalagent.domain.conversation;

import com.shenzhen.meteorologicalagent.domain.ai.AIResponse;
import com.shenzhen.meteorologicalagent.domain.ai.PromptSnapshot;
import com.shenzhen.meteorologicalagent.domain.weather.WeatherContext;
import java.time.OffsetDateTime;
import java.util.List;

public record Conversation(
        String conversationId,
        String sessionId,
        ConversationStatus status,
        WeatherContext lastWeatherContext,
        AIResponse lastResponse,
        PromptSnapshot lastPrompt,
        int currentVersion,
        List<ConversationMessage> messages,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public Conversation {
        messages = messages == null ? List.of() : List.copyOf(messages);
    }
}
