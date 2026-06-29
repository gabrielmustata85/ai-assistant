package com.ai.assistant.advisor;

import com.ai.assistant.ai.ClaudeResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/advisor")
public class AdvisorController {

    private final AdvisorService service;

    public AdvisorController(AdvisorService service) {
        this.service = service;
    }

    @PostMapping("/ask")
    public ResponseEntity<ClaudeResponse> ask(
            @RequestHeader("X-Session-ID") String sessionId,
            @RequestBody Map<String, Object> request) {
        Long companyId = Long.valueOf(String.valueOf(request.get("companyId")));
        String question = String.valueOf(request.get("question"));
        return ResponseEntity.ok(service.ask(sessionId, companyId, question));
    }

    @GetMapping("/obligations/{companyId}")
    public ResponseEntity<ClaudeResponse> obligations(@PathVariable Long companyId) {
        return ResponseEntity.ok(service.obligations(companyId));
    }

    @PostMapping("/reset")
    public ResponseEntity<Void> reset(@RequestHeader("X-Session-ID") String sessionId) {
        service.reset(sessionId);
        return ResponseEntity.noContent().build();
    }
}
