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
        // Fix 3: read context BEFORE adding the current question to avoid duplication in prompt
        String conversation = conversationContext.getFullContext(sessionId);
        conversationContext.addMessage(sessionId, "User: " + question);
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

        // Fix 2: build full AI text for persistence; use short summary only for conversation memory
        String aiText = buildAuditText(response);
        String gaps = (response.dataGaps() == null || response.dataGaps().isEmpty())
                ? null : String.join(" | ", response.dataGaps());
        String summary = summarize(response);

        conversationContext.addMessage(sessionId, "AI: " + summary);
        historyService.logInteraction(sessionId, companyId, question, aiText, gaps);
        return response;
    }

    private String buildAuditText(ClaudeResponse r) {
        StringBuilder sb = new StringBuilder();
        if (r.raspuns() != null && !r.raspuns().isBlank()) {
            sb.append("RĂSPUNS: ").append(r.raspuns()).append("\n\n");
        }
        if (r.disclaimer() != null && !r.disclaimer().isBlank()) {
            sb.append("DISCLAIMER: ").append(r.disclaimer()).append("\n\n");
        }
        if (r.estimari() != null && !r.estimari().isEmpty()) {
            sb.append("ESTIMĂRI:\n");
            for (ClaudeResponse.Estimare e : r.estimari()) {
                sb.append("  - ").append(e.tipTaxa())
                  .append(": ").append(e.suma())
                  .append(" RON (").append(e.perioada()).append(")\n");
            }
            sb.append("\n");
        }
        if (r.termene() != null && !r.termene().isEmpty()) {
            sb.append("TERMENE:\n");
            for (ClaudeResponse.Termen t : r.termene()) {
                sb.append("  - ").append(t.obligatie())
                  .append(" — scadenta: ").append(t.scadenta()).append("\n");
            }
            sb.append("\n");
        }
        if (r.recomandari() != null && !r.recomandari().isEmpty()) {
            sb.append("RECOMANDĂRI:\n");
            for (ClaudeResponse.Recomandare rec : r.recomandari()) {
                sb.append("  - ").append(rec.text()).append("\n");
            }
        }
        return sb.toString().trim();
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
