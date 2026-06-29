package com.ai.assistant.invoicing;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
