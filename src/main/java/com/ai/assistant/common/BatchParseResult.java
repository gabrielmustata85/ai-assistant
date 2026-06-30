package com.ai.assistant.common;

/** Rezultatul extragerii unui fișier dintr-un lot: ori datele extrase (parsed), ori un mesaj de eroare. */
public record BatchParseResult<T>(String filename, T parsed, String error) {

    public static <T> BatchParseResult<T> ok(String filename, T parsed) {
        return new BatchParseResult<>(filename, parsed, null);
    }

    public static <T> BatchParseResult<T> failed(String filename, String error) {
        return new BatchParseResult<>(filename, null, error);
    }
}
