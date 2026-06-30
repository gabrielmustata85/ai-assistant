package com.ai.assistant.payroll;

import com.ai.assistant.company.CompanyService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PayrollServiceTest {

    @Mock EmployeeRepository employeeRepository;
    @Mock ExpenseRepository expenseRepository;
    @Mock CompanyService companyService;
    @Mock ExpensePdfParser expensePdfParser;
    @InjectMocks PayrollService service;

    @Test
    void addEmployeeValidatesCompanyAndSetsCompanyId() {
        when(employeeRepository.save(any(Employee.class))).thenAnswer(inv -> inv.getArgument(0));

        Employee e = new Employee();
        e.setFullName("Ion Pop");
        e.setGrossSalary(new BigDecimal("5000.00"));

        Employee saved = service.addEmployee(7L, e);

        verify(companyService).get(7L);
        assertEquals(7L, saved.getCompanyId());
    }

    @Test
    void employeesGoesThroughCompanyGuard() {
        when(employeeRepository.findByCompanyId(7L)).thenReturn(java.util.List.of());

        service.employees(7L);

        verify(companyService).get(7L);
    }

    @Test
    void expensesGoesThroughCompanyGuard() {
        when(expenseRepository.findByCompanyId(7L)).thenReturn(java.util.List.of());

        service.expenses(7L);

        verify(companyService).get(7L);
    }
}
