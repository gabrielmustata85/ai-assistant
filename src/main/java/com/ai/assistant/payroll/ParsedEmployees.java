package com.ai.assistant.payroll;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

/** Toți angajații extrași dintr-un singur PDF (un stat de plată conține de obicei mai mulți). */
public record ParsedEmployees(
        @JsonPropertyDescription("Lista tuturor angajaților găsiți în document") List<ParsedEmployee> employees) {
}
