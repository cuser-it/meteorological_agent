package com.shenzhen.meteorologicalagent.service.conversation;

import com.shenzhen.meteorologicalagent.common.ErrorCode;
import com.shenzhen.meteorologicalagent.common.exception.BusinessException;
import com.shenzhen.meteorologicalagent.domain.ai.AIResponse;
import com.shenzhen.meteorologicalagent.domain.ai.IntentResult;
import com.shenzhen.meteorologicalagent.domain.ai.IntentType;
import com.shenzhen.meteorologicalagent.domain.ai.PromptSnapshot;
import com.shenzhen.meteorologicalagent.domain.conversation.Conversation;
import com.shenzhen.meteorologicalagent.domain.conversation.ConversationMessage;
import com.shenzhen.meteorologicalagent.domain.weather.RainForecast;
import com.shenzhen.meteorologicalagent.domain.weather.WeatherContext;
import com.shenzhen.meteorologicalagent.dto.request.ConversationResetRequest;
import com.shenzhen.meteorologicalagent.dto.request.WeatherChatRequest;
import com.shenzhen.meteorologicalagent.dto.request.WeatherGenerateRequest;
import com.shenzhen.meteorologicalagent.dto.response.ConversationHistoryResponse;
import com.shenzhen.meteorologicalagent.dto.response.ConversationMessageResponse;
import com.shenzhen.meteorologicalagent.dto.response.ConversationResetResponse;
import com.shenzhen.meteorologicalagent.dto.response.WeatherAiResponse;
import com.shenzhen.meteorologicalagent.service.chat.ChatTaskType;
import com.shenzhen.meteorologicalagent.service.chat.LlmChatRequest;
import com.shenzhen.meteorologicalagent.service.chat.LlmChatService;
import com.shenzhen.meteorologicalagent.service.intent.IntentService;
import com.shenzhen.meteorologicalagent.service.memory.MemoryService;
import com.shenzhen.meteorologicalagent.service.prompt.PromptBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ConversationService {

    private static final Logger log = LoggerFactory.getLogger(ConversationService.class);

    private final IntentService intentService;
    private final PromptBuilder promptBuilder;
    private final LlmChatService llmChatService;
    private final MemoryService memoryService;

    public ConversationService(
            IntentService intentService,
            PromptBuilder promptBuilder,
            LlmChatService llmChatService,
            MemoryService memoryService
    ) {
        this.intentService = intentService;
        this.promptBuilder = promptBuilder;
        this.llmChatService = llmChatService;
        this.memoryService = memoryService;
    }

    public WeatherAiResponse generate(WeatherGenerateRequest request) {
        validateWeatherContext(request.weatherContext());
        IntentResult intentResult = intentService.generateIntent(request.style(), request.outputFormat());
        PromptSnapshot prompt = promptBuilder.buildGeneratePrompt(request, intentResult);
        AIResponse aiResponse = llmChatService.call(new LlmChatRequest(
                ChatTaskType.GENERATE,
                prompt,
                request.weatherContext(),
                null,
                intentResult,
                "生成一版短临天气预报"
        ));
        Conversation conversation = memoryService.create(
                request.sessionId(),
                request.weatherContext(),
                prompt,
                aiResponse
        );
        log.info(
                "conversation_generate conversationId={} sessionId={} intent={} version={} latencyMs={} promptLength={}",
                conversation.conversationId(),
                conversation.sessionId(),
                intentResult.intent(),
                conversation.currentVersion(),
                aiResponse.latencyMs(),
                prompt.promptLength()
        );
        return new WeatherAiResponse(
                conversation.conversationId(),
                conversation.sessionId(),
                null,
                conversation.currentVersion(),
                intentResult,
                aiResponse,
                List.of()
        );
    }

    public WeatherAiResponse chat(WeatherChatRequest request) {
        Conversation conversation = memoryService.findActive(request.conversationId(), request.sessionId());
        IntentResult intentResult = intentService.recognize(request.message());
        if (intentResult.intent() == IntentType.UNKNOWN) {
            throw new BusinessException(ErrorCode.UNKNOWN_INTENT, "无法识别可执行意图，请输入明确的修改要求。");
        }

        PromptSnapshot prompt = promptBuilder.buildRewritePrompt(conversation, request.message(), intentResult);
        AIResponse aiResponse = llmChatService.call(new LlmChatRequest(
                ChatTaskType.REWRITE,
                prompt,
                conversation.lastWeatherContext(),
                conversation.lastResponse(),
                intentResult,
                request.message()
        ));
        Conversation updated = memoryService.appendRewrite(
                conversation,
                request.message(),
                intentResult.intent(),
                prompt,
                aiResponse
        );
        List<String> changes = changes(intentResult);
        log.info(
                "conversation_rewrite conversationId={} sessionId={} intent={} previousVersion={} version={} changes={} latencyMs={}",
                updated.conversationId(),
                updated.sessionId(),
                intentResult.intent(),
                conversation.currentVersion(),
                updated.currentVersion(),
                changes,
                aiResponse.latencyMs()
        );
        return new WeatherAiResponse(
                updated.conversationId(),
                updated.sessionId(),
                conversation.currentVersion(),
                updated.currentVersion(),
                intentResult,
                aiResponse,
                changes
        );
    }

    public ConversationHistoryResponse history(String conversationId, String sessionId, boolean includePrompt) {
        Conversation conversation = memoryService.find(conversationId, sessionId);
        List<ConversationMessageResponse> messages = conversation.messages().stream()
                .map(message -> toMessageResponse(message, includePrompt))
                .toList();
        return new ConversationHistoryResponse(
                conversation.conversationId(),
                conversation.sessionId(),
                conversation.status(),
                conversation.currentVersion(),
                messages
        );
    }

    public ConversationResetResponse reset(ConversationResetRequest request) {
        Conversation conversation = memoryService.reset(request.conversationId(), request.sessionId());
        log.info(
                "conversation_reset conversationId={} sessionId={} status={}",
                conversation.conversationId(),
                conversation.sessionId(),
                conversation.status()
        );
        return new ConversationResetResponse(
                conversation.conversationId(),
                conversation.sessionId(),
                conversation.status()
        );
    }

    private ConversationMessageResponse toMessageResponse(ConversationMessage message, boolean includePrompt) {
        String promptName = includePrompt ? message.promptName() : null;
        String promptVersion = includePrompt ? message.promptVersion() : null;
        return new ConversationMessageResponse(
                message.messageId(),
                message.role(),
                message.content(),
                message.intent(),
                message.version(),
                promptName,
                promptVersion,
                message.modelName(),
                message.latencyMs(),
                message.createdAt()
        );
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
}
