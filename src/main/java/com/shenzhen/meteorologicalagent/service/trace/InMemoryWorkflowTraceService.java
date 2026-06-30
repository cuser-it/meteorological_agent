package com.shenzhen.meteorologicalagent.service.trace;

import com.shenzhen.meteorologicalagent.domain.ai.WorkflowTrace;
import com.shenzhen.meteorologicalagent.domain.ai.WorkflowTraceStep;
import com.shenzhen.meteorologicalagent.util.IdUtils;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class InMemoryWorkflowTraceService implements WorkflowTraceService {

    private static final Logger log = LoggerFactory.getLogger(InMemoryWorkflowTraceService.class);

    private final ConcurrentMap<String, WorkflowTrace> traces = new ConcurrentHashMap<>();

    @Override
    public WorkflowTraceContext start(String workflowType, String sessionId) {
        return new WorkflowTraceContext(IdUtils.newId("wt"), workflowType, sessionId);
    }

    @Override
    public <T> T traceStep(WorkflowTraceContext context, String stepName, Supplier<T> supplier) {
        OffsetDateTime startedAt = OffsetDateTime.now();
        try {
            T value = supplier.get();
            addStep(context, stepName, "SUCCESS", Map.of(), Duration.between(startedAt, OffsetDateTime.now()).toMillis());
            return value;
        } catch (RuntimeException exception) {
            addStep(
                    context,
                    stepName,
                    "FAILED",
                    Map.of("error", exception.getClass().getSimpleName(), "message", safeMessage(exception.getMessage())),
                    Duration.between(startedAt, OffsetDateTime.now()).toMillis()
            );
            throw exception;
        }
    }

    @Override
    public void traceStep(WorkflowTraceContext context, String stepName, Runnable runnable) {
        traceStep(context, stepName, () -> {
            runnable.run();
            return null;
        });
    }

    @Override
    public void addStep(WorkflowTraceContext context, String stepName, String status, Map<String, Object> attributes, long latencyMs) {
        OffsetDateTime finishedAt = OffsetDateTime.now();
        OffsetDateTime startedAt = finishedAt.minusNanos(Math.max(0, latencyMs) * 1_000_000);
        context.addStep(new WorkflowTraceStep(stepName, status, startedAt, finishedAt, latencyMs, attributes));
    }

    @Override
    public WorkflowTrace finish(WorkflowTraceContext context, String conversationId, String status) {
        OffsetDateTime finishedAt = OffsetDateTime.now();
        WorkflowTrace trace = new WorkflowTrace(
                context.traceId(),
                conversationId,
                context.sessionId(),
                context.workflowType(),
                status,
                context.startedAt(),
                finishedAt,
                Duration.between(context.startedAt(), finishedAt).toMillis(),
                context.steps(),
                context.metadata()
        );
        traces.put(trace.traceId(), trace);
        log.info(
                "workflow_trace traceId={} conversationId={} workflow={} status={} steps={} latencyMs={}",
                trace.traceId(),
                trace.conversationId(),
                trace.workflowType(),
                trace.status(),
                trace.steps().size(),
                trace.latencyMs()
        );
        return trace;
    }

    @Override
    public Optional<WorkflowTrace> find(String traceId) {
        return Optional.ofNullable(traces.get(traceId));
    }

    private String safeMessage(String message) {
        if (message == null) {
            return "";
        }
        return message.length() > 160 ? message.substring(0, 160) : message;
    }
}
