package com.shenzhen.meteorologicalagent.dto.request;

import com.shenzhen.meteorologicalagent.domain.weather.WeatherContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

public record EvaluationRequest(
        @NotBlank(message = "content must not be blank")
        String content,
        @Valid
        WeatherContext weatherContext
) {
}
