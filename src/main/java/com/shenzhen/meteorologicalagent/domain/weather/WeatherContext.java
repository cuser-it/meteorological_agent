package com.shenzhen.meteorologicalagent.domain.weather;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.List;

public record WeatherContext(
        @NotBlank(message = "city must not be blank")
        String city,
        @NotNull(message = "forecastTime must not be null")
        OffsetDateTime forecastTime,
        @NotBlank(message = "validPeriod must not be blank")
        String validPeriod,
        @Valid
        RadarInfo radarInfo,
        @NotNull(message = "rainForecast must not be null")
        @Valid
        RainForecast rainForecast,
        List<@Valid RegionForecast> regionForecasts,
        List<String> riskSignals,
        String dataSource
) {

    public WeatherContext {
        city = city == null || city.isBlank() ? "深圳" : city;
        regionForecasts = regionForecasts == null ? List.of() : List.copyOf(regionForecasts);
        riskSignals = riskSignals == null ? List.of() : List.copyOf(riskSignals);
    }
}
