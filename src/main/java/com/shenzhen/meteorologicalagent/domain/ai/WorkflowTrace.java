package com.shenzhen.meteorologicalagent.domain.ai;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record WorkflowTrace(
        String traceId,
        String conversationId,
        String sessionId,
        String workflowType,
        String status,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt,
        long latencyMs,
        List<WorkflowTraceStep> steps,
        Map<String, Object> metadata
) {

    public WorkflowTrace {
        steps = steps == null ? List.of() : List.copyOf(steps);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
