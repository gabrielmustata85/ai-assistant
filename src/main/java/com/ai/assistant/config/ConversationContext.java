package com.ai.assistant.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class ConversationContext {

    private final Map<String, LinkedList<String>> conversationHistory = new ConcurrentHashMap<>();
    private final int MAX_HISTORY_LENGTH = 10;

    public void addMessage(String sessionId, String message) {
        conversationHistory
                .computeIfAbsent(sessionId, k -> new LinkedList<>())
                .addLast(message);

        // Trim history to max length
        LinkedList<String> history = conversationHistory.get(sessionId);
        while (history.size() > MAX_HISTORY_LENGTH) {
            history.removeFirst();
        }
    }

    public String getFullContext(String sessionId) {
        return String.join("\n", conversationHistory.getOrDefault(sessionId, new LinkedList<>()));
    }

    public List<String> getRecentMessages(String sessionId, int count) {
        LinkedList<String> history = conversationHistory.getOrDefault(sessionId, new LinkedList<>());
        int fromIndex = Math.max(0, history.size() - count);
        return new ArrayList<>(history.subList(fromIndex, history.size()));
    }

    public void clear(String sessionId) {
        conversationHistory.remove(sessionId);
    }

    @Scheduled(fixedRate = 3600000) // Cleanup every hour
    public void cleanupOldSessions() {
        // Optional: Add logic to remove very old sessions
        log.info("Current active conversation sessions: {}", conversationHistory.size());
    }
}