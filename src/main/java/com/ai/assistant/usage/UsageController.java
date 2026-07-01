package com.ai.assistant.usage;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UsageController {

    private final UsageService service;

    public UsageController(UsageService service) {
        this.service = service;
    }

    /** Consumul de tokens al userului curent, în luna curentă. */
    @GetMapping("/usage")
    public ResponseEntity<UsageStatus> usage() {
        return ResponseEntity.ok(service.current());
    }
}
