package com.shenzhen.meteorologicalagent.domain.weather;

import java.time.OffsetDateTime;

public record RegionForecast(
        String regionName,
        String rainLevel,
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        String impact
) {
}
