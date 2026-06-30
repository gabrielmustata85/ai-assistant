package com.ai.assistant.payroll;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

/** Toate cheltuielile extrase dintr-un singur PDF (un document poate conține mai multe). */
public record ParsedExpenses(
        @JsonPropertyDescription("Lista tuturor cheltuielilor găsite în document") List<ParsedExpense> expenses) {
}
