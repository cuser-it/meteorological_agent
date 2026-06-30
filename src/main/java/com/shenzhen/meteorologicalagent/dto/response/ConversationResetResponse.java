package com.shenzhen.meteorologicalagent.dto.response;

import com.shenzhen.meteorologicalagent.domain.conversation.ConversationStatus;

public record ConversationResetResponse(
        String conversationId,
        String sessionId,
        ConversationStatus status
) {
}
