package com.shenzhen.meteorologicalagent.service.memory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.shenzhen.meteorologicalagent.common.ErrorCode;
import com.shenzhen.meteorologicalagent.common.exception.BusinessException;
import com.shenzhen.meteorologicalagent.domain.ai.AIResponse;
import com.shenzhen.meteorologicalagent.domain.ai.IntentType;
import com.shenzhen.meteorologicalagent.domain.ai.PromptSnapshot;
import com.shenzhen.meteorologicalagent.domain.conversation.Conversation;
import com.shenzhen.meteorologicalagent.domain.conversation.ConversationStatus;
import com.shenzhen.meteorologicalagent.domain.weather.RainForecast;
import com.shenzhen.meteorologicalagent.domain.weather.WeatherContext;
import com.shenzhen.meteorologicalagent.service.prompt.PromptName;
import java.time.OffsetDateTime;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MemoryServiceTest {

    private final MemoryService memoryService = new MemoryService(new InMemoryRepository());

    @Test
    void shouldCreateAppendAndResetConversation() {
        Conversation created = memoryService.create("s-001", weatherContext(), prompt(), response("r-001"));

        assertThat(created.currentVersion()).isEqualTo(1);
        assertThat(created.messages()).hasSize(2);

        Conversation updated = memoryService.appendRewrite(
                created,
                "简单一点",
                IntentType.SIMPLIFY,
                prompt(),
                response("r-002")
        );

        assertThat(updated.currentVersion()).isEqualTo(2);
        assertThat(updated.messages()).hasSize(4);
        assertThat(updated.lastResponse().responseId()).isEqualTo("r-002");

        Conversation reset = memoryService.reset(updated.conversationId(), "s-001");

        assertThat(reset.status()).isEqualTo(ConversationStatus.RESET);
        assertThatThrownBy(() -> memoryService.findActive(updated.conversationId(), "s-001"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CONVERSATION_RESET);
    }

    private WeatherContext weatherContext() {
        return new WeatherContext(
                "深圳",
                OffsetDateTime.parse("2026-06-30T10:00:00+08:00"),
                "未来3小时",
                null,
                new RainForecast("中到大雨", "10-30毫米", "10:30-12:00", "逐渐增强后减弱", 0.82),
                null,
                java.util.List.of("短时强降水"),
                "业务系统"
        );
    }

    private PromptSnapshot prompt() {
        return new PromptSnapshot(PromptName.GENERATE, "v1", "system", "user", "sha256:test", 10);
    }

    private AIResponse response(String responseId) {
        return new AIResponse(
                responseId,
                "预计未来3小时深圳有中到大雨。",
                Map.of("summary", "未来3小时深圳有中到大雨"),
                "local-fallback",
                "generate",
                "v1",
                10,
                10,
                1
        );
    }
}
