package com.ai.assistant.payroll;

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
public class PayrollController {

    private final PayrollService service;

    public PayrollController(PayrollService service) {
        this.service = service;
    }

    @PostMapping("/companies/{companyId}/employees")
    public ResponseEntity<Employee> addEmployee(@PathVariable Long companyId, @RequestBody Employee employee) {
        return ResponseEntity.ok(service.addEmployee(companyId, employee));
    }

    @GetMapping("/companies/{companyId}/employees")
    public ResponseEntity<List<Employee>> employees(@PathVariable Long companyId) {
        return ResponseEntity.ok(service.employees(companyId));
    }

    @PostMapping("/companies/{companyId}/expenses")
    public ResponseEntity<Expense> addExpense(@PathVariable Long companyId, @RequestBody Expense expense) {
        return ResponseEntity.ok(service.addExpense(companyId, expense));
    }

    @GetMapping("/companies/{companyId}/expenses")
    public ResponseEntity<List<Expense>> expenses(@PathVariable Long companyId) {
        return ResponseEntity.ok(service.expenses(companyId));
    }

    @DeleteMapping("/employees/{id}")
    public ResponseEntity<Void> deleteEmployee(@PathVariable Long id) {
        service.deleteEmployee(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/expenses/{id}")
    public ResponseEntity<Void> deleteExpense(@PathVariable Long id) {
        service.deleteExpense(id);
        return ResponseEntity.noContent().build();
    }

    /** Încarcă un PDF (bon/factură); Claude extrage datele cheltuielii (nu salvează — userul confirmă). */
    @PostMapping("/companies/{companyId}/expenses/parse")
    public ResponseEntity<?> parseExpense(@PathVariable Long companyId, @RequestParam("file") MultipartFile file) {
        if (file.isEmpty() || file.getOriginalFilename() == null
                || !file.getOriginalFilename().toLowerCase().endsWith(".pdf")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Trimite un fișier PDF (form-data, cheia 'file')."));
        }
        try {
            return ResponseEntity.ok(service.parseExpensePdf(companyId, file));
        } catch (Exception e) {
            log.error("Expense PDF parse failed for {}: {}", file.getOriginalFilename(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getClass().getSimpleName(), "message", String.valueOf(e.getMessage())));
        }
    }

    /** Încarcă mai multe PDF-uri de cheltuieli; întoarce datele extrase per fișier. */
    @PostMapping("/companies/{companyId}/expenses/parse-batch")
    public ResponseEntity<List<BatchParseResult<ParsedExpense>>> parseExpenseBatch(@PathVariable Long companyId,
                                                                @RequestParam("files") MultipartFile[] files) {
        return ResponseEntity.ok(service.parseExpensePdfBatch(companyId, files));
    }
}
