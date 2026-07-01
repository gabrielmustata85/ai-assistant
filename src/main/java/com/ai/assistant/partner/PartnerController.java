package com.ai.assistant.partner;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class PartnerController {

    private final PartnerService service;

    public PartnerController(PartnerService service) {
        this.service = service;
    }

    @GetMapping("/companies/{companyId}/partners")
    public ResponseEntity<List<Partner>> list(@PathVariable Long companyId) {
        return ResponseEntity.ok(service.list(companyId));
    }

    @PostMapping("/companies/{companyId}/partners")
    public ResponseEntity<Partner> add(@PathVariable Long companyId, @RequestBody Partner partner) {
        return ResponseEntity.ok(service.add(companyId, partner));
    }

    @PutMapping("/partners/{id}")
    public ResponseEntity<Partner> update(@PathVariable Long id, @RequestBody Partner partner) {
        return ResponseEntity.ok(service.update(id, partner));
    }

    @DeleteMapping("/partners/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
