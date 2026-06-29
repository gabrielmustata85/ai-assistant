package com.ai.assistant.company;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {

    @Mock CompanyRepository repository;

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private void authAs(Long userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null, List.of()));
    }

    private CompanyService service() {
        return new CompanyService(repository, new CompanyAccessGuard());
    }

    @Test
    void getThrowsWhenMissing() {
        when(repository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(CompanyNotFoundException.class, () -> service().get(99L));
    }

    @Test
    void updateAppliesPatchAndSaves() {
        Company existing = new Company();
        existing.setId(1L);
        existing.setName("Old");
        existing.setVatPayer(false);
        existing.setOwnerUserId(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(any(Company.class))).thenAnswer(inv -> inv.getArgument(0));

        Company patch = new Company();
        patch.setName("New SRL");
        patch.setVatPayer(true);
        patch.setTaxRegime(TaxRegime.PROFIT_16);

        authAs(1L);
        Company result = service().update(1L, patch);

        assertEquals("New SRL", result.getName());
        assertTrue(result.getVatPayer());
        assertEquals(TaxRegime.PROFIT_16, result.getTaxRegime());
    }

    @Test
    void updateLeavesVatPayerUnchangedWhenOmitted() {
        Company existing = new Company();
        existing.setId(2L);
        existing.setName("Existing SRL");
        existing.setVatPayer(true);
        existing.setOwnerUserId(1L);
        when(repository.findById(2L)).thenReturn(Optional.of(existing));
        when(repository.save(any(Company.class))).thenAnswer(inv -> inv.getArgument(0));

        Company patch = new Company();
        patch.setName("Updated Name SRL");
        // vatPayer intentionally left null (not provided in PATCH body)

        authAs(1L);
        Company result = service().update(2L, patch);

        assertEquals("Updated Name SRL", result.getName());
        assertEquals(Boolean.TRUE, result.getVatPayer());
    }
}
