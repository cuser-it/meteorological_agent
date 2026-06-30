package com.shenzhen.meteorologicalagent.service.memory;

import com.shenzhen.meteorologicalagent.domain.conversation.Conversation;
import java.util.Optional;

public interface MemoryRepository {

    Conversation save(Conversation conversation);

    Optional<Conversation> findById(String conversationId);
}
