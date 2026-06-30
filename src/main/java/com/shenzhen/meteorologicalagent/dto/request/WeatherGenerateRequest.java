package com.shenzhen.meteorologicalagent.dto.request;

import com.shenzhen.meteorologicalagent.domain.weather.WeatherContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record WeatherGenerateRequest(
        @NotBlank(message = "sessionId must not be blank")
        String sessionId,
        String style,
        String outputFormat,
        @NotNull(message = "weatherContext must not be null")
        @Valid
        WeatherContext weatherContext
) {

    public WeatherGenerateRequest {
        style = style == null || style.isBlank() ? "FORMAL" : style;
        outputFormat = outputFormat == null || outputFormat.isBlank() ? "STANDARD_FORECAST" : outputFormat;
    }
}
