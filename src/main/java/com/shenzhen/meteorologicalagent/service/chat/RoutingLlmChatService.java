package com.shenzhen.meteorologicalagent.service.chat;

import com.shenzhen.meteorologicalagent.common.ErrorCode;
import com.shenzhen.meteorologicalagent.common.exception.BusinessException;
import com.shenzhen.meteorologicalagent.config.AiModelProperties;
import com.shenzhen.meteorologicalagent.domain.ai.AIResponse;
import com.shenzhen.meteorologicalagent.parser.ResponseSectionParser;
import com.shenzhen.meteorologicalagent.util.IdUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class RoutingLlmChatService implements LlmChatService {

    private static final Logger log = LoggerFactory.getLogger(RoutingLlmChatService.class);

    private final ObjectProvider<ChatClient.Builder> chatClientBuilderProvider;
    private final Environment environment;
    private final AiModelProperties aiModelProperties;
    private final ForecastTextRenderer fallbackRenderer;
    private final ResponseSectionParser responseSectionParser;

    public RoutingLlmChatService(
            ObjectProvider<ChatClient.Builder> chatClientBuilderProvider,
            Environment environment,
            AiModelProperties aiModelProperties,
            ForecastTextRenderer fallbackRenderer,
            ResponseSectionParser responseSectionParser
    ) {
        this.chatClientBuilderProvider = chatClientBuilderProvider;
        this.environment = environment;
        this.aiModelProperties = aiModelProperties;
        this.fallbackRenderer = fallbackRenderer;
        this.responseSectionParser = responseSectionParser;
    }

    @Override
    public AIResponse call(LlmChatRequest request) {
        long startedAt = System.currentTimeMillis();
        boolean springAiEnabled = shouldUseSpringAi();
        String content = springAiEnabled ? callSpringAi(request) : fallbackRenderer.render(request);
        long latencyMs = System.currentTimeMillis() - startedAt;

        AIResponse response = new AIResponse(
                IdUtils.newId("r"),
                content,
                responseSectionParser.parse(content),
                modelName(springAiEnabled),
                request.prompt().promptName().value(),
                request.prompt().promptVersion(),
                estimateTokens(request.prompt().promptLength()),
                estimateTokens(content.length()),
                latencyMs
        );
        log.info(
                "llm_call task={} model={} prompt={} promptLength={} inputTokens={} outputTokens={} latencyMs={}",
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

    private String callSpringAi(LlmChatRequest request) {
        ChatClient.Builder builder = chatClientBuilderProvider.getIfAvailable();
        if (builder == null) {
            return fallbackRenderer.render(request);
        }
        try {
            String content = builder.build()
                    .prompt()
                    .system(request.prompt().systemPrompt())
                    .user(request.prompt().userPrompt())
                    .call()
                    .content();
            if (content == null || content.isBlank()) {
                throw new BusinessException(ErrorCode.LLM_CALL_FAILED, "llm response content is empty");
            }
            return content;
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            log.warn("spring_ai_call_failed prompt={} reason={}", request.prompt().promptName().value(), exception.getMessage());
            throw new BusinessException(ErrorCode.LLM_CALL_FAILED, "llm call failed");
        }
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
}
