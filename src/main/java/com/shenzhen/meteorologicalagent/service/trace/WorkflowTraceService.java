package com.shenzhen.meteorologicalagent.service.trace;

import com.shenzhen.meteorologicalagent.domain.ai.WorkflowTrace;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public interface WorkflowTraceService {

    WorkflowTraceContext start(String workflowType, String sessionId);

    <T> T traceStep(WorkflowTraceContext context, String stepName, Supplier<T> supplier);

    void traceStep(WorkflowTraceContext context, String stepName, Runnable runnable);

    void addStep(WorkflowTraceContext context, String stepName, String status, Map<String, Object> attributes, long latencyMs);

    WorkflowTrace finish(WorkflowTraceContext context, String conversationId, String status);

    Optional<WorkflowTrace> find(String traceId);
}
