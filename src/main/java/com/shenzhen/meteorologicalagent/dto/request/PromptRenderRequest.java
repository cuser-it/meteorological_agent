package com.shenzhen.meteorologicalagent.dto.request;

import com.shenzhen.meteorologicalagent.domain.ai.IntentType;
import com.shenzhen.meteorologicalagent.domain.weather.WeatherContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record PromptRenderRequest(
        @NotNull(message = "promptName must not be null")
        String promptName,
        String style,
        String outputFormat,
        String userInstruction,
        IntentType intent,
        @Valid
        WeatherContext weatherContext,
        String previousResponse
) {
}
