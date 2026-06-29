package com.ai.assistant.advisor;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AdvisorPromptBuilder {

    public String build(String question,
                        String conversationContext,
                        String companyContext,
                        List<String> legislationSnippets) {
        StringBuilder p = new StringBuilder();
        p.append("Ești un asistent fiscal pentru firme din România. ");
        p.append("Oferi sugestii și estimări ORIENTATIVE — nu înlocuiești contabilul. ");
        p.append("Folosește regulile de legislație de mai jos și datele firmei pentru a estima ");
        p.append("când și cât are de plătit, și pentru a recomanda optimizări de cheltuieli/taxe.\n");
        p.append("Dacă lipsesc date necesare (ex: salarii, cheltuieli), enumeră-le în câmpul data_gaps.\n\n");

        p.append("=== LEGISLAȚIE RELEVANTĂ ===\n");
        if (legislationSnippets == null || legislationSnippets.isEmpty()) {
            p.append("(niciun fragment găsit)\n");
        } else {
            for (String s : legislationSnippets) {
                p.append("- ").append(s).append("\n");
            }
        }
        p.append("\n");

        p.append("=== DATELE FIRMEI ===\n").append(companyContext).append("\n");

        if (conversationContext != null && !conversationContext.isBlank()) {
            p.append("=== CONVERSAȚIA ANTERIOARĂ ===\n").append(conversationContext).append("\n\n");
        }

        p.append("=== ÎNTREBAREA ===\n").append(question).append("\n\n");
        p.append("Răspunde STRICT în structura cerută (estimari, termene, recomandari, data_gaps, disclaimer). ");
        p.append("Disclaimer-ul trebuie să precizeze că sumele sunt orientative.");
        return p.toString();
    }
}
