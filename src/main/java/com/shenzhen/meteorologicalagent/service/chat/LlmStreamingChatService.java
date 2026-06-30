package com.shenzhen.meteorologicalagent.service.chat;

import com.shenzhen.meteorologicalagent.domain.ai.AIResponse;
import java.util.function.Consumer;

public interface LlmStreamingChatService {

    AIResponse stream(LlmChatRequest request, Consumer<String> tokenConsumer);
}
