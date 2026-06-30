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

    /** Extrage TOATE cheltuielile dintr-un PDF (poate conține mai multe). Nu salvează — userul confirmă. */
    public List<ParsedExpense> parseExpensePdf(Long companyId, MultipartFile file) throws IOException {
        companyService.get(companyId);
        return expensePdfParser.parse(file);
    }

    /** Extrage cheltuieli din mai multe PDF-uri, fiecare putând conține mai multe; listă plată per cheltuială. */
    public List<BatchParseResult<ParsedExpense>> parseExpensePdfBatch(Long companyId, MultipartFile[] files) {
        companyService.get(companyId);
        List<BatchParseResult<ParsedExpense>> results = new ArrayList<>();
        for (MultipartFile file : files) {
            String name = file.getOriginalFilename();
            try {
                List<ParsedExpense> expenses = expensePdfParser.parse(file);
                if (expenses.isEmpty()) {
                    results.add(BatchParseResult.failed(name, "Nicio cheltuială găsită în document."));
                } else {
                    for (ParsedExpense ex : expenses) {
                        results.add(BatchParseResult.ok(name, ex));
                    }
                }
            } catch (Exception e) {
                results.add(BatchParseResult.failed(name, e.getMessage()));
            }
        }
        return results;
    }
}
