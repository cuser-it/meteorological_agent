package com.shenzhen.meteorologicalagent.service.conversation;

import com.shenzhen.meteorologicalagent.common.ErrorCode;
import com.shenzhen.meteorologicalagent.common.exception.BusinessException;
import com.shenzhen.meteorologicalagent.domain.ai.AIResponse;
import com.shenzhen.meteorologicalagent.domain.ai.EvaluationResult;
import com.shenzhen.meteorologicalagent.domain.ai.IntentResult;
import com.shenzhen.meteorologicalagent.domain.ai.IntentType;
import com.shenzhen.meteorologicalagent.domain.ai.PromptSnapshot;
import com.shenzhen.meteorologicalagent.domain.ai.WorkflowTrace;
import com.shenzhen.meteorologicalagent.domain.conversation.Conversation;
import com.shenzhen.meteorologicalagent.domain.weather.RainForecast;
import com.shenzhen.meteorologicalagent.domain.weather.WeatherContext;
import com.shenzhen.meteorologicalagent.dto.request.WeatherChatRequest;
import com.shenzhen.meteorologicalagent.dto.request.WeatherGenerateRequest;
import com.shenzhen.meteorologicalagent.dto.response.WeatherAiResponse;
import com.shenzhen.meteorologicalagent.service.chat.ChatTaskType;
import com.shenzhen.meteorologicalagent.service.chat.LlmChatRequest;
import com.shenzhen.meteorologicalagent.service.chat.LlmStreamingChatService;
import com.shenzhen.meteorologicalagent.service.evaluation.EvaluationService;
import com.shenzhen.meteorologicalagent.service.intent.IntentService;
import com.shenzhen.meteorologicalagent.service.memory.MemoryService;
import com.shenzhen.meteorologicalagent.service.prompt.PromptBuilder;
import com.shenzhen.meteorologicalagent.service.trace.WorkflowTraceContext;
import com.shenzhen.meteorologicalagent.service.trace.WorkflowTraceService;
import java.io.IOException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class ConversationStreamingService {

    private static final Logger log = LoggerFactory.getLogger(ConversationStreamingService.class);
    private static final long STREAM_TIMEOUT_MS = Duration.ofMinutes(3).toMillis();

    private final IntentService intentService;
    private final PromptBuilder promptBuilder;
    private final LlmStreamingChatService llmStreamingChatService;
    private final MemoryService memoryService;
    private final EvaluationService evaluationService;
    private final WorkflowTraceService workflowTraceService;

    public ConversationStreamingService(
            IntentService intentService,
            PromptBuilder promptBuilder,
            LlmStreamingChatService llmStreamingChatService,
            MemoryService memoryService,
            EvaluationService evaluationService,
            WorkflowTraceService workflowTraceService
    ) {
        this.intentService = intentService;
        this.promptBuilder = promptBuilder;
        this.llmStreamingChatService = llmStreamingChatService;
        this.memoryService = memoryService;
        this.evaluationService = evaluationService;
        this.workflowTraceService = workflowTraceService;
    }

    public SseEmitter generate(WeatherGenerateRequest request) {
        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MS);
        Thread.ofVirtual().name("weather-generate-stream-", 0).start(() -> generateInternal(request, emitter));
        return emitter;
    }

    public SseEmitter chat(WeatherChatRequest request) {
        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MS);
        Thread.ofVirtual().name("weather-rewrite-stream-", 0).start(() -> chatInternal(request, emitter));
        return emitter;
    }

    private void generateInternal(WeatherGenerateRequest request, SseEmitter emitter) {
        WorkflowTraceContext traceContext = workflowTraceService.start("WEATHER_GENERATE_STREAM", request.sessionId());
        send(emitter, "workflow", Map.of("traceId", traceContext.traceId(), "workflowType", traceContext.workflowType()));
        try {
            traced(traceContext, emitter, "validate-weather-context", () -> validateWeatherContext(request.weatherContext()));
            IntentResult intentResult = traced(
                    traceContext,
                    emitter,
                    "intent-detection",
                    () -> intentService.generateIntent(request.style(), request.outputFormat())
            );
            send(emitter, "intent", intentResult);
            PromptSnapshot prompt = traced(
                    traceContext,
                    emitter,
                    "prompt-render",
                    () -> promptBuilder.buildGeneratePrompt(request, intentResult)
            );
            send(emitter, "prompt", promptMetadata(prompt));
            AIResponse aiResponse = streamLlm(
                    traceContext,
                    emitter,
                    new LlmChatRequest(
                            ChatTaskType.GENERATE,
                            prompt,
                            request.weatherContext(),
                            null,
                            intentResult,
                            "生成一版短临天气预报",
                            traceContext.traceId()
                    )
            );
            EvaluationResult evaluation = traced(
                    traceContext,
                    emitter,
                    "evaluation",
                    () -> evaluationService.evaluate(aiResponse.content(), request.weatherContext())
            );
            Conversation conversation = traced(
                    traceContext,
                    emitter,
                    "memory-save",
                    () -> memoryService.create(request.sessionId(), request.weatherContext(), prompt, aiResponse)
            );
            traceContext.putMetadata("promptHash", prompt.contentHash());
            traceContext.putMetadata("evaluationScore", evaluation.score());
            WorkflowTrace trace = workflowTraceService.finish(traceContext, conversation.conversationId(), "SUCCESS");
            WeatherAiResponse response = new WeatherAiResponse(
                    conversation.conversationId(),
                    conversation.sessionId(),
                    null,
                    conversation.currentVersion(),
                    intentResult,
                    aiResponse,
                    List.of(),
                    evaluation,
                    trace
            );
            send(emitter, "complete", response);
            log.info(
                    "conversation_generate_stream traceId={} conversationId={} sessionId={} intent={} version={} latencyMs={} evaluationScore={}",
                    trace.traceId(),
                    conversation.conversationId(),
                    conversation.sessionId(),
                    intentResult.intent(),
                    conversation.currentVersion(),
                    aiResponse.latencyMs(),
                    evaluation.score()
            );
            emitter.complete();
        } catch (RuntimeException exception) {
            finishFailedTrace(traceContext, null, exception);
            sendError(emitter, exception);
        }
    }

    private void chatInternal(WeatherChatRequest request, SseEmitter emitter) {
        WorkflowTraceContext traceContext = workflowTraceService.start("WEATHER_REWRITE_STREAM", request.sessionId());
        send(emitter, "workflow", Map.of("traceId", traceContext.traceId(), "workflowType", traceContext.workflowType()));
        try {
            Conversation conversation = traced(
                    traceContext,
                    emitter,
                    "memory-load",
                    () -> memoryService.findActive(request.conversationId(), request.sessionId())
            );
            IntentResult intentResult = traced(
                    traceContext,
                    emitter,
                    "intent-detection",
                    () -> intentService.recognize(request.message())
            );
            if (intentResult.intent() == IntentType.UNKNOWN) {
                throw new BusinessException(ErrorCode.UNKNOWN_INTENT, "无法识别可执行意图，请输入明确的修改要求。");
            }
            send(emitter, "intent", intentResult);
            PromptSnapshot prompt = traced(
                    traceContext,
                    emitter,
                    "prompt-render",
                    () -> promptBuilder.buildRewritePrompt(conversation, request.message(), intentResult)
            );
            send(emitter, "prompt", promptMetadata(prompt));
            AIResponse aiResponse = streamLlm(
                    traceContext,
                    emitter,
                    new LlmChatRequest(
                            ChatTaskType.REWRITE,
                            prompt,
                            conversation.lastWeatherContext(),
                            conversation.lastResponse(),
                            intentResult,
                            request.message(),
                            traceContext.traceId()
                    )
            );
            EvaluationResult evaluation = traced(
                    traceContext,
                    emitter,
                    "evaluation",
                    () -> evaluationService.evaluate(aiResponse.content(), conversation.lastWeatherContext())
            );
            Conversation updated = traced(
                    traceContext,
                    emitter,
                    "memory-save",
                    () -> memoryService.appendRewrite(conversation, request.message(), intentResult.intent(), prompt, aiResponse)
            );
            List<String> changes = changes(intentResult);
            traceContext.putMetadata("promptHash", prompt.contentHash());
            traceContext.putMetadata("evaluationScore", evaluation.score());
            traceContext.putMetadata("changes", changes);
            WorkflowTrace trace = workflowTraceService.finish(traceContext, updated.conversationId(), "SUCCESS");
            WeatherAiResponse response = new WeatherAiResponse(
                    updated.conversationId(),
                    updated.sessionId(),
                    conversation.currentVersion(),
                    updated.currentVersion(),
                    intentResult,
                    aiResponse,
                    changes,
                    evaluation,
                    trace
            );
            send(emitter, "complete", response);
            log.info(
                    "conversation_rewrite_stream traceId={} conversationId={} sessionId={} intent={} previousVersion={} version={} latencyMs={} evaluationScore={}",
                    trace.traceId(),
                    updated.conversationId(),
                    updated.sessionId(),
                    intentResult.intent(),
                    conversation.currentVersion(),
                    updated.currentVersion(),
                    aiResponse.latencyMs(),
                    evaluation.score()
            );
            emitter.complete();
        } catch (RuntimeException exception) {
            finishFailedTrace(traceContext, request.conversationId(), exception);
            sendError(emitter, exception);
        }
    }

    private AIResponse streamLlm(WorkflowTraceContext context, SseEmitter emitter, LlmChatRequest request) {
        OffsetDateTime startedAt = OffsetDateTime.now();
        send(emitter, "step", stepEvent("llm-call", "RUNNING", 0, Map.of("streaming", true)));
        try {
            final int[] chunks = {0};
            AIResponse response = llmStreamingChatService.stream(request, token -> {
                chunks[0]++;
                send(emitter, "delta", Map.of("content", token));
            });
            long latencyMs = Duration.between(startedAt, OffsetDateTime.now()).toMillis();
            workflowTraceService.addStep(context, "llm-call", "SUCCESS", Map.of("streaming", true, "chunks", chunks[0]), latencyMs);
            send(emitter, "step", stepEvent("llm-call", "SUCCESS", latencyMs, Map.of("streaming", true, "chunks", chunks[0])));
            return response;
        } catch (RuntimeException exception) {
            long latencyMs = Duration.between(startedAt, OffsetDateTime.now()).toMillis();
            workflowTraceService.addStep(
                    context,
                    "llm-call",
                    "FAILED",
                    Map.of("error", exception.getClass().getSimpleName(), "message", safeMessage(exception.getMessage())),
                    latencyMs
            );
            send(emitter, "step", stepEvent("llm-call", "FAILED", latencyMs, Map.of("error", exception.getClass().getSimpleName())));
            throw exception;
        }
    }

    private <T> T traced(WorkflowTraceContext context, SseEmitter emitter, String stepName, Supplier<T> supplier) {
        OffsetDateTime startedAt = OffsetDateTime.now();
        send(emitter, "step", stepEvent(stepName, "RUNNING", 0, Map.of()));
        try {
            T value = workflowTraceService.traceStep(context, stepName, supplier);
            long latencyMs = Duration.between(startedAt, OffsetDateTime.now()).toMillis();
            send(emitter, "step", stepEvent(stepName, "SUCCESS", latencyMs, Map.of()));
            return value;
        } catch (RuntimeException exception) {
            long latencyMs = Duration.between(startedAt, OffsetDateTime.now()).toMillis();
            send(emitter, "step", stepEvent(stepName, "FAILED", latencyMs, Map.of("error", exception.getClass().getSimpleName())));
            throw exception;
        }
    }

    private void traced(WorkflowTraceContext context, SseEmitter emitter, String stepName, Runnable runnable) {
        traced(context, emitter, stepName, () -> {
            runnable.run();
            return null;
        });
    }

    private Map<String, Object> stepEvent(String stepName, String status, long latencyMs, Map<String, Object> attributes) {
        return Map.of(
                "stepName", stepName,
                "status", status,
                "latencyMs", latencyMs,
                "attributes", attributes
        );
    }

    private Map<String, Object> promptMetadata(PromptSnapshot prompt) {
        return Map.of(
                "promptName", prompt.promptName().value(),
                "promptVersion", prompt.promptVersion(),
                "contentHash", prompt.contentHash(),
                "promptLength", prompt.promptLength(),
                "moduleNames", prompt.moduleNames()
        );
    }

    private void send(SseEmitter emitter, String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(data));
        } catch (IOException | IllegalStateException exception) {
            throw new BusinessException(ErrorCode.LLM_CALL_FAILED, "stream client disconnected");
        }
    }

    private void sendError(SseEmitter emitter, RuntimeException exception) {
        ErrorCode code = exception instanceof BusinessException businessException
                ? businessException.getErrorCode()
                : ErrorCode.INTERNAL_ERROR;
        try {
            emitter.send(SseEmitter.event().name("error").data(Map.of(
                    "code", code.name(),
                    "message", safeMessage(exception.getMessage())
            )));
            emitter.complete();
        } catch (IOException | IllegalStateException ignored) {
            emitter.completeWithError(exception);
        }
    }

    private void validateWeatherContext(WeatherContext context) {
        if (context == null) {
            throw new BusinessException(ErrorCode.INVALID_WEATHER_CONTEXT, "weatherContext must not be null");
        }
        RainForecast rainForecast = context.rainForecast();
        if (rainForecast == null || rainForecast.level() == null || rainForecast.level().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_WEATHER_CONTEXT, "rainForecast.level must not be blank");
        }
    }

    private List<String> changes(IntentResult intentResult) {
        List<String> values = new ArrayList<>();
        switch (intentResult.intent()) {
            case SIMPLIFY -> values.add("简化表达");
            case MORE_DETAIL -> values.add("增加细节");
            case MORE_FORMAL -> values.add("提升正式程度");
            case MORE_CASUAL -> values.add("调整为通俗表达");
            case INCREASE_RAIN -> values.add("增强雨量表达");
            case DECREASE_RAIN -> values.add("减弱雨量表达");
            case ADD_WARNING -> values.add("增加风险提示");
            case REGENERATE -> values.add("重新生成");
            case GENERATE, UNKNOWN -> {
            }
        }
        Map<String, Object> parameters = intentResult.parameters();
        addSecondaryChange(values, parameters, "simplify", "简化表达");
        addSecondaryChange(values, parameters, "moreDetail", "增加细节");
        addSecondaryChange(values, parameters, "moreFormal", "提升正式程度");
        addSecondaryChange(values, parameters, "moreCasual", "调整为通俗表达");
        addSecondaryChange(values, parameters, "increaseRain", "增强雨量表达");
        addSecondaryChange(values, parameters, "decreaseRain", "减弱雨量表达");
        addSecondaryChange(values, parameters, "addWarning", "增加风险提示");
        addSecondaryChange(values, parameters, "regenerate", "重新生成");
        return values.stream().distinct().toList();
    }

    private void addSecondaryChange(List<String> values, Map<String, Object> parameters, String key, String change) {
        if (Boolean.TRUE.equals(parameters.get(key))) {
            values.add(change);
        }
    }

    private void finishFailedTrace(WorkflowTraceContext traceContext, String conversationId, RuntimeException exception) {
        traceContext.putMetadata("errorType", exception.getClass().getSimpleName());
        traceContext.putMetadata("errorMessage", safeMessage(exception.getMessage()));
        workflowTraceService.finish(traceContext, conversationId, "FAILED");
    }

    private String safeMessage(String message) {
        if (message == null) {
            return "";
        }
        return message.length() > 160 ? message.substring(0, 160) : message;
    }
}
