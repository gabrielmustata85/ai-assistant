package com.ai.assistant.invoicing;

import com.ai.assistant.company.CompanyService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock InvoiceRepository repository;
    @Mock CompanyService companyService;
    @InjectMocks InvoiceService service;

    @Test
    void addValidatesCompanyAndSetsCompanyId() {
        when(repository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        Invoice inv = new Invoice();
        inv.setDirection(Direction.RECEIVED);
        inv.setIssueDate(LocalDate.of(2026, 1, 10));
        inv.setNetAmount(new BigDecimal("100.00"));
        inv.setGrossAmount(new BigDecimal("119.00"));

        Invoice saved = service.add(5L, inv);

        verify(companyService).get(5L);          // firma e validată
        assertEquals(5L, saved.getCompanyId());  // company_id e setat din path
    }
}
