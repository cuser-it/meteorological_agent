package com.shenzhen.meteorologicalagent.domain.ai;

import java.util.List;

public record StructuredForecast(
        String summary,
        String details,
        String warning,
        List<String> affectedRegions,
        List<String> riskSignals
) {

    public StructuredForecast {
        affectedRegions = affectedRegions == null ? List.of() : List.copyOf(affectedRegions);
        riskSignals = riskSignals == null ? List.of() : List.copyOf(riskSignals);
    }
}
