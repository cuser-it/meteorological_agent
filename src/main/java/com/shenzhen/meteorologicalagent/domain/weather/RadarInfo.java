package com.shenzhen.meteorologicalagent.domain.weather;

import java.time.OffsetDateTime;
import java.util.List;

public record RadarInfo(
        String echoIntensity,
        String movementDirection,
        String movementSpeed,
        List<String> affectedAreas,
        OffsetDateTime observedAt
) {

    public RadarInfo {
        affectedAreas = affectedAreas == null ? List.of() : List.copyOf(affectedAreas);
    }
}
