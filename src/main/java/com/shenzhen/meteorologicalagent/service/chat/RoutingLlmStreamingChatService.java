package com.shenzhen.meteorologicalagent.service.chat;

import com.shenzhen.meteorologicalagent.common.ErrorCode;
import com.shenzhen.meteorologicalagent.common.exception.BusinessException;
import com.shenzhen.meteorologicalagent.config.AiModelProperties;
import com.shenzhen.meteorologicalagent.domain.ai.AIResponse;
import com.shenzhen.meteorologicalagent.domain.ai.StructuredForecast;
import com.shenzhen.meteorologicalagent.parser.ResponseSectionParser;
import com.shenzhen.meteorologicalagent.parser.StructuredForecastParser;
import com.shenzhen.meteorologicalagent.util.IdUtils;
import java.util.Map;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class RoutingLlmStreamingChatService implements LlmStreamingChatService {

    private static final Logger log = LoggerFactory.getLogger(RoutingLlmStreamingChatService.class);

    private final ObjectProvider<ChatClient.Builder> chatClientBuilderProvider;
    private final Environment environment;
    private final AiModelProperties aiModelProperties;
    private final ForecastTextRenderer fallbackRenderer;
    private final ResponseSectionParser responseSectionParser;
    private final StructuredForecastParser structuredForecastParser;

    public RoutingLlmStreamingChatService(
            ObjectProvider<ChatClient.Builder> chatClientBuilderProvider,
            Environment environment,
            AiModelProperties aiModelProperties,
            ForecastTextRenderer fallbackRenderer,
            ResponseSectionParser responseSectionParser,
            StructuredForecastParser structuredForecastParser
    ) {
        this.chatClientBuilderProvider = chatClientBuilderProvider;
        this.environment = environment;
        this.aiModelProperties = aiModelProperties;
        this.fallbackRenderer = fallbackRenderer;
        this.responseSectionParser = responseSectionParser;
        this.structuredForecastParser = structuredForecastParser;
    }

    @Override
    public AIResponse stream(LlmChatRequest request, Consumer<String> tokenConsumer) {
        long startedAt = System.currentTimeMillis();
        boolean springAiEnabled = shouldUseSpringAi();
        String content = springAiEnabled
                ? streamSpringAi(request, tokenConsumer)
                : streamFallback(request, tokenConsumer);
        long latencyMs = System.currentTimeMillis() - startedAt;
        StructuredForecast structuredOutput = structuredForecastParser.parse(content, request.weatherContext());

        AIResponse response = new AIResponse(
                IdUtils.newId("r"),
                content,
                responseSectionParser.parse(content),
                modelName(springAiEnabled),
                request.prompt().promptName().value(),
                request.prompt().promptVersion(),
                estimateTokens(request.prompt().promptLength()),
                estimateTokens(content.length()),
                latencyMs,
                structuredOutput
        );
        log.info(
                "llm_stream traceId={} task={} model={} prompt={} promptLength={} inputTokens={} outputTokens={} latencyMs={}",
                request.workflowTraceId(),
                request.taskType(),
                response.modelName(),
                response.promptName(),
                request.prompt().promptLength(),
                response.inputTokens(),
                response.outputTokens(),
                response.latencyMs()
        );
        return response;
    }

    private boolean shouldUseSpringAi() {
        String chatModel = environment.getProperty("spring.ai.model.chat", "none");
        return "openai".equalsIgnoreCase(chatModel) && chatClientBuilderProvider.getIfAvailable() != null;
    }

    private String streamSpringAi(LlmChatRequest request, Consumer<String> tokenConsumer) {
        ChatClient.Builder builder = chatClientBuilderProvider.getIfAvailable();
        if (builder == null) {
            return streamFallback(request, tokenConsumer);
        }
        StringBuilder content = new StringBuilder();
        try {
            builder.build()
                    .prompt()
                    .system(request.prompt().systemPrompt())
                    .user(request.prompt().userPrompt())
                    .stream()
                    .content()
                    .doOnNext(token -> appendToken(content, tokenConsumer, token))
                    .blockLast();
            if (content.isEmpty()) {
                throw new BusinessException(ErrorCode.LLM_CALL_FAILED, "llm stream response content is empty");
            }
            return content.toString();
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            log.warn("spring_ai_stream_failed prompt={} reason={}", request.prompt().promptName().value(), exception.getMessage());
            throw new BusinessException(
                    ErrorCode.LLM_CALL_FAILED,
                    "llm stream failed",
                    Map.of("promptName", request.prompt().promptName().value(), "reason", safeReason(exception.getMessage()))
            );
        }
    }

    private String streamFallback(LlmChatRequest request, Consumer<String> tokenConsumer) {
        String content = fallbackRenderer.render(request);
        for (String token : content.split("(?<=。|；|！|？|\\n)")) {
            appendToken(new StringBuilder(), tokenConsumer, token);
        }
        return content;
    }

    private void appendToken(StringBuilder content, Consumer<String> tokenConsumer, String token) {
        if (token == null || token.isEmpty()) {
            return;
        }
        content.append(token);
        tokenConsumer.accept(token);
    }

    private String modelName(boolean springAiEnabled) {
        if (!springAiEnabled) {
            return "local-fallback";
        }
        return aiModelProperties.model() == null || aiModelProperties.model().isBlank()
                ? "local-fallback"
                : aiModelProperties.model();
    }

    private int estimateTokens(int length) {
        return Math.max(1, (int) Math.ceil(length / 2.0));
    }

    private String safeReason(String message) {
        if (message == null) {
            return "";
        }
        return message.length() > 200 ? message.substring(0, 200) : message;
    }
}
