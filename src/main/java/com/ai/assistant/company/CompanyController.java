package com.ai.assistant.company;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/companies")
public class CompanyController {

    private final CompanyService service;

    public CompanyController(CompanyService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<Company> create(@RequestBody Company company) {
        return ResponseEntity.status(201).body(service.create(company));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Company> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.get(id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Company> update(@PathVariable Long id, @RequestBody Company patch) {
        return ResponseEntity.ok(service.update(id, patch));
    }

    @ExceptionHandler(CompanyNotFoundException.class)
    public ResponseEntity<String> notFound(CompanyNotFoundException e) {
        return ResponseEntity.status(404).body(e.getMessage());
    }
}
