package com.ai.assistant.company;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {

    @Mock CompanyRepository repository;
    @InjectMocks CompanyService service;

    @Test
    void getThrowsWhenMissing() {
        when(repository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(CompanyNotFoundException.class, () -> service.get(99L));
    }

    @Test
    void updateAppliesPatchAndSaves() {
        Company existing = new Company();
        existing.setId(1L);
        existing.setName("Old");
        existing.setVatPayer(false);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(any(Company.class))).thenAnswer(inv -> inv.getArgument(0));

        Company patch = new Company();
        patch.setName("New SRL");
        patch.setVatPayer(true);
        patch.setTaxRegime(TaxRegime.PROFIT_16);

        Company result = service.update(1L, patch);

        assertEquals("New SRL", result.getName());
        assertTrue(result.isVatPayer());
        assertEquals(TaxRegime.PROFIT_16, result.getTaxRegime());
    }
}
