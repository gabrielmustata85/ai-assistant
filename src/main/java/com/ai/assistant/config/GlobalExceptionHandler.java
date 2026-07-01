package com.ai.assistant.config;

import com.ai.assistant.usage.QuotaExceededException;
import com.anthropic.errors.AnthropicServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Mapează excepțiile comune la coduri HTTP clare, ca să nu mai apară 500-uri
 * generice / mascate ca 401 (din cauza re-dispatch-ului către /error).
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Încălcare de constrângere unică (ex: CUI sau username deja existent) -> 409. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleIntegrity(DataIntegrityViolationException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "error", "Conflict",
                "message", "O valoare unică există deja (ex: CUI sau username). Folosește alta."));
    }

    /** Limita lunară de tokens atinsă -> 402 Payment Required (frontend cere upgrade). */
    @ExceptionHandler(QuotaExceededException.class)
    public ResponseEntity<Map<String, Object>> handleQuota(QuotaExceededException e) {
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(Map.of(
                "error", "QuotaExceeded",
                "message", e.getMessage(),
                "used", e.getUsed(),
                "limit", e.getLimit()));
    }

    /** Erori de la API-ul Claude (Anthropic) — le scoatem în răspuns, nu un 500 generic. */
    @ExceptionHandler(AnthropicServiceException.class)
    public ResponseEntity<Map<String, String>> handleAnthropic(AnthropicServiceException e) {
        log.error("Claude API error: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of(
                "error", "ClaudeApiError",
                "message", String.valueOf(e.getMessage())));
    }
}
