package com.ai.assistant.auth;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock AppUserRepository repository;
    @Mock JwtService jwtService;

    private AuthService newService(PasswordEncoder encoder) {
        return new AuthService(repository, jwtService, encoder);
    }

    @Test
    void loginWithWrongPasswordThrows() {
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        AppUser user = new AppUser();
        user.setId(1L);
        user.setUsername("ana");
        user.setPasswordHash(encoder.encode("correct"));
        when(repository.findByUsername("ana")).thenReturn(Optional.of(user));

        AuthService service = newService(encoder);
        assertThrows(InvalidCredentialsException.class, () -> service.login("ana", "wrong"));
    }

    @Test
    void loginWithCorrectPasswordReturnsToken() {
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        AppUser user = new AppUser();
        user.setId(1L);
        user.setUsername("ana");
        user.setPasswordHash(encoder.encode("correct"));
        when(repository.findByUsername("ana")).thenReturn(Optional.of(user));
        when(jwtService.issue(1L, "ana")).thenReturn("token-123");

        AuthService service = newService(encoder);
        assertEquals("token-123", service.login("ana", "correct"));
    }
}
