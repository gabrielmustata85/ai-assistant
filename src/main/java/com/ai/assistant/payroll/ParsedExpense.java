package com.ai.assistant.payroll;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/** Datele unei cheltuieli extrase dintr-un PDF/bon/factură de către Claude (output structurat). */
public record ParsedExpense(
        @JsonPropertyDescription("Descriere scurtă a cheltuielii (ex: 'Carburant Petrom', 'Chirie birou')")
        String description,
        @JsonPropertyDescription("Categoria (ex: carburant, chirie, marfă, utilități, servicii, birotică)")
        String category,
        @JsonPropertyDescription("Suma totală a cheltuielii (număr, fără simbol valutar)") double amount,
        @JsonPropertyDescription("Data cheltuielii în format YYYY-MM-DD") String expenseDate,
        @JsonPropertyDescription("true dacă pare o cheltuială deductibilă fiscal") boolean deductible) {
}
