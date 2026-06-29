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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyServiceOwnershipTest {

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
    void getDeniedWhenNotOwner() {
        Company c = new Company();
        c.setId(1L);
        c.setOwnerUserId(100L);
        when(repository.findById(1L)).thenReturn(Optional.of(c));

        authAs(999L); // alt user
        assertThrows(CompanyAccessDeniedException.class, () -> service().get(1L));
    }

    @Test
    void getAllowedForOwner() {
        Company c = new Company();
        c.setId(1L);
        c.setOwnerUserId(100L);
        when(repository.findById(1L)).thenReturn(Optional.of(c));

        authAs(100L); // owner
        assertEquals(1L, service().get(1L).getId());
    }
}
