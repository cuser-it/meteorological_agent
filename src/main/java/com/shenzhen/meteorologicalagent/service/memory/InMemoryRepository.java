package com.shenzhen.meteorologicalagent.service.memory;

import com.shenzhen.meteorologicalagent.domain.conversation.Conversation;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryRepository implements MemoryRepository {

    private final ConcurrentMap<String, Conversation> conversations = new ConcurrentHashMap<>();

    @Override
    public Conversation save(Conversation conversation) {
        conversations.put(conversation.conversationId(), conversation);
        return conversation;
    }

    @Override
    public Optional<Conversation> findById(String conversationId) {
        return Optional.ofNullable(conversations.get(conversationId));
    }
}
