package com.shenzhen.meteorologicalagent.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.shenzhen.meteorologicalagent.domain.ai.IntentType;
import com.shenzhen.meteorologicalagent.domain.conversation.ConversationRole;
import java.time.OffsetDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ConversationMessageResponse(
        String messageId,
        ConversationRole role,
        String content,
        IntentType intent,
        int version,
        String promptName,
        String promptVersion,
        String modelName,
        Long latencyMs,
        OffsetDateTime createdAt
) {
}
