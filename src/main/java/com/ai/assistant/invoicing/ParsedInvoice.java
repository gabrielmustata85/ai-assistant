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
        @JsonPropertyDescription("true dacă pare o cheltuială deductibilă fiscal") boolean deductible,
        @JsonPropertyDescription("IBAN-ul partenerului dacă apare pe factură; altfel gol") String partnerIban,
        @JsonPropertyDescription("Telefonul partenerului dacă apare; altfel gol") String partnerPhone,
        @JsonPropertyDescription("Emailul partenerului dacă apare; altfel gol") String partnerEmail,
        @JsonPropertyDescription("Adresa/sediul partenerului dacă apare; altfel gol") String partnerAddress,
        @JsonPropertyDescription("Nr. de ordine în registrul comerțului al partenerului (ex: J23/5883/2021); altfel gol") String partnerRegCom,
        @JsonPropertyDescription("Unitatea de măsură a liniei (ex: ore, buc, luni); altfel gol") String unit,
        @JsonPropertyDescription("Cantitatea de pe linia facturii; 0 dacă lipsește") double quantity,
        @JsonPropertyDescription("Prețul unitar (fără TVA); 0 dacă lipsește") double unitPrice) {
}
