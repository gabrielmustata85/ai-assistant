package com.ai.assistant.invoicing;

import com.ai.assistant.common.BatchParseResult;
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
    private final InvoiceAssistantService assistant;

    public InvoiceController(InvoiceService service, InvoiceAssistantService assistant) {
        this.service = service;
        this.assistant = assistant;
    }

    @PostMapping("/companies/{companyId}/invoices")
    public ResponseEntity<Invoice> add(@PathVariable Long companyId, @RequestBody Invoice invoice) {
        return ResponseEntity.ok(service.add(companyId, invoice));
    }

    /** Marius pregătește o schiță de factură dintr-o instrucțiune; userul o verifică și o emite. */
    @PostMapping("/companies/{companyId}/invoices/draft")
    public ResponseEntity<?> draft(@PathVariable Long companyId, @RequestBody Map<String, String> body) {
        String instruction = body.get("instruction");
        if (instruction == null || instruction.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Lipsește 'instruction'."));
        }
        try {
            return ResponseEntity.ok(assistant.draft(companyId, instruction));
        } catch (Exception e) {
            log.error("Invoice draft failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getClass().getSimpleName(), "message", String.valueOf(e.getMessage())));
        }
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

    /** Încarcă mai multe PDF-uri; întoarce datele extrase per fișier (cu erori individuale). */
    @PostMapping("/companies/{companyId}/invoices/parse-batch")
    public ResponseEntity<List<BatchParseResult<ParsedInvoice>>> parseBatch(@PathVariable Long companyId,
                                                             @RequestParam("files") MultipartFile[] files) {
        return ResponseEntity.ok(service.parsePdfBatch(companyId, files));
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
