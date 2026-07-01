package com.ai.assistant.common;

/**
 * Rezultatul extragerii unui fișier dintr-un lot: ori datele extrase (parsed), ori un mesaj de eroare.
 * documentId (opțional) leagă rezultatul de fișierul original stocat (folosit la facturi).
 */
public record BatchParseResult<T>(String filename, T parsed, String error, Long documentId) {

    public static <T> BatchParseResult<T> ok(String filename, T parsed) {
        return new BatchParseResult<>(filename, parsed, null, null);
    }

    public static <T> BatchParseResult<T> ok(String filename, T parsed, Long documentId) {
        return new BatchParseResult<>(filename, parsed, null, documentId);
    }

    public static <T> BatchParseResult<T> failed(String filename, String error) {
        return new BatchParseResult<>(filename, null, error, null);
    }
}
