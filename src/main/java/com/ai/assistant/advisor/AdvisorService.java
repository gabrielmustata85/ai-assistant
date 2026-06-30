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
        String today = java.time.LocalDate.now().toString();
        // Atenție: textul conține % literali (procente) — NU folosi String.formatted aici; concatenăm data.
        String question = "Azi este " + today + ". " + """
                Pe baza datelor firmei (facturi, salarii, cheltuieli, extrase bancare), \
                estimează TOATE taxele și contribuțiile pe care firma le are de plătit în perioada următoare.
                - Include OBLIGATORIU taxele pe salarii calculate din fondul de salarii brut lunar al \
                angajaților activi (CAS 25%, CASS 10%, impozit pe venit 10%, CAM 2.25% — aplicate conform \
                regulilor în vigoare), cu Declarația 112 și scadența pe 25 a lunii următoare. Dacă firma \
                nu are angajați activi, nu pune taxe pe salarii.
                - În `estimari` pune fiecare taxă cu suma estimată și perioada.
                - În `termene` pune scadențele, ORDONATE de la cea mai apropiată la cea mai îndepărtată, \
                începând cu cele din luna/lunile imediat următoare față de data de azi.
                - În `recomandari` dă sugestii CONCRETE de optimizare, cu sume aproximative în lei, de exemplu: \
                „dacă mai adaugi cheltuieli deductibile de ~X lei, impozitul scade cu ~Y lei” sau \
                „dacă mai facturezi ~Z lei până la finalul trimestrului, ...”. Pune efectul în lei în `impactEstimat`.
                - Dacă lipsesc date esențiale (nr. angajați, salarii etc.), cere-le scurt în `data_gaps`.
                - În `raspuns` scrie pe scurt care e următoarea taxă de plătit și până când.""";
        return ask("obligations-" + companyId, companyId, question);
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
