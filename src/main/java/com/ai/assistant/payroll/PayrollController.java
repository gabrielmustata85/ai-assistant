package com.ai.assistant.payroll;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
}
