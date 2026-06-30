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
        p.append("Te numești Dario și ești asistentul fiscal pentru firme din România. ");
        p.append("Dacă userul te întreabă cine ești, spune că ești Dario, asistentul lui fiscal. ");
        p.append("Oferi sugestii și estimări orientative — nu înlocuiești contabilul.\n\n");

        p.append("REGULI DE RĂSPUNS (respectă-le strict):\n");
        p.append("1. Răspunde DIRECT la întrebarea userului, în câmpul `raspuns`, CONCIS: 2-4 propoziții. ");
        p.append("Fără introduceri, fără a repeta întrebarea, fără text de umplutură.\n");
        p.append("2. Completează `estimari`, `termene`, `recomandari` DOAR dacă întrebarea e fiscală ");
        p.append("ȘI ai datele necesare. Dacă nu e cazul, lasă-le liste GOALE.\n");
        p.append("3. Nu recita legislația sau datele de mai jos — folosește-le doar ca să răspunzi. ");
        p.append("Sunt material de referință, nu de afișat.\n");
        p.append("4. Dacă întrebarea NU e despre taxe (ex: despre o persoană dintr-un document), ");
        p.append("răspunde simplu în `raspuns` și lasă restul câmpurilor goale.\n");
        p.append("5. `disclaimer` doar dacă ai dat sume/estimări; altfel lasă-l gol.\n");
        p.append("6. Dacă lipsesc date pentru o estimare fiscală, cere-le scurt în `data_gaps`.\n\n");

        p.append("=== CONTEXT DE REFERINȚĂ (legislație/documente găsite) ===\n");
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
        p.append("Răspunde în structura JSON cerută. Fii concis peste tot.");
        return p.toString();
    }
}
