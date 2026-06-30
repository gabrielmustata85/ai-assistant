package com.ai.assistant.payroll;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/** Datele unui angajat extrase dintr-un stat de plată / pontaj de către Claude (output structurat). */
public record ParsedEmployee(
        @JsonPropertyDescription("Numele complet al angajatului") String fullName,
        @JsonPropertyDescription("CNP-ul angajatului (13 cifre), dacă apare; altfel gol") String cnp,
        @JsonPropertyDescription("Salariul BRUT lunar (număr, fără simbol valutar). Dacă apare doar netul, estimează brutul sau lasă-l.") double grossSalary,
        @JsonPropertyDescription("Funcția/postul angajatului, dacă apare; altfel gol") String position,
        @JsonPropertyDescription("Data angajării în format YYYY-MM-DD, dacă apare; altfel gol") String startDate) {
}
