package com.ai.assistant.auth;

import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {

    private static final String SECRET = "test-secret-test-secret-test-secret-123456";

    @Test
    void issuedTokenParsesBackToUserId() {
        JwtService jwt = new JwtService(SECRET);
        String token = jwt.issue(42L, "ana");
        assertEquals(42L, jwt.parseUserId(token));
    }

    @Test
    void tokenSignedWithDifferentSecretIsRejected() {
        String token = new JwtService(SECRET).issue(1L, "ana");
        JwtService other = new JwtService("other-secret-other-secret-other-secret-99");
        assertThrows(SignatureException.class, () -> other.parseUserId(token));
    }
}
