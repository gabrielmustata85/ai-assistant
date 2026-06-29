package com.ai.assistant.ai;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

/** Răspunsul structurat al asistentului fiscal. Servește și ca schemă pentru output structurat Claude. */
public record ClaudeResponse(
        @JsonPropertyDescription("Estimări de taxe de plată") List<Estimare> estimari,
        @JsonPropertyDescription("Termene/scadențe viitoare") List<Termen> termene,
        @JsonPropertyDescription("Recomandări de optimizare") List<Recomandare> recomandari,
        @JsonPropertyDescription("Date lipsă pe care asistentul trebuie să le ceară userului") List<String> dataGaps,
        @JsonPropertyDescription("Disclaimer: sumele sunt orientative") String disclaimer) {

    public record Estimare(String tipTaxa, double suma, String perioada, String explicatie) {}

    public record Termen(String obligatie, String scadenta) {}

    public record Recomandare(String text, String impactEstimat) {}
}
