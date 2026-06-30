package com.ai.assistant.invoicing;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

/** Toate facturile extrase dintr-un singur PDF (un document poate conține mai multe facturi). */
public record ParsedInvoices(
        @JsonPropertyDescription("Lista tuturor facturilor găsite în document") List<ParsedInvoice> invoices) {
}
