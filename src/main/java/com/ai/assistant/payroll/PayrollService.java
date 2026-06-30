package com.ai.assistant.payroll;

import com.ai.assistant.common.BatchParseResult;
import com.ai.assistant.company.CompanyService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class PayrollService {

    private final EmployeeRepository employeeRepository;
    private final ExpenseRepository expenseRepository;
    private final CompanyService companyService;
    private final ExpensePdfParser expensePdfParser;

    public PayrollService(EmployeeRepository employeeRepository,
                          ExpenseRepository expenseRepository,
                          CompanyService companyService,
                          ExpensePdfParser expensePdfParser) {
        this.employeeRepository = employeeRepository;
        this.expenseRepository = expenseRepository;
        this.companyService = companyService;
        this.expensePdfParser = expensePdfParser;
    }

    public Employee addEmployee(Long companyId, Employee employee) {
        companyService.get(companyId);
        employee.setId(null);
        employee.setCompanyId(companyId);
        return employeeRepository.save(employee);
    }

    public List<Employee> employees(Long companyId) {
        companyService.get(companyId);   // verifică ownership
        return employeeRepository.findByCompanyId(companyId);
    }

    public Expense addExpense(Long companyId, Expense expense) {
        companyService.get(companyId);
        expense.setId(null);
        expense.setCompanyId(companyId);
        return expenseRepository.save(expense);
    }

    public List<Expense> expenses(Long companyId) {
        companyService.get(companyId);   // verifică ownership
        return expenseRepository.findByCompanyId(companyId);
    }

    public void deleteEmployee(Long id) {
        Employee employee = employeeRepository.findById(id).orElseThrow();
        companyService.get(employee.getCompanyId());   // verifică ownership-ul firmei
        employeeRepository.deleteById(id);
    }

    public void deleteExpense(Long id) {
        Expense expense = expenseRepository.findById(id).orElseThrow();
        companyService.get(expense.getCompanyId());    // verifică ownership-ul firmei
        expenseRepository.deleteById(id);
    }

    /** Extrage datele unei cheltuieli dintr-un PDF (nu o salvează — userul confirmă apoi). */
    public ParsedExpense parseExpensePdf(Long companyId, MultipartFile file) throws IOException {
        companyService.get(companyId);
        return expensePdfParser.parse(file);
    }

    /** Extrage cheltuieli din mai multe PDF-uri; per fișier întoarce ori datele, ori eroarea. */
    public List<BatchParseResult<ParsedExpense>> parseExpensePdfBatch(Long companyId, MultipartFile[] files) {
        companyService.get(companyId);
        List<BatchParseResult<ParsedExpense>> results = new ArrayList<>();
        for (MultipartFile file : files) {
            String name = file.getOriginalFilename();
            try {
                results.add(BatchParseResult.ok(name, expensePdfParser.parse(file)));
            } catch (Exception e) {
                results.add(BatchParseResult.failed(name, e.getMessage()));
            }
        }
        return results;
    }
}
