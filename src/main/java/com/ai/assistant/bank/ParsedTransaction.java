package com.ai.assistant.bank;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/** O tranzacție extrasă dintr-un extras bancar. */
public record ParsedTransaction(
        @JsonPropertyDescription("Data tranzacției în format YYYY-MM-DD") String txnDate,
        @JsonPropertyDescription("Descrierea/detaliile tranzacției de pe extras") String description,
        @JsonPropertyDescription("Cealaltă parte (beneficiar/plătitor), dacă apare") String counterparty,
        @JsonPropertyDescription("IN dacă e încasare (bani intrați), OUT dacă e plată (bani ieșiți)") String direction,
        @JsonPropertyDescription("Suma tranzacției, număr pozitiv, fără simbol valutar") double amount) {
}
