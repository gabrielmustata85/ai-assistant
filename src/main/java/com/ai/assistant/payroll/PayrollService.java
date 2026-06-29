package com.ai.assistant.payroll;

import com.ai.assistant.company.CompanyService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PayrollService {

    private final EmployeeRepository employeeRepository;
    private final ExpenseRepository expenseRepository;
    private final CompanyService companyService;

    public PayrollService(EmployeeRepository employeeRepository,
                          ExpenseRepository expenseRepository,
                          CompanyService companyService) {
        this.employeeRepository = employeeRepository;
        this.expenseRepository = expenseRepository;
        this.companyService = companyService;
    }

    public Employee addEmployee(Long companyId, Employee employee) {
        companyService.get(companyId);
        employee.setId(null);
        employee.setCompanyId(companyId);
        return employeeRepository.save(employee);
    }

    public List<Employee> employees(Long companyId) {
        return employeeRepository.findByCompanyId(companyId);
    }

    public Expense addExpense(Long companyId, Expense expense) {
        companyService.get(companyId);
        expense.setId(null);
        expense.setCompanyId(companyId);
        return expenseRepository.save(expense);
    }

    public List<Expense> expenses(Long companyId) {
        return expenseRepository.findByCompanyId(companyId);
    }
}
