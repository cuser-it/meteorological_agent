package com.shenzhen.meteorologicalagent.domain.weather;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;

public record RainForecast(
        @NotBlank(message = "rainForecast.level must not be blank")
        String level,
        String amountRange,
        String peakPeriod,
        String trend,
        @DecimalMin(value = "0.0", message = "rainForecast.confidence must be greater than or equal to 0")
        @DecimalMax(value = "1.0", message = "rainForecast.confidence must be less than or equal to 1")
        Double confidence
) {
}
