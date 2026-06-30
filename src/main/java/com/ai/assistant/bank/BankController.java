package com.ai.assistant.bank;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
public class BankController {

    private final BankService service;

    public BankController(BankService service) {
        this.service = service;
    }

    /** Încarcă un extras bancar PDF; Claude extrage tranzacțiile (nu le salvează — userul confirmă). */
    @PostMapping("/companies/{companyId}/bank/parse")
    public ResponseEntity<?> parse(@PathVariable Long companyId, @RequestParam("file") MultipartFile file) {
        if (file.isEmpty() || file.getOriginalFilename() == null
                || !file.getOriginalFilename().toLowerCase().endsWith(".pdf")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Trimite un extras PDF (form-data, cheia 'file')."));
        }
        try {
            return ResponseEntity.ok(service.parseStatement(companyId, file));
        } catch (Exception e) {
            log.error("Bank statement parse failed for {}: {}", file.getOriginalFilename(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getClass().getSimpleName(), "message", String.valueOf(e.getMessage())));
        }
    }

    /** Salvează în lot tranzacțiile confirmate. */
    @PostMapping("/companies/{companyId}/bank/transactions")
    public ResponseEntity<List<BankTransaction>> saveAll(@PathVariable Long companyId,
                                                         @RequestBody List<BankTransaction> transactions) {
        return ResponseEntity.ok(service.saveAll(companyId, transactions));
    }

    @GetMapping("/companies/{companyId}/bank/transactions")
    public ResponseEntity<List<BankTransaction>> list(@PathVariable Long companyId) {
        return ResponseEntity.ok(service.list(companyId));
    }

    @DeleteMapping("/bank/transactions/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
