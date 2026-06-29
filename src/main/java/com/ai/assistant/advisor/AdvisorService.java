package com.ai.assistant.advisor;

import com.ai.assistant.ai.ClaudeClient;
import com.ai.assistant.ai.ClaudeResponse;
import com.ai.assistant.client.EnhancedPineconeClient;
import com.ai.assistant.config.ConversationContext;
import com.ai.assistant.service.AIResponseHistoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class AdvisorService {

    private final CompanyContextBuilder contextBuilder;
    private final AdvisorPromptBuilder promptBuilder;
    private final EnhancedPineconeClient pineconeClient;
    private final ConversationContext conversationContext;
    private final ClaudeClient claudeClient;
    private final AIResponseHistoryService historyService;

    public AdvisorService(CompanyContextBuilder contextBuilder,
                          AdvisorPromptBuilder promptBuilder,
                          EnhancedPineconeClient pineconeClient,
                          ConversationContext conversationContext,
                          ClaudeClient claudeClient,
                          AIResponseHistoryService historyService) {
        this.contextBuilder = contextBuilder;
        this.promptBuilder = promptBuilder;
        this.pineconeClient = pineconeClient;
        this.conversationContext = conversationContext;
        this.claudeClient = claudeClient;
        this.historyService = historyService;
    }

    public ClaudeResponse ask(String sessionId, Long companyId, String question) {
        conversationContext.addMessage(sessionId, "User: " + question);
        String conversation = conversationContext.getFullContext(sessionId);
        String companyContext = contextBuilder.build(companyId);

        List<String> legislation = new ArrayList<>();
        try {
            for (String id : pineconeClient.searchLegislation(question)) {
                String text = pineconeClient.fetchLegislation(id);
                if (text != null && !text.isBlank()) {
                    legislation.add(text);
                }
            }
        } catch (IOException e) {
            log.warn("Căutarea legislației a eșuat: {}", e.getMessage());
        }

        String prompt = promptBuilder.build(question, conversation, companyContext, legislation);
        ClaudeResponse response = claudeClient.ask(prompt);

        conversationContext.addMessage(sessionId, "AI: " + summarize(response));
        historyService.logInteraction(sessionId, question, summarize(response));
        return response;
    }

    public ClaudeResponse obligations(Long companyId) {
        return ask("obligations-" + companyId, companyId,
                "Ce taxe am de plătit și până când? Estimează sumele și termenele.");
    }

    public void reset(String sessionId) {
        conversationContext.clear(sessionId);
    }

    private String summarize(ClaudeResponse r) {
        int est = r.estimari() == null ? 0 : r.estimari().size();
        int rec = r.recomandari() == null ? 0 : r.recomandari().size();
        return est + " estimări, " + rec + " recomandări";
    }
}
