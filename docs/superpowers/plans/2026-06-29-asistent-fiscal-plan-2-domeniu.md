# Plan 2 — Domeniu (Company, Invoice, Employee, Expense)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Modelul de date și CRUD-ul REST pentru firmă, facturi, angajați și cheltuieli — datele brute pe care le va folosi advisor-ul în Plan 3.

**Architecture:** Module de domeniu sub `com.ai.assistant`: `company`, `invoicing`, `payroll`. Fiecare cu entitate JPA + repository Spring Data + service + controller REST. Migrări Flyway ca sursă de adevăr a schemei. Izolare pe `companyId` (fiecare resursă aparține unei firme).

**Tech Stack:** Spring Boot 4, Spring Data JPA, Flyway (PostgreSQL), Lombok, JUnit 5 + Mockito.

**Prerequisite:** Plan 1 finalizat (clienții AI există; build-ul trece).

## Global Constraints

- Java **21**. Pachet rădăcină `com.ai.assistant`.
- Migrările Flyway în `src/main/resources/db/migration`, denumite `V<N>__descriere.sql`. Schema se schimbă DOAR prin migrări.
- În `application.properties`: `spring.jpa.hibernate.ddl-auto=validate` (NU `update`).
- Sume monetare: `BigDecimal` (niciodată `double` pentru bani).
- Toate entitățile de domeniu au `company_id` și se filtrează pe el. Endpoint-urile nested sunt sub `/companies/{companyId}/...`.
- Testele unitare folosesc Mockito (mock pe repository); NU ating un DB real.
- Comenzi: `./mvnw -q -Dtest=<ClassName> test`, build `./mvnw -q -DskipTests package`.

---

### Task 1: Migrația Flyway V1 + comutare pe `validate`

**Files:**
- Create: `src/main/resources/db/migration/V1__core_domain.sql`
- Modify: `src/main/resources/application.properties`

**Interfaces:**
- Produces: tabelele `company`, `invoice`, `employee`, `expense` în Postgres.

- [ ] **Step 1: Scrie migrația**

```sql
-- V1__core_domain.sql

CREATE TABLE company (
    id            BIGSERIAL PRIMARY KEY,
    cui           VARCHAR(32)  NOT NULL UNIQUE,
    name          VARCHAR(255) NOT NULL,
    company_type  VARCHAR(32)  NOT NULL,   -- SRL, PFA, II
    tax_regime    VARCHAR(32)  NOT NULL,   -- MICRO_1, MICRO_3, PROFIT_16, PFA
    vat_payer     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE TABLE invoice (
    id             BIGSERIAL PRIMARY KEY,
    company_id     BIGINT        NOT NULL REFERENCES company(id) ON DELETE CASCADE,
    direction      VARCHAR(16)   NOT NULL,  -- ISSUED, RECEIVED
    invoice_number VARCHAR(64),
    partner_name   VARCHAR(255),
    partner_cui    VARCHAR(32),
    issue_date     DATE          NOT NULL,
    due_date       DATE,
    net_amount     NUMERIC(15,2) NOT NULL,
    vat_amount     NUMERIC(15,2) NOT NULL DEFAULT 0,
    gross_amount   NUMERIC(15,2) NOT NULL,
    category       VARCHAR(64),
    deductible     BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMP     NOT NULL DEFAULT now()
);
CREATE INDEX idx_invoice_company ON invoice(company_id);

CREATE TABLE employee (
    id            BIGSERIAL PRIMARY KEY,
    company_id    BIGINT        NOT NULL REFERENCES company(id) ON DELETE CASCADE,
    full_name     VARCHAR(255)  NOT NULL,
    cnp           VARCHAR(13),
    gross_salary  NUMERIC(15,2) NOT NULL,
    position      VARCHAR(128),
    start_date    DATE,
    active        BOOLEAN       NOT NULL DEFAULT TRUE
);
CREATE INDEX idx_employee_company ON employee(company_id);

CREATE TABLE expense (
    id           BIGSERIAL PRIMARY KEY,
    company_id   BIGINT        NOT NULL REFERENCES company(id) ON DELETE CASCADE,
    description  VARCHAR(255),
    category     VARCHAR(64),
    amount       NUMERIC(15,2) NOT NULL,
    expense_date DATE          NOT NULL,
    deductible   BOOLEAN       NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_expense_company ON expense(company_id);
```

- [ ] **Step 2: Comută `ddl-auto` pe validate**

În `application.properties`, înlocuiește `spring.jpa.hibernate.ddl-auto=update` cu `spring.jpa.hibernate.ddl-auto=validate`.

- [ ] **Step 3: Verifică build-ul**

Run: `./mvnw -q -DskipTests package`
Expected: BUILD SUCCESS. (Aplicarea migrației se face la pornirea cu DB; nu o cerem aici.)

- [ ] **Step 4: Commit**

```bash
git add src/main/resources/db/migration/V1__core_domain.sql src/main/resources/application.properties
git commit -m "feat(db): V1 core domain schema; ddl-auto=validate"
```

---

### Task 2: Modulul `company` (entitate + enum-uri + repo + service + controller)

**Files:**
- Create: `src/main/java/com/ai/assistant/company/CompanyType.java`
- Create: `src/main/java/com/ai/assistant/company/TaxRegime.java`
- Create: `src/main/java/com/ai/assistant/company/Company.java`
- Create: `src/main/java/com/ai/assistant/company/CompanyRepository.java`
- Create: `src/main/java/com/ai/assistant/company/CompanyService.java`
- Create: `src/main/java/com/ai/assistant/company/CompanyController.java`
- Test: `src/test/java/com/ai/assistant/company/CompanyServiceTest.java`

**Interfaces:**
- Produces:
  - `enum CompanyType { SRL, PFA, II }`
  - `enum TaxRegime { MICRO_1, MICRO_3, PROFIT_16, PFA }`
  - `Company` entitate JPA (getters Lombok).
  - `interface CompanyRepository extends JpaRepository<Company, Long>`
  - `CompanyService` cu `Company create(Company c)`, `Company get(Long id)`, `Company update(Long id, Company patch)`.
  - `CompanyNotFoundException extends RuntimeException` (creată inline în acest task).

- [ ] **Step 1: Scrie enum-urile și entitatea**

`CompanyType.java`:
```java
package com.ai.assistant.company;

public enum CompanyType { SRL, PFA, II }
```

`TaxRegime.java`:
```java
package com.ai.assistant.company;

public enum TaxRegime { MICRO_1, MICRO_3, PROFIT_16, PFA }
```

`Company.java`:
```java
package com.ai.assistant.company;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "company")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String cui;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "company_type", nullable = false, length = 32)
    private CompanyType companyType;

    @Enumerated(EnumType.STRING)
    @Column(name = "tax_regime", nullable = false, length = 32)
    private TaxRegime taxRegime;

    @Column(name = "vat_payer", nullable = false)
    private boolean vatPayer;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
```

- [ ] **Step 2: Scrie repository și excepția**

`CompanyRepository.java`:
```java
package com.ai.assistant.company;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository extends JpaRepository<Company, Long> {
}
```

`CompanyNotFoundException.java` (în același pachet):
```java
package com.ai.assistant.company;

public class CompanyNotFoundException extends RuntimeException {
    public CompanyNotFoundException(Long id) {
        super("Company not found: " + id);
    }
}
```

- [ ] **Step 3: Scrie testul de service care eșuează**

`CompanyServiceTest.java`:
```java
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
```

- [ ] **Step 4: Rulează testul — trebuie să eșueze**

Run: `./mvnw -q -Dtest=CompanyServiceTest test`
Expected: FAIL — `CompanyService` nu există încă.

- [ ] **Step 5: Scrie service-ul**

`CompanyService.java`:
```java
package com.ai.assistant.company;

import org.springframework.stereotype.Service;

@Service
public class CompanyService {

    private final CompanyRepository repository;

    public CompanyService(CompanyRepository repository) {
        this.repository = repository;
    }

    public Company create(Company company) {
        company.setId(null);
        return repository.save(company);
    }

    public Company get(Long id) {
        return repository.findById(id).orElseThrow(() -> new CompanyNotFoundException(id));
    }

    /** Aplică pe entitatea existentă doar câmpurile non-null din patch. */
    public Company update(Long id, Company patch) {
        Company existing = get(id);
        if (patch.getName() != null) existing.setName(patch.getName());
        if (patch.getCui() != null) existing.setCui(patch.getCui());
        if (patch.getCompanyType() != null) existing.setCompanyType(patch.getCompanyType());
        if (patch.getTaxRegime() != null) existing.setTaxRegime(patch.getTaxRegime());
        existing.setVatPayer(patch.isVatPayer());
        return repository.save(existing);
    }
}
```

- [ ] **Step 6: Rulează testul — trebuie să treacă**

Run: `./mvnw -q -Dtest=CompanyServiceTest test`
Expected: PASS.

- [ ] **Step 7: Scrie controllerul**

`CompanyController.java`:
```java
package com.ai.assistant.company;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/companies")
public class CompanyController {

    private final CompanyService service;

    public CompanyController(CompanyService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<Company> create(@RequestBody Company company) {
        return ResponseEntity.ok(service.create(company));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Company> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.get(id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Company> update(@PathVariable Long id, @RequestBody Company patch) {
        return ResponseEntity.ok(service.update(id, patch));
    }

    @ExceptionHandler(CompanyNotFoundException.class)
    public ResponseEntity<String> notFound(CompanyNotFoundException e) {
        return ResponseEntity.status(404).body(e.getMessage());
    }
}
```

- [ ] **Step 8: Verifică build + commit**

Run: `./mvnw -q -DskipTests package`
Expected: BUILD SUCCESS.
```bash
git add src/main/java/com/ai/assistant/company/ src/test/java/com/ai/assistant/company/
git commit -m "feat(company): entity, repo, service, controller"
```

---

### Task 3: Modulul `invoicing`

**Files:**
- Create: `src/main/java/com/ai/assistant/invoicing/Direction.java`
- Create: `src/main/java/com/ai/assistant/invoicing/Invoice.java`
- Create: `src/main/java/com/ai/assistant/invoicing/InvoiceRepository.java`
- Create: `src/main/java/com/ai/assistant/invoicing/InvoiceService.java`
- Create: `src/main/java/com/ai/assistant/invoicing/InvoiceController.java`
- Test: `src/test/java/com/ai/assistant/invoicing/InvoiceServiceTest.java`

**Interfaces:**
- Consumes: `CompanyService.get(Long)` (validează că firma există înainte de a adăuga factura).
- Produces:
  - `enum Direction { ISSUED, RECEIVED }`
  - `Invoice` entitate JPA.
  - `InvoiceRepository extends JpaRepository<Invoice, Long>` cu `List<Invoice> findByCompanyId(Long companyId)`.
  - `InvoiceService` cu `Invoice add(Long companyId, Invoice inv)`, `List<Invoice> listForCompany(Long companyId)`, `void delete(Long id)`.

- [ ] **Step 1: Scrie enum-ul și entitatea**

`Direction.java`:
```java
package com.ai.assistant.invoicing;

public enum Direction { ISSUED, RECEIVED }
```

`Invoice.java`:
```java
package com.ai.assistant.invoicing;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "invoice")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Direction direction;

    @Column(name = "invoice_number", length = 64)
    private String invoiceNumber;

    @Column(name = "partner_name")
    private String partnerName;

    @Column(name = "partner_cui", length = 32)
    private String partnerCui;

    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "net_amount", nullable = false)
    private BigDecimal netAmount;

    @Column(name = "vat_amount", nullable = false)
    private BigDecimal vatAmount = BigDecimal.ZERO;

    @Column(name = "gross_amount", nullable = false)
    private BigDecimal grossAmount;

    @Column(length = 64)
    private String category;

    @Column(nullable = false)
    private boolean deductible;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
```

- [ ] **Step 2: Scrie repository-ul**

`InvoiceRepository.java`:
```java
package com.ai.assistant.invoicing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    List<Invoice> findByCompanyId(Long companyId);
}
```

- [ ] **Step 3: Scrie testul care eșuează**

`InvoiceServiceTest.java`:
```java
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
```

- [ ] **Step 4: Rulează testul — trebuie să eșueze**

Run: `./mvnw -q -Dtest=InvoiceServiceTest test`
Expected: FAIL — `InvoiceService` nu există.

- [ ] **Step 5: Scrie service-ul**

`InvoiceService.java`:
```java
package com.ai.assistant.invoicing;

import com.ai.assistant.company.CompanyService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InvoiceService {

    private final InvoiceRepository repository;
    private final CompanyService companyService;

    public InvoiceService(InvoiceRepository repository, CompanyService companyService) {
        this.repository = repository;
        this.companyService = companyService;
    }

    public Invoice add(Long companyId, Invoice invoice) {
        companyService.get(companyId);   // aruncă dacă firma nu există
        invoice.setId(null);
        invoice.setCompanyId(companyId);
        return repository.save(invoice);
    }

    public List<Invoice> listForCompany(Long companyId) {
        return repository.findByCompanyId(companyId);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}
```

- [ ] **Step 6: Rulează testul — trebuie să treacă**

Run: `./mvnw -q -Dtest=InvoiceServiceTest test`
Expected: PASS.

- [ ] **Step 7: Scrie controllerul**

`InvoiceController.java`:
```java
package com.ai.assistant.invoicing;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class InvoiceController {

    private final InvoiceService service;

    public InvoiceController(InvoiceService service) {
        this.service = service;
    }

    @PostMapping("/companies/{companyId}/invoices")
    public ResponseEntity<Invoice> add(@PathVariable Long companyId, @RequestBody Invoice invoice) {
        return ResponseEntity.ok(service.add(companyId, invoice));
    }

    @GetMapping("/companies/{companyId}/invoices")
    public ResponseEntity<List<Invoice>> list(@PathVariable Long companyId) {
        return ResponseEntity.ok(service.listForCompany(companyId));
    }

    @DeleteMapping("/invoices/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

- [ ] **Step 8: Verifică build + commit**

Run: `./mvnw -q -DskipTests package`
Expected: BUILD SUCCESS.
```bash
git add src/main/java/com/ai/assistant/invoicing/ src/test/java/com/ai/assistant/invoicing/
git commit -m "feat(invoicing): invoice entity, repo, service, controller"
```

---

### Task 4: Modulul `payroll` (Employee + Expense)

**Files:**
- Create: `src/main/java/com/ai/assistant/payroll/Employee.java`
- Create: `src/main/java/com/ai/assistant/payroll/EmployeeRepository.java`
- Create: `src/main/java/com/ai/assistant/payroll/Expense.java`
- Create: `src/main/java/com/ai/assistant/payroll/ExpenseRepository.java`
- Create: `src/main/java/com/ai/assistant/payroll/PayrollService.java`
- Create: `src/main/java/com/ai/assistant/payroll/PayrollController.java`
- Test: `src/test/java/com/ai/assistant/payroll/PayrollServiceTest.java`

**Interfaces:**
- Consumes: `CompanyService.get(Long)`.
- Produces:
  - `Employee`, `Expense` entități JPA.
  - `EmployeeRepository` cu `List<Employee> findByCompanyId(Long)`; `ExpenseRepository` cu `List<Expense> findByCompanyId(Long)`.
  - `PayrollService` cu `Employee addEmployee(Long companyId, Employee e)`, `List<Employee> employees(Long companyId)`, `Expense addExpense(Long companyId, Expense x)`, `List<Expense> expenses(Long companyId)`.

- [ ] **Step 1: Scrie entitățile**

`Employee.java`:
```java
package com.ai.assistant.payroll;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "employee")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(length = 13)
    private String cnp;

    @Column(name = "gross_salary", nullable = false)
    private BigDecimal grossSalary;

    @Column(length = 128)
    private String position;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(nullable = false)
    private boolean active = true;
}
```

`Expense.java`:
```java
package com.ai.assistant.payroll;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "expense")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column
    private String description;

    @Column(length = 64)
    private String category;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(name = "expense_date", nullable = false)
    private LocalDate expenseDate;

    @Column(nullable = false)
    private boolean deductible;
}
```

- [ ] **Step 2: Scrie repository-urile**

`EmployeeRepository.java`:
```java
package com.ai.assistant.payroll;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    List<Employee> findByCompanyId(Long companyId);
}
```

`ExpenseRepository.java`:
```java
package com.ai.assistant.payroll;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findByCompanyId(Long companyId);
}
```

- [ ] **Step 3: Scrie testul care eșuează**

`PayrollServiceTest.java`:
```java
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
}
```

- [ ] **Step 4: Rulează testul — trebuie să eșueze**

Run: `./mvnw -q -Dtest=PayrollServiceTest test`
Expected: FAIL — `PayrollService` nu există.

- [ ] **Step 5: Scrie service-ul**

`PayrollService.java`:
```java
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
```

- [ ] **Step 6: Rulează testul — trebuie să treacă**

Run: `./mvnw -q -Dtest=PayrollServiceTest test`
Expected: PASS.

- [ ] **Step 7: Scrie controllerul**

`PayrollController.java`:
```java
package com.ai.assistant.payroll;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class PayrollController {

    private final PayrollService service;

    public PayrollController(PayrollService service) {
        this.service = service;
    }

    @PostMapping("/companies/{companyId}/employees")
    public ResponseEntity<Employee> addEmployee(@PathVariable Long companyId, @RequestBody Employee employee) {
        return ResponseEntity.ok(service.addEmployee(companyId, employee));
    }

    @GetMapping("/companies/{companyId}/employees")
    public ResponseEntity<List<Employee>> employees(@PathVariable Long companyId) {
        return ResponseEntity.ok(service.employees(companyId));
    }

    @PostMapping("/companies/{companyId}/expenses")
    public ResponseEntity<Expense> addExpense(@PathVariable Long companyId, @RequestBody Expense expense) {
        return ResponseEntity.ok(service.addExpense(companyId, expense));
    }

    @GetMapping("/companies/{companyId}/expenses")
    public ResponseEntity<List<Expense>> expenses(@PathVariable Long companyId) {
        return ResponseEntity.ok(service.expenses(companyId));
    }
}
```

- [ ] **Step 8: Verifică build + commit**

Run: `./mvnw -q -DskipTests package`
Expected: BUILD SUCCESS.
```bash
git add src/main/java/com/ai/assistant/payroll/ src/test/java/com/ai/assistant/payroll/
git commit -m "feat(payroll): employee + expense entities, service, controller"
```

---

## Self-Review

- **Spec coverage:** acoperă modelul de date (company, invoice, employee, expense — Task 1-4) și endpoint-urile CRUD din spec (`/companies`, `/companies/{id}/invoices`, `/employees`, `/expenses`). `knowledge_document`, `ai_interaction` și `/advisor` sunt în Plan 3.
- **Placeholder scan:** fără TBD; cod complet în fiecare pas.
- **Type consistency:** `CompanyService.get(Long)` definit în Task 2 și consumat în Task 3/4; `findByCompanyId` consistent peste invoice/employee/expense; sume în `BigDecimal` peste tot.
- **Izolare:** fiecare service setează `companyId` din path și validează firma prin `companyService.get()` — niciun consumator nu acceptă `companyId` din body.
