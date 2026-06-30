package com.ai.assistant.config;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Mapează excepțiile comune la coduri HTTP clare, ca să nu mai apară 500-uri
 * mascate ca 401 (din cauza re-dispatch-ului către /error).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Încălcare de constrângere unică (ex: CUI sau username deja existent) -> 409. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleIntegrity(DataIntegrityViolationException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "error", "Conflict",
                "message", "O valoare unică există deja (ex: CUI sau username). Folosește alta."));
    }
}
