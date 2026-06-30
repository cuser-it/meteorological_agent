package com.shenzhen.meteorologicalagent.service.chat;

import com.shenzhen.meteorologicalagent.domain.ai.AIResponse;

public interface LlmChatService {

    AIResponse call(LlmChatRequest request);
}
