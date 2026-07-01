package com.ai.assistant.usage;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

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

    /** Schimbă planul userului (PRO / MAX / FREE) și mărește limita. */
    @PostMapping("/usage/upgrade")
    public ResponseEntity<UsageStatus> upgrade(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(service.upgrade(body.get("plan")));
    }
}
