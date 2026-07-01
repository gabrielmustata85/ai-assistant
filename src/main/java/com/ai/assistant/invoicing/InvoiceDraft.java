package com.ai.assistant.invoicing;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

/** Schiță de factură generată de asistent dintr-o instrucțiune în limbaj natural. */
public record InvoiceDraft(
        @JsonPropertyDescription("ISSUED dacă firma EMITE factura către un client (implicit), RECEIVED dacă o primește")
        String direction,
        @JsonPropertyDescription("Numărul/seria facturii dacă userul l-a dat; altfel gol (îl completează userul)")
        String invoiceNumber,
        @JsonPropertyDescription("Numele clientului/partenerului") String partnerName,
        @JsonPropertyDescription("CUI-ul partenerului dacă e dat; altfel gol") String partnerCui,
        @JsonPropertyDescription("Data emiterii YYYY-MM-DD; folosește data de azi dacă nu se specifică") String issueDate,
        @JsonPropertyDescription("Data scadenței YYYY-MM-DD; gol dacă nu se specifică") String dueDate,
        @JsonPropertyDescription("Valoarea netă (fără TVA)") double netAmount,
        @JsonPropertyDescription("Valoarea TVA") double vatAmount,
        @JsonPropertyDescription("Valoarea totală (cu TVA)") double grossAmount,
        @JsonPropertyDescription("Descriere/categorie scurtă (ex: consultanță, marfă, servicii)") String category,
        @JsonPropertyDescription("Unitatea de măsură (ex: ore, buc, luni); gol dacă nu se specifică") String unit,
        @JsonPropertyDescription("Cantitatea; 0 dacă nu se specifică") double quantity,
        @JsonPropertyDescription("Prețul unitar fără TVA; 0 dacă nu se specifică") double unitPrice,
        @JsonPropertyDescription("true DOAR dacă ai destule date pentru a propune factura (cel puțin client și o sumă)")
        boolean ready,
        @JsonPropertyDescription("Dacă ready=false: întrebări scurte către user pentru datele lipsă. Altfel listă goală.")
        List<String> missing,
        @JsonPropertyDescription("Mesaj scurt de la asistent: ce a pregătit sau ce mai are nevoie")
        String message) {
}
