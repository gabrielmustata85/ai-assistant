# Plan 4 — Autentificare (JWT) & Izolare Tenant

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fiecare user se autentifică (JWT) și vede/modifică DOAR firmele lui. Un user nu poate accesa facturile/datele altui user.

**Architecture:** Spring Security stateless cu JWT (bearer token). `AppUser` deține una sau mai multe firme (`company.owner_user_id`). Un filtru JWT pune userul curent în `SecurityContext`. **Punct unic de control:** `CompanyService.get(companyId)` verifică ownership-ul prin `CompanyAccessGuard`; pentru că invoice/payroll/advisor trec prin `CompanyService.get`, izolarea se aplică automat peste tot (defense in depth).

**Tech Stack:** Spring Security (Spring Boot 4), `io.jsonwebtoken:jjwt` 0.12.6, BCrypt, JPA, Flyway, JUnit 5 + Mockito.

**Prerequisite:** Plan 1, 2 și 3 finalizate.

## Global Constraints

- Java **21**. Secretul JWT din mediu: `JWT_SECRET` (minim 32 de caractere / 256 biți pentru HS256).
- Userul curent se ia DOAR din `SecurityContext` — niciodată din body/URL/header de tip `userId`.
- Parole stocate hash-uite cu **BCrypt**; niciodată în clar.
- Endpoint-urile publice: `POST /auth/register`, `POST /auth/login`. Restul cer JWT valid.
- Verificarea ownership eșuată → **403**; lipsă/invaliditate token → **401**.
- Comenzi: `./mvnw -q -Dtest=<ClassName> test`, build `./mvnw -q -DskipTests package`.

---

### Task 1: Dependențe JWT + migrația V3 (app_user + owner_user_id)

**Files:**
- Modify: `pom.xml`
- Create: `src/main/resources/db/migration/V3__auth.sql`

**Interfaces:**
- Produces: jjwt pe classpath; tabel `app_user`; coloana `company.owner_user_id`.

- [ ] **Step 1: Adaugă dependențele jjwt în `pom.xml`**

```xml
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.6</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
```

- [ ] **Step 2: Scrie migrația**

`V3__auth.sql`:
```sql
CREATE TABLE app_user (
    id            BIGSERIAL PRIMARY KEY,
    username      VARCHAR(128) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT now()
);

ALTER TABLE company ADD COLUMN owner_user_id BIGINT REFERENCES app_user(id);
CREATE INDEX idx_company_owner ON company(owner_user_id);
```

- [ ] **Step 3: Adaugă câmpul `ownerUserId` pe entitatea `Company`**

În `src/main/java/com/ai/assistant/company/Company.java`, adaugă:
```java
    @Column(name = "owner_user_id")
    private Long ownerUserId;
```

- [ ] **Step 4: Verifică build-ul**

Run: `./mvnw -q -DskipTests package`
Expected: BUILD SUCCESS.

- [ ] **Step 5: Commit**

```bash
git add pom.xml src/main/resources/db/migration/V3__auth.sql \
        src/main/java/com/ai/assistant/company/Company.java
git commit -m "feat(auth): jjwt deps, V3 app_user + company.owner_user_id"
```

---

### Task 2: `AppUser` entitate + repository

**Files:**
- Create: `src/main/java/com/ai/assistant/auth/AppUser.java`
- Create: `src/main/java/com/ai/assistant/auth/AppUserRepository.java`

**Interfaces:**
- Produces:
  - `AppUser` entitate JPA (`id`, `username`, `passwordHash`, `createdAt`).
  - `AppUserRepository extends JpaRepository<AppUser, Long>` cu `Optional<AppUser> findByUsername(String username)`.

- [ ] **Step 1: Scrie entitatea**

`AppUser.java`:
```java
package com.ai.assistant.auth;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "app_user")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 128)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
```

- [ ] **Step 2: Scrie repository-ul**

`AppUserRepository.java`:
```java
package com.ai.assistant.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByUsername(String username);
}
```

- [ ] **Step 3: Verifică build + commit**

Run: `./mvnw -q -DskipTests package`
Expected: BUILD SUCCESS.
```bash
git add src/main/java/com/ai/assistant/auth/AppUser.java \
        src/main/java/com/ai/assistant/auth/AppUserRepository.java
git commit -m "feat(auth): AppUser entity + repository"
```

---

### Task 3: `JwtService` (emitere + validare token)

**Files:**
- Create: `src/main/java/com/ai/assistant/auth/JwtService.java`
- Test: `src/test/java/com/ai/assistant/auth/JwtServiceTest.java`

**Interfaces:**
- Produces:
  - `String JwtService.issue(Long userId, String username)`
  - `Long JwtService.parseUserId(String token)` — aruncă dacă tokenul e invalid/expirat.
  - Constructor `JwtService(@Value("${jwt.secret}") String secret)`.

- [ ] **Step 1: Scrie testul care eșuează (round-trip)**

`JwtServiceTest.java`:
```java
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
```

- [ ] **Step 2: Rulează testul — trebuie să eșueze**

Run: `./mvnw -q -Dtest=JwtServiceTest test`
Expected: FAIL — `JwtService` nu există.

- [ ] **Step 3: Scrie service-ul**

`JwtService.java`:
```java
package com.ai.assistant.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    private static final long EXPIRATION_MS = 24L * 60 * 60 * 1000; // 24h

    private final SecretKey key;

    public JwtService(@Value("${jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String issue(Long userId, String username) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(username)
                .claim("uid", userId)
                .issuedAt(new Date(now))
                .expiration(new Date(now + EXPIRATION_MS))
                .signWith(key)
                .compact();
    }

    public Long parseUserId(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.get("uid", Long.class);
    }
}
```

- [ ] **Step 4: Rulează testul — trebuie să treacă**

Run: `./mvnw -q -Dtest=JwtServiceTest test`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/ai/assistant/auth/JwtService.java \
        src/test/java/com/ai/assistant/auth/JwtServiceTest.java
git commit -m "feat(auth): JWT issue/validate service"
```

---

### Task 4: `CurrentUser` + `AuthService` + `AuthController` (register/login)

**Files:**
- Create: `src/main/java/com/ai/assistant/auth/CurrentUser.java`
- Create: `src/main/java/com/ai/assistant/auth/AuthService.java`
- Create: `src/main/java/com/ai/assistant/auth/AuthController.java`
- Test: `src/test/java/com/ai/assistant/auth/AuthServiceTest.java`

**Interfaces:**
- Consumes: `AppUserRepository`, `JwtService`, `PasswordEncoder` (bean din SecurityConfig, Task 5).
- Produces:
  - `CurrentUser.id()` static → `Long` (userId din `SecurityContext`, sau `null`).
  - `AuthService.register(String username, String rawPassword)` → `AppUser`; `AuthService.login(String username, String rawPassword)` → `String token`.
  - Endpoint-uri `POST /auth/register`, `POST /auth/login` întorcând `{ "token": "..." }`.
  - `InvalidCredentialsException extends RuntimeException`.

- [ ] **Step 1: Scrie `CurrentUser` (helper SecurityContext)**

`CurrentUser.java`:
```java
package com.ai.assistant.auth;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class CurrentUser {

    private CurrentUser() {}

    /** userId-ul autentificat (principal-ul filtrului JWT), sau null dacă nu e nimeni autentificat. */
    public static Long id() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        Object principal = auth.getPrincipal();
        return (principal instanceof Long) ? (Long) principal : null;
    }
}
```

- [ ] **Step 2: Scrie testul de AuthService care eșuează**

`AuthServiceTest.java`:
```java
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
```

- [ ] **Step 3: Rulează testul — trebuie să eșueze**

Run: `./mvnw -q -Dtest=AuthServiceTest test`
Expected: FAIL — `AuthService` / `InvalidCredentialsException` nu există.

- [ ] **Step 4: Scrie excepția și service-ul**

`InvalidCredentialsException.java`:
```java
package com.ai.assistant.auth;

public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException() {
        super("Invalid username or password");
    }
}
```

`AuthService.java`:
```java
package com.ai.assistant.auth;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AppUserRepository repository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public AuthService(AppUserRepository repository, JwtService jwtService, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    public AppUser register(String username, String rawPassword) {
        if (repository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username already taken");
        }
        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        return repository.save(user);
    }

    public String login(String username, String rawPassword) {
        AppUser user = repository.findByUsername(username)
                .orElseThrow(InvalidCredentialsException::new);
        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        return jwtService.issue(user.getId(), user.getUsername());
    }
}
```

- [ ] **Step 5: Rulează testul — trebuie să treacă**

Run: `./mvnw -q -Dtest=AuthServiceTest test`
Expected: PASS.

- [ ] **Step 6: Scrie controllerul**

`AuthController.java`:
```java
package com.ai.assistant.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody Map<String, String> body) {
        AppUser user = service.register(body.get("username"), body.get("password"));
        return ResponseEntity.ok(Map.of("id", user.getId(), "username", user.getUsername()));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody Map<String, String> body) {
        String token = service.login(body.get("username"), body.get("password"));
        return ResponseEntity.ok(Map.of("token", token));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<String> badCreds(InvalidCredentialsException e) {
        return ResponseEntity.status(401).body(e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> badRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }
}
```

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/ai/assistant/auth/CurrentUser.java \
        src/main/java/com/ai/assistant/auth/AuthService.java \
        src/main/java/com/ai/assistant/auth/InvalidCredentialsException.java \
        src/main/java/com/ai/assistant/auth/AuthController.java \
        src/test/java/com/ai/assistant/auth/AuthServiceTest.java
git commit -m "feat(auth): register/login + current-user helper"
```

---

### Task 5: `JwtAuthFilter` + `SecurityConfig`

**Files:**
- Create: `src/main/java/com/ai/assistant/auth/JwtAuthFilter.java`
- Create: `src/main/java/com/ai/assistant/config/SecurityConfig.java`
- Modify: `src/main/resources/application.properties`

**Interfaces:**
- Consumes: `JwtService.parseUserId`.
- Produces:
  - `JwtAuthFilter` (OncePerRequestFilter) care pune `userId` ca principal în `SecurityContext`.
  - `SecurityConfig`: bean `SecurityFilterChain` (stateless, `/auth/**` public, restul autentificat), bean `PasswordEncoder` (BCrypt).
  - Proprietatea `jwt.secret=${JWT_SECRET}`.

- [ ] **Step 1: Scrie filtrul JWT**

`JwtAuthFilter.java`:
```java
package com.ai.assistant.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                Long userId = jwtService.parseUserId(token);
                var auth = new UsernamePasswordAuthenticationToken(userId, null, List.of());
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (Exception e) {
                // token invalid/expirat → rămâne neautentificat; Security va răspunde 401/403
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }
}
```

- [ ] **Step 2: Scrie `SecurityConfig`**

`SecurityConfig.java`:
```java
package com.ai.assistant.config;

import com.ai.assistant.auth.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthFilter jwtAuthFilter) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/**").permitAll()
                .anyRequest().authenticated())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
```

- [ ] **Step 3: Adaugă secretul în config**

În `application.properties`, adaugă: `jwt.secret=${JWT_SECRET}`.

- [ ] **Step 4: Verifică build-ul**

Run: `./mvnw -q -DskipTests package`
Expected: BUILD SUCCESS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/ai/assistant/auth/JwtAuthFilter.java \
        src/main/java/com/ai/assistant/config/SecurityConfig.java \
        src/main/resources/application.properties
git commit -m "feat(auth): JWT filter + stateless SecurityConfig"
```

---

### Task 6: Izolare tenant — guard de ownership în `CompanyService` (+ acoperire read paths)

**Files:**
- Create: `src/main/java/com/ai/assistant/company/CompanyAccessGuard.java`
- Create: `src/main/java/com/ai/assistant/company/CompanyAccessDeniedException.java`
- Modify: `src/main/java/com/ai/assistant/company/CompanyService.java`
- Modify: `src/main/java/com/ai/assistant/invoicing/InvoiceService.java`
- Modify: `src/main/java/com/ai/assistant/payroll/PayrollService.java`
- Test: `src/test/java/com/ai/assistant/company/CompanyServiceOwnershipTest.java`

**Interfaces:**
- Consumes: `CurrentUser.id()`.
- Produces:
  - `CompanyAccessGuard.assertOwner(Long ownerUserId)` — aruncă `CompanyAccessDeniedException` dacă userul curent ≠ owner.
  - `CompanyService.create` setează `ownerUserId = CurrentUser.id()`.
  - `CompanyService.get(Long)` verifică ownership prin guard (PUNCT UNIC DE CONTROL).
  - `InvoiceService.listForCompany` și `delete`, `PayrollService.employees/expenses` apelează `companyService.get(companyId)` întâi (ca să moștenească verificarea).

- [ ] **Step 1: Scrie guard-ul și excepția**

`CompanyAccessDeniedException.java`:
```java
package com.ai.assistant.company;

public class CompanyAccessDeniedException extends RuntimeException {
    public CompanyAccessDeniedException() {
        super("You do not have access to this company's data");
    }
}
```

`CompanyAccessGuard.java`:
```java
package com.ai.assistant.company;

import com.ai.assistant.auth.CurrentUser;
import org.springframework.stereotype.Component;

@Component
public class CompanyAccessGuard {

    /** Aruncă dacă userul autentificat nu este proprietarul firmei. */
    public void assertOwner(Long ownerUserId) {
        Long current = CurrentUser.id();
        if (current == null || ownerUserId == null || !current.equals(ownerUserId)) {
            throw new CompanyAccessDeniedException();
        }
    }
}
```

- [ ] **Step 2: Scrie testul de ownership care eșuează**

`CompanyServiceOwnershipTest.java`:
```java
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
```

- [ ] **Step 3: Rulează testul — trebuie să eșueze**

Run: `./mvnw -q -Dtest=CompanyServiceOwnershipTest test`
Expected: FAIL — constructorul `CompanyService(repository, guard)` nu există încă.

- [ ] **Step 4: Modifică `CompanyService` (injectează guard, setează owner, verifică în get)**

Înlocuiește conținutul lui `CompanyService.java`:
```java
package com.ai.assistant.company;

import com.ai.assistant.auth.CurrentUser;
import org.springframework.stereotype.Service;

@Service
public class CompanyService {

    private final CompanyRepository repository;
    private final CompanyAccessGuard accessGuard;

    public CompanyService(CompanyRepository repository, CompanyAccessGuard accessGuard) {
        this.repository = repository;
        this.accessGuard = accessGuard;
    }

    public Company create(Company company) {
        company.setId(null);
        company.setOwnerUserId(CurrentUser.id());   // firma aparține userului care o creează
        return repository.save(company);
    }

    /** PUNCT UNIC DE CONTROL: orice acces la o firmă trece prin verificarea de ownership. */
    public Company get(Long id) {
        Company company = repository.findById(id).orElseThrow(() -> new CompanyNotFoundException(id));
        accessGuard.assertOwner(company.getOwnerUserId());
        return company;
    }

    public Company update(Long id, Company patch) {
        Company existing = get(id);   // get() verifică deja ownership-ul
        if (patch.getName() != null) existing.setName(patch.getName());
        if (patch.getCui() != null) existing.setCui(patch.getCui());
        if (patch.getCompanyType() != null) existing.setCompanyType(patch.getCompanyType());
        if (patch.getTaxRegime() != null) existing.setTaxRegime(patch.getTaxRegime());
        existing.setVatPayer(patch.isVatPayer());
        return repository.save(existing);
    }
}
```

- [ ] **Step 5: Acoperă read paths în `InvoiceService`**

În `InvoiceService.java`, fă ca `listForCompany` și `delete` să treacă prin `companyService.get`:
- în `listForCompany`, adaugă pe prima linie: `companyService.get(companyId);`
- în `delete`, încarcă factura, ia `companyId`-ul ei și verifică: 
```java
    public void delete(Long id) {
        Invoice invoice = repository.findById(id).orElseThrow();
        companyService.get(invoice.getCompanyId());   // verifică ownership-ul firmei facturii
        repository.deleteById(id);
    }
```

- [ ] **Step 6: Acoperă read paths în `PayrollService`**

În `PayrollService.java`, adaugă `companyService.get(companyId);` pe prima linie din `employees(companyId)` și din `expenses(companyId)`.

- [ ] **Step 7: Rulează testul de ownership + toate testele**

Run: `./mvnw -q -Dtest=CompanyServiceOwnershipTest test`
Expected: PASS.
Run: `./mvnw -q test`
Expected: toate testele PASS. (Notă: `CompanyServiceTest` din Plan 2 folosea constructorul cu un singur argument — actualizează-l să folosească `new CompanyService(repository, new CompanyAccessGuard())` și autentifică un user în context acolo unde apelează `get/update`, la fel ca în testul de ownership.)

- [ ] **Step 8: Verifică build + commit**

Run: `./mvnw -q -DskipTests package`
Expected: BUILD SUCCESS.
```bash
git add -A
git commit -m "feat(tenancy): ownership guard via CompanyService.get chokepoint"
```

---

### Task 7: Izolare în advisor + verificare manuală end-to-end

**Files:**
- Modify: `src/test/java/com/ai/assistant/advisor/AdvisorServiceTest.java` (dacă e necesar pentru compilare după schimbarea constructorului CompanyService — advisor-ul consumă serviciile, nu CompanyService direct, deci de obicei nu se schimbă)
- (verificare; fără cod nou de producție — advisor-ul e deja protejat prin `CompanyContextBuilder` → `CompanyService.get`)

**Interfaces:**
- Consumes: lanțul existent `AdvisorService` → `CompanyContextBuilder.build` → `CompanyService.get` (acum cu guard).

- [ ] **Step 1: Confirmă că advisor-ul moștenește izolarea**

`CompanyContextBuilder.build(companyId)` (Plan 3 Task 4) apelează `companyService.get(companyId)`, care acum verifică ownership-ul. Deci `POST /advisor/ask` și `GET /advisor/obligations/{companyId}` resping automat accesul la firme străine cu **403**. Nu e nevoie de cod nou.

- [ ] **Step 2: Rulează întreaga suită de teste**

Run: `./mvnw -q test`
Expected: toate testele PASS (cele de rețea SKIPPED fără chei).

- [ ] **Step 3: Verificare manuală (cu DB + chei setate) — opțional dar recomandat**

Pornește aplicația cu `ANTHROPIC_API_KEY`, `VOYAGE_API_KEY`, `PINECONE_API_KEY`, `JWT_SECRET`, `DB_USER`, `DB_PASSWORD` setate. Apoi:
```bash
# user A
curl -s -XPOST localhost:8080/auth/register -H 'Content-Type: application/json' -d '{"username":"a","password":"p1"}'
TOKEN_A=$(curl -s -XPOST localhost:8080/auth/login -H 'Content-Type: application/json' -d '{"username":"a","password":"p1"}' | sed 's/.*"token":"//;s/".*//')
# user A creează o firmă
CID=$(curl -s -XPOST localhost:8080/companies -H "Authorization: Bearer $TOKEN_A" -H 'Content-Type: application/json' -d '{"cui":"RO1","name":"A SRL","companyType":"SRL","taxRegime":"MICRO_1","vatPayer":false}' | sed 's/.*"id"://;s/[,}].*//')

# user B
curl -s -XPOST localhost:8080/auth/register -H 'Content-Type: application/json' -d '{"username":"b","password":"p2"}'
TOKEN_B=$(curl -s -XPOST localhost:8080/auth/login -H 'Content-Type: application/json' -d '{"username":"b","password":"p2"}' | sed 's/.*"token":"//;s/".*//')
# user B încearcă să vadă firma lui A → trebuie 403
curl -s -o /dev/null -w "%{http_code}\n" localhost:8080/companies/$CID -H "Authorization: Bearer $TOKEN_B"
```
Expected: ultima comandă afișează **403**. Fără token → **401**.

- [ ] **Step 4: Commit (dacă a fost nevoie de ajustări de test)**

```bash
git add -A
git commit -m "test(tenancy): confirm advisor inherits ownership isolation"
```

---

## Self-Review

- **Spec coverage:** autentificare (JWT — Task 3-5), izolare strictă pe firmă/owner (Task 1, 6), un user nu accede datele altuia (verificat în Task 6 test + Task 7 manual). Acoperă cerința explicită a userului: facturi/extrase per user, fără amestec între useri.
- **Placeholder scan:** fără TBD; cod complet în fiecare pas.
- **Type consistency:** `CurrentUser.id()` definit în Task 4 și folosit în Task 6 guard + Task 6 `CompanyService.create`; `CompanyAccessGuard.assertOwner(Long)` definit și consumat în `CompanyService.get`; constructorul nou `CompanyService(repository, guard)` reflectat în testele actualizate.
- **Punct unic de control:** toate căile de citire (invoice list/delete, payroll employees/expenses, advisor context) trec prin `CompanyService.get`, care e singurul loc cu verificarea — ușor de auditat.
- **Dependență de ordine:** Plan 4 se execută DUPĂ Plan 2 și Plan 3 (modifică servicii din ambele). Notă: testul `CompanyServiceTest` din Plan 2 trebuie actualizat la noul constructor (semnalat în Task 6 Step 7).
