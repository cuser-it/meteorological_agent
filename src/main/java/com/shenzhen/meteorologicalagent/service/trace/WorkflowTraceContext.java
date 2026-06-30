package com.shenzhen.meteorologicalagent.service.trace;

import com.shenzhen.meteorologicalagent.domain.ai.WorkflowTraceStep;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class WorkflowTraceContext {

    private final String traceId;
    private final String workflowType;
    private final String sessionId;
    private final OffsetDateTime startedAt;
    private final List<WorkflowTraceStep> steps = new ArrayList<>();
    private final Map<String, Object> metadata = new LinkedHashMap<>();

    WorkflowTraceContext(String traceId, String workflowType, String sessionId) {
        this.traceId = traceId;
        this.workflowType = workflowType;
        this.sessionId = sessionId;
        this.startedAt = OffsetDateTime.now();
    }

    public String traceId() {
        return traceId;
    }

    public String workflowType() {
        return workflowType;
    }

    public String sessionId() {
        return sessionId;
    }

    public OffsetDateTime startedAt() {
        return startedAt;
    }

    public List<WorkflowTraceStep> steps() {
        return List.copyOf(steps);
    }

    public Map<String, Object> metadata() {
        return Map.copyOf(metadata);
    }

    void addStep(WorkflowTraceStep step) {
        steps.add(step);
    }

    public void putMetadata(String key, Object value) {
        if (key != null && value != null) {
            metadata.put(key, value);
        }
    }
}
