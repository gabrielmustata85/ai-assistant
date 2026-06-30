package com.ai.assistant.ai;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

/** Răspunsul structurat al asistentului fiscal. Servește și ca schemă pentru output structurat Claude. */
public record ClaudeResponse(
        @JsonPropertyDescription("Răspuns direct și CONCIS la întrebarea userului (2-4 propoziții). Aici răspunzi la ce s-a întrebat.") String raspuns,
        @JsonPropertyDescription("Estimări de taxe de plată. Completează DOAR dacă întrebarea e fiscală și ai datele; altfel listă goală.") List<Estimare> estimari,
        @JsonPropertyDescription("Termene/scadențe viitoare. Doar dacă e relevant; altfel listă goală.") List<Termen> termene,
        @JsonPropertyDescription("Recomandări de optimizare. Doar dacă e relevant; altfel listă goală.") List<Recomandare> recomandari,
        @JsonPropertyDescription("Date lipsă pe care asistentul trebuie să le ceară userului. Altfel listă goală.") List<String> dataGaps,
        @JsonPropertyDescription("Disclaimer scurt: sumele sunt orientative. Doar dacă ai dat estimări/sume.") String disclaimer) {

    public record Estimare(String tipTaxa, double suma, String perioada, String explicatie) {}

    public record Termen(String obligatie, String scadenta) {}

    public record Recomandare(String text, String impactEstimat) {}
}
