package com.ai.assistant.invoicing;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
public class InvoiceController {

    private final InvoiceService service;

    public InvoiceController(InvoiceService service) {
        this.service = service;
    }

    @PostMapping("/companies/{companyId}/invoices")
    public ResponseEntity<Invoice> add(@PathVariable Long companyId, @RequestBody Invoice invoice) {
        return ResponseEntity.ok(service.add(companyId, invoice));
    }

    /** Încarcă un PDF de factură; Claude extrage datele și le întoarce (nu salvează — userul confirmă). */
    @PostMapping("/companies/{companyId}/invoices/parse")
    public ResponseEntity<?> parse(@PathVariable Long companyId, @RequestParam("file") MultipartFile file) {
        if (file.isEmpty() || file.getOriginalFilename() == null
                || !file.getOriginalFilename().toLowerCase().endsWith(".pdf")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Trimite un fișier PDF (form-data, cheia 'file')."));
        }
        try {
            return ResponseEntity.ok(service.parsePdf(companyId, file));
        } catch (Exception e) {
            log.error("Invoice PDF parse failed for {}: {}", file.getOriginalFilename(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getClass().getSimpleName(), "message", String.valueOf(e.getMessage())));
        }
    }

    @GetMapping("/companies/{companyId}/invoices")
    public ResponseEntity<List<Invoice>> list(@PathVariable Long companyId) {
        return ResponseEntity.ok(service.listForCompany(companyId));
    }

    @DeleteMapping("/invoices/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
