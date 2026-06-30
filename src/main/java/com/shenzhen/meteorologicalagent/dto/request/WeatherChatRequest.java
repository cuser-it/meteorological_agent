package com.shenzhen.meteorologicalagent.dto.request;

import jakarta.validation.constraints.NotBlank;

public record WeatherChatRequest(
        @NotBlank(message = "conversationId must not be blank")
        String conversationId,
        @NotBlank(message = "sessionId must not be blank")
        String sessionId,
        @NotBlank(message = "message must not be blank")
        String message
) {
}
