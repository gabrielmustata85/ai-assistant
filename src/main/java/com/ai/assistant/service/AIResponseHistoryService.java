package com.ai.assistant.service;

import com.ai.assistant.client.CopilotClient;
import com.ai.assistant.model.AIResponseHistory;
import com.ai.assistant.repository.AIResponseHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AIResponseHistoryService {

    private final AIResponseHistoryRepository historyRepository;
    private final CopilotClient copilotClient;

    public void logInteraction(String sessionId, String userQuery, String aiResponse) {
        AIResponseHistory history = new AIResponseHistory();
        history.setSessionId(sessionId);
        history.setUserQuery(userQuery);
        history.setAiResponse(aiResponse);
        history.setTimestamp(LocalDateTime.now());
        historyRepository.save(history);
    }

    public void logCorrection(Long responseId, String correctedResponse) {
        historyRepository.findById(responseId).ifPresent(history -> {
            history.setWasCorrected(true);
            history.setCorrectedResponse(correctedResponse);
            history.setCorrectionTimestamp(LocalDateTime.now());
            historyRepository.save(history);
        });
    }

    public String getConversationContext(String sessionId) {
        List<AIResponseHistory> history = historyRepository
                .findBySessionIdOrderByTimestampDesc(sessionId);

        if (history.isEmpty()) {
            return null;
        }

        return history.stream()
                .limit(3)
                .sorted(Comparator.comparing(AIResponseHistory::getTimestamp))
                .map(entry -> String.format("%s: %s\nAI: %s",
                        entry.getWasCorrected() ? "User (corrected)" : "User",
                        entry.getUserQuery(),
                        entry.getWasCorrected() ? entry.getCorrectedResponse() : entry.getAiResponse()))
                .collect(Collectors.joining("\n\n"));
    }

    @Scheduled(cron = "0 0 3 * * ?") // Daily at 3am
    public void cleanupOldHistory() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
        int deleted = historyRepository.deleteByTimestampBefore(cutoff);
        log.info("Cleaned up {} old history entries", deleted);
    }
}
