package com.ai.assistant.bank;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

/** Toate tranzacțiile extrase dintr-un extras bancar (wrapper pt. output structurat Claude). */
public record ParsedStatement(
        @JsonPropertyDescription("Lista tuturor tranzacțiilor din extras") List<ParsedTransaction> transactions) {
}
