package com.shenzhen.meteorologicalagent.domain.conversation;

import com.shenzhen.meteorologicalagent.domain.ai.IntentType;
import java.time.OffsetDateTime;

public record ConversationMessage(
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
