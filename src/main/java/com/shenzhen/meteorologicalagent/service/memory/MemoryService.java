package com.shenzhen.meteorologicalagent.service.memory;

import com.shenzhen.meteorologicalagent.common.ErrorCode;
import com.shenzhen.meteorologicalagent.common.exception.BusinessException;
import com.shenzhen.meteorologicalagent.domain.ai.AIResponse;
import com.shenzhen.meteorologicalagent.domain.ai.IntentType;
import com.shenzhen.meteorologicalagent.domain.ai.PromptSnapshot;
import com.shenzhen.meteorologicalagent.domain.conversation.Conversation;
import com.shenzhen.meteorologicalagent.domain.conversation.ConversationMessage;
import com.shenzhen.meteorologicalagent.domain.conversation.ConversationRole;
import com.shenzhen.meteorologicalagent.domain.conversation.ConversationStatus;
import com.shenzhen.meteorologicalagent.domain.weather.WeatherContext;
import com.shenzhen.meteorologicalagent.util.IdUtils;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class MemoryService {

    private final MemoryRepository repository;

    public MemoryService(MemoryRepository repository) {
        this.repository = repository;
    }

    public Conversation create(
            String sessionId,
            WeatherContext weatherContext,
            PromptSnapshot prompt,
            AIResponse response
    ) {
        OffsetDateTime now = OffsetDateTime.now();
        String conversationId = IdUtils.newId("c");
        List<ConversationMessage> messages = new ArrayList<>();
        messages.add(userMessage("生成一版短临天气预报", IntentType.GENERATE, 1, now));
        messages.add(assistantMessage(response, IntentType.GENERATE, 1, now));
        Conversation conversation = new Conversation(
                conversationId,
                sessionId,
                ConversationStatus.ACTIVE,
                weatherContext,
                response,
                prompt,
                1,
                messages,
                now,
                now
        );
        return repository.save(conversation);
    }

    public Conversation appendRewrite(
            Conversation conversation,
            String userInstruction,
            IntentType intent,
            PromptSnapshot prompt,
            AIResponse response
    ) {
        requireActive(conversation);
        int nextVersion = conversation.currentVersion() + 1;
        OffsetDateTime now = OffsetDateTime.now();
        List<ConversationMessage> messages = new ArrayList<>(conversation.messages());
        messages.add(userMessage(userInstruction, intent, nextVersion, now));
        messages.add(assistantMessage(response, intent, nextVersion, now));
        Conversation updated = new Conversation(
                conversation.conversationId(),
                conversation.sessionId(),
                ConversationStatus.ACTIVE,
                conversation.lastWeatherContext(),
                response,
                prompt,
                nextVersion,
                messages,
                conversation.createdAt(),
                now
        );
        return repository.save(updated);
    }

    public Conversation findActive(String conversationId, String sessionId) {
        Conversation conversation = find(conversationId, sessionId);
        requireActive(conversation);
        if (conversation.lastWeatherContext() == null || conversation.lastResponse() == null) {
            throw new BusinessException(ErrorCode.NO_PREVIOUS_RESPONSE, "当前会话还没有可改写的预报，请先生成一版预报。");
        }
        return conversation;
    }

    public Conversation find(String conversationId, String sessionId) {
        Conversation conversation = repository.findById(conversationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONVERSATION_NOT_FOUND, "conversation not found"));
        if (!conversation.sessionId().equals(sessionId)) {
            throw new BusinessException(ErrorCode.CONVERSATION_NOT_FOUND, "conversation not found");
        }
        return conversation;
    }

    public Conversation reset(String conversationId, String sessionId) {
        Conversation conversation = find(conversationId, sessionId);
        if (conversation.status() == ConversationStatus.RESET) {
            return conversation;
        }
        Conversation reset = new Conversation(
                conversation.conversationId(),
                conversation.sessionId(),
                ConversationStatus.RESET,
                conversation.lastWeatherContext(),
                conversation.lastResponse(),
                conversation.lastPrompt(),
                conversation.currentVersion(),
                conversation.messages(),
                conversation.createdAt(),
                OffsetDateTime.now()
        );
        return repository.save(reset);
    }

    private static void requireActive(Conversation conversation) {
        if (conversation.status() == ConversationStatus.RESET) {
            throw new BusinessException(ErrorCode.CONVERSATION_RESET, "会话已重置，不能继续改写。");
        }
    }

    private static ConversationMessage userMessage(
            String content,
            IntentType intent,
            int version,
            OffsetDateTime createdAt
    ) {
        return new ConversationMessage(
                IdUtils.newId("m"),
                ConversationRole.USER,
                content,
                intent,
                version,
                null,
                null,
                null,
                null,
                createdAt
        );
    }

    private static ConversationMessage assistantMessage(
            AIResponse response,
            IntentType intent,
            int version,
            OffsetDateTime createdAt
    ) {
        return new ConversationMessage(
                IdUtils.newId("m"),
                ConversationRole.ASSISTANT,
                response.content(),
                intent,
                version,
                response.promptName(),
                response.promptVersion(),
                response.modelName(),
                response.latencyMs(),
                createdAt
        );
    }
}
