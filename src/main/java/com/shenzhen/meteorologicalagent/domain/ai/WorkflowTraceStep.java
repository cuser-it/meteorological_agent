package com.shenzhen.meteorologicalagent.domain.ai;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.OffsetDateTime;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record WorkflowTraceStep(
        String stepName,
        String status,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt,
        long latencyMs,
        Map<String, Object> attributes
) {

    public WorkflowTraceStep {
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }
}
