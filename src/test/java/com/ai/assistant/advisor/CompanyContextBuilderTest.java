package com.ai.assistant.advisor;

import com.ai.assistant.bank.BankService;
import com.ai.assistant.company.*;
import com.ai.assistant.invoicing.*;
import com.ai.assistant.payroll.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyContextBuilderTest {

    @Mock CompanyService companyService;
    @Mock InvoiceService invoiceService;
    @Mock PayrollService payrollService;
    @Mock BankService bankService;
    @InjectMocks CompanyContextBuilder builder;

    @Test
    void includesCompanyRegimeInvoicesAndEmployees() {
        Company c = new Company();
        c.setId(1L);
        c.setCui("RO123");
        c.setName("Acme SRL");
        c.setCompanyType(CompanyType.SRL);
        c.setTaxRegime(TaxRegime.MICRO_1);
        c.setVatPayer(true);
        when(companyService.get(1L)).thenReturn(c);

        Invoice inv = new Invoice();
        inv.setDirection(Direction.ISSUED);
        inv.setNetAmount(new BigDecimal("1000.00"));
        inv.setGrossAmount(new BigDecimal("1190.00"));
        inv.setIssueDate(LocalDate.of(2026, 3, 1));
        when(invoiceService.listForCompany(1L)).thenReturn(List.of(inv));

        Employee e = new Employee();
        e.setFullName("Ion Pop");
        e.setGrossSalary(new BigDecimal("5000.00"));
        when(payrollService.employees(1L)).thenReturn(List.of(e));
        when(payrollService.expenses(1L)).thenReturn(List.of());
        when(bankService.list(1L)).thenReturn(List.of());

        String ctx = builder.build(1L);

        assertTrue(ctx.contains("RO123"));
        assertTrue(ctx.contains("MICRO_1"));
        assertTrue(ctx.contains("1190.00"));
        assertTrue(ctx.contains("Ion Pop"));
    }
}
