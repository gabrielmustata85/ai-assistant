package com.ai.assistant.invoicing;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/** Datele unei facturi extrase dintr-un PDF de către Claude (output structurat). */
public record ParsedInvoice(
        @JsonPropertyDescription("ISSUED dacă firma noastră a EMIS factura (vânzare către client), RECEIVED dacă a PRIMIT-o (achiziție de la furnizor)")
        String direction,
        @JsonPropertyDescription("Numărul/seria facturii") String invoiceNumber,
        @JsonPropertyDescription("Numele partenerului (furnizorul sau clientul de pe factură)") String partnerName,
        @JsonPropertyDescription("Codul fiscal (CUI) al partenerului") String partnerCui,
        @JsonPropertyDescription("Data emiterii în format YYYY-MM-DD") String issueDate,
        @JsonPropertyDescription("Data scadenței în format YYYY-MM-DD; gol dacă lipsește") String dueDate,
        @JsonPropertyDescription("Valoarea netă, fără TVA") double netAmount,
        @JsonPropertyDescription("Valoarea TVA") double vatAmount,
        @JsonPropertyDescription("Valoarea totală, cu TVA inclus") double grossAmount,
        @JsonPropertyDescription("Categoria cheltuielii (ex: carburant, chirie, marfă, utilități, servicii)") String category,
        @JsonPropertyDescription("true dacă pare o cheltuială deductibilă fiscal") boolean deductible) {
}
