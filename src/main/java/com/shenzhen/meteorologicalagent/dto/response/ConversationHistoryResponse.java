package com.shenzhen.meteorologicalagent.dto.response;

import com.shenzhen.meteorologicalagent.domain.conversation.ConversationStatus;
import java.util.List;

public record ConversationHistoryResponse(
        String conversationId,
        String sessionId,
        ConversationStatus status,
        int currentVersion,
        List<ConversationMessageResponse> messages
) {

    public ConversationHistoryResponse {
        messages = messages == null ? List.of() : List.copyOf(messages);
    }
}
