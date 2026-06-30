package com.shenzhen.meteorologicalagent.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ConversationResetRequest(
        @NotBlank(message = "conversationId must not be blank")
        String conversationId,
        @NotBlank(message = "sessionId must not be blank")
        String sessionId
) {
}
