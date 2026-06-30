package com.ai.assistant.knowledge;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/knowledge")
public class KnowledgeController {

    private final KnowledgeService service;

    public KnowledgeController(KnowledgeService service) {
        this.service = service;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty() || file.getOriginalFilename() == null
                || !file.getOriginalFilename().toLowerCase().endsWith(".pdf")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Trimite un fișier PDF valid (form-data, cheia 'file')."));
        }
        try {
            return ResponseEntity.ok(service.ingest(file));
        } catch (Exception e) {
            // Surface the real cause (Voyage / Pinecone / PDF) instead of a silent 500
            log.error("Knowledge upload failed for {}: {}", file.getOriginalFilename(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getClass().getSimpleName(),
                            "message", String.valueOf(e.getMessage())));
        }
    }

    @GetMapping("/documents")
    public ResponseEntity<List<KnowledgeDocument>> documents() {
        return ResponseEntity.ok(service.list());
    }
}
