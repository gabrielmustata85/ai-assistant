# Plan 3 — Knowledge (legislație RAG) & Advisor (orchestrator)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ingestia documentelor de legislație în Pinecone (namespace dedicat) și orchestratorul advisor care strânge datele firmei + legislația relevantă, întreabă Claude (JSON structurat) și returnează estimări/termene/recomandări + date lipsă.

**Architecture:** Modulele `knowledge` și `advisor`. `knowledge` încarcă PDF-uri de legislație și le pune în Pinecone (`legislation` namespace), reutilizând extragerea+chunking din `DocumentIngestionService`. `advisor` orchestrează: `CompanyContextBuilder` (datele firmei → text) + căutare legislație + `AdvisorPromptBuilder` (prompt) → `ClaudeClient.ask` → `ClaudeResponse`, cu istoric salvat.

**Tech Stack:** Spring Boot 4, Pinecone, `ClaudeClient`/`EmbeddingClient` (Plan 1), domeniul (Plan 2), JUnit 5 + Mockito.

**Prerequisite:** Plan 1 și Plan 2 finalizate.

## Global Constraints

- Java **21**. Model Claude `claude-opus-4-8` (prin `ClaudeClient`, deja configurat).
- Sumele din răspuns sunt **orientative** — disclaimer-ul se injectează mereu în prompt și în `ClaudeResponse.disclaimer`.
- Namespace Pinecone pentru legislație: constanta `LEGISLATION_NAMESPACE = "legislation"`.
- Calculul îl face Claude (decizie de design); codul doar strânge datele și construiește prompt-ul.
- Testele unitare folosesc Mockito; `AdvisorPromptBuilder` e funcție pură, testată fără mock-uri.
- Comenzi: `./mvnw -q -Dtest=<ClassName> test`, build `./mvnw -q -DskipTests package`.

---

### Task 1: Migrația V2 (knowledge_document + ai_response_history)

**Files:**
- Create: `src/main/resources/db/migration/V2__knowledge_and_interactions.sql`
- Modify: `src/main/java/com/ai/assistant/model/AIResponseHistory.java`

**Interfaces:**
- Produces: tabelele `knowledge_document` și `ai_response_history` (cu `company_id` și `data_gaps` adăugate).

> Context: Plan 2 a comutat `ddl-auto=validate`, deci tabelul folosit de `AIResponseHistory` trebuie creat prin Flyway. Adăugăm și coloanele `company_id` + `data_gaps` din spec (`ai_interaction`).

- [ ] **Step 1: Scrie migrația**

```sql
-- V2__knowledge_and_interactions.sql

CREATE TABLE knowledge_document (
    id          BIGSERIAL PRIMARY KEY,
    title       VARCHAR(255) NOT NULL,
    source      VARCHAR(255),
    namespace   VARCHAR(64)  NOT NULL DEFAULT 'legislation',
    uploaded_at TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE TABLE ai_response_history (
    id                   BIGSERIAL PRIMARY KEY,
    session_id           VARCHAR(64) NOT NULL,
    company_id           BIGINT,
    user_query           TEXT        NOT NULL,
    ai_response          TEXT,
    data_gaps            TEXT,
    timestamp            TIMESTAMP   NOT NULL DEFAULT now(),
    was_corrected        BOOLEAN     DEFAULT FALSE,
    corrected_response   TEXT,
    correction_timestamp TIMESTAMP,
    embedding_vector     TEXT,
    metadata             JSON
);
CREATE INDEX idx_history_session ON ai_response_history(session_id);
```

- [ ] **Step 2: Adaugă câmpurile noi în entitate**

În `AIResponseHistory.java`, adaugă două câmpuri (înainte de `embeddingVector`):
```java
    @Column(name = "company_id")
    private Long companyId;

    @Column(name = "data_gaps", columnDefinition = "TEXT")
    private String dataGaps;
```

- [ ] **Step 3: Verifică build-ul**

Run: `./mvnw -q -DskipTests package`
Expected: BUILD SUCCESS.

- [ ] **Step 4: Commit**

```bash
git add src/main/resources/db/migration/V2__knowledge_and_interactions.sql \
        src/main/java/com/ai/assistant/model/AIResponseHistory.java
git commit -m "feat(db): V2 knowledge_document + ai_response_history"
```

---

### Task 2: Namespace legislație + metode Pinecone

**Files:**
- Modify: `src/main/java/com/ai/assistant/constants/NameSpaces.java`
- Modify: `src/main/java/com/ai/assistant/client/EnhancedPineconeClient.java`

**Interfaces:**
- Consumes: `EmbeddingClient.embed` (deja injectat în `EnhancedPineconeClient` din Plan 1), `super.searchEmbedding`, `super.upsertEmbedding`, `super.fetch`.
- Produces:
  - `NameSpaces.LEGISLATION_NAMESPACE = "legislation"`.
  - `List<String> EnhancedPineconeClient.searchLegislation(String query) throws IOException`
  - `void EnhancedPineconeClient.upsertLegislation(Vector vector, JSONObject metadata) throws IOException`
  - `String EnhancedPineconeClient.fetchLegislation(String id) throws IOException`

- [ ] **Step 1: Adaugă constanta de namespace**

În `NameSpaces.java`, adaugă constanta `LEGISLATION_NAMESPACE`:
```java
public static final String LEGISLATION_NAMESPACE = "legislation";
```
(Păstrează stilul existent al fișierului — dacă celelalte sunt `public static final String`, urmează-l identic.)

- [ ] **Step 2: Adaugă metodele în `EnhancedPineconeClient`**

Adaugă importul `import static com.ai.assistant.constants.NameSpaces.LEGISLATION_NAMESPACE;` și metodele:
```java
    /** Caută fragmente de legislație relevante pentru întrebare. */
    public List<String> searchLegislation(String query) throws IOException {
        List<Float> embedding = embeddingClient.embed(query);
        return super.searchEmbedding(embedding, LEGISLATION_NAMESPACE);
    }

    /** Salvează un chunk de legislație în Pinecone. */
    public void upsertLegislation(Vector vector, JSONObject metadata) throws IOException {
        super.upsertEmbedding(vector, metadata, LEGISLATION_NAMESPACE);
    }

    /** Recuperează textul unui fragment de legislație după id. */
    public String fetchLegislation(String id) throws IOException {
        return super.fetch(id, LEGISLATION_NAMESPACE);
    }
```

- [ ] **Step 3: Verifică build-ul**

Run: `./mvnw -q -DskipTests package`
Expected: BUILD SUCCESS. Dacă `embeddingClient` nu e numele câmpului (depinde de Plan 1 Task 6), folosește numele real al câmpului `EmbeddingClient` din clasă.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/ai/assistant/constants/NameSpaces.java \
        src/main/java/com/ai/assistant/client/EnhancedPineconeClient.java
git commit -m "feat(knowledge): legislation namespace + Pinecone methods"
```

---

### Task 3: Ingestie legislație + modulul `knowledge`

**Files:**
- Modify: `src/main/java/com/ai/assistant/service/DocumentIngestionService.java`
- Create: `src/main/java/com/ai/assistant/knowledge/KnowledgeDocument.java`
- Create: `src/main/java/com/ai/assistant/knowledge/KnowledgeDocumentRepository.java`
- Create: `src/main/java/com/ai/assistant/knowledge/KnowledgeService.java`
- Create: `src/main/java/com/ai/assistant/knowledge/KnowledgeController.java`

**Interfaces:**
- Consumes: `DocumentIngestionService` (extragere+chunking PDF), `EnhancedPineconeClient.upsertLegislation`, `EmbeddingClient.embed`.
- Produces:
  - `void DocumentIngestionService.ingestLegislationPdf(MultipartFile file) throws IOException` — extrage, chunk-uiește, embed-uiește și face upsert în namespace-ul de legislație.
  - `KnowledgeDocument` entitate + `KnowledgeDocumentRepository`.
  - `KnowledgeService.ingest(MultipartFile)` și `List<KnowledgeDocument> list()`.
  - Endpoint-uri `POST /knowledge/upload`, `GET /knowledge/documents`.

- [ ] **Step 1: Adaugă ingestia de legislație în `DocumentIngestionService`**

Adaugă o metodă publică care reutilizează helperele private existente (`extractTextFromPDF`, `chunkTextIntelligently`) și `embeddingClient.embed`, făcând upsert în namespace-ul de legislație printr-un nou helper privat:
```java
    /** Ingestă un PDF de legislație în namespace-ul de legislație (RAG). */
    public void ingestLegislationPdf(MultipartFile file) throws IOException {
        validatePdfFile(file);
        String text = extractTextFromPDF(file);
        List<String> chunks = chunkTextIntelligently(text);
        if (chunks.isEmpty()) {
            throw new IOException("No valid text chunks could be extracted");
        }
        for (int i = 0; i < chunks.size(); i++) {
            processLegislationChunk(file, chunks.get(i), i, chunks.size());
        }
    }

    private void processLegislationChunk(MultipartFile file, String chunk, int chunkIndex, int totalChunks)
            throws IOException {
        if (chunk == null || chunk.trim().isEmpty()) {
            return;
        }
        List<Float> embedding = embeddingClient.embed(chunk);
        JSONObject metadata = new JSONObject();
        metadata.put("documentId", file.getOriginalFilename());
        metadata.put("documentType", "LEGISLATION");
        metadata.put("chunkIndex", chunkIndex);
        metadata.put("totalChunks", totalChunks);
        metadata.put("textPreview", getTextPreview(chunk, TEXT_PREVIEW_LENGTH));
        metadata.put("uploadTimestamp", System.currentTimeMillis());

        Vector vector = new Vector(
                generatePdfChunkId("legis_" + file.getOriginalFilename(), chunkIndex),
                embedding);
        pineconeClient.upsertLegislation(vector, metadata);
    }
```
(Folosește numele real al câmpului `EmbeddingClient`/`EnhancedPineconeClient` din clasă — vezi Plan 1 Task 6. `getTextPreview`, `generatePdfChunkId`, `TEXT_PREVIEW_LENGTH`, `validatePdfFile` există deja.)

- [ ] **Step 2: Scrie entitatea și repository-ul**

`KnowledgeDocument.java`:
```java
package com.ai.assistant.knowledge;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "knowledge_document")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column
    private String source;

    @Column(nullable = false, length = 64)
    private String namespace = "legislation";

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt = LocalDateTime.now();
}
```

`KnowledgeDocumentRepository.java`:
```java
package com.ai.assistant.knowledge;

import org.springframework.data.jpa.repository.JpaRepository;

public interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocument, Long> {
}
```

- [ ] **Step 3: Scrie service-ul**

`KnowledgeService.java`:
```java
package com.ai.assistant.knowledge;

import com.ai.assistant.service.DocumentIngestionService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
public class KnowledgeService {

    private final DocumentIngestionService ingestionService;
    private final KnowledgeDocumentRepository repository;

    public KnowledgeService(DocumentIngestionService ingestionService,
                            KnowledgeDocumentRepository repository) {
        this.ingestionService = ingestionService;
        this.repository = repository;
    }

    public KnowledgeDocument ingest(MultipartFile file) throws IOException {
        ingestionService.ingestLegislationPdf(file);
        KnowledgeDocument doc = new KnowledgeDocument();
        doc.setTitle(file.getOriginalFilename());
        doc.setSource("upload");
        doc.setNamespace("legislation");
        return repository.save(doc);
    }

    public List<KnowledgeDocument> list() {
        return repository.findAll();
    }
}
```

- [ ] **Step 4: Scrie controllerul**

`KnowledgeController.java`:
```java
package com.ai.assistant.knowledge;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/knowledge")
public class KnowledgeController {

    private final KnowledgeService service;

    public KnowledgeController(KnowledgeService service) {
        this.service = service;
    }

    @PostMapping("/upload")
    public ResponseEntity<KnowledgeDocument> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty() || file.getOriginalFilename() == null
                || !file.getOriginalFilename().toLowerCase().endsWith(".pdf")) {
            return ResponseEntity.badRequest().build();
        }
        try {
            return ResponseEntity.ok(service.ingest(file));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/documents")
    public ResponseEntity<List<KnowledgeDocument>> documents() {
        return ResponseEntity.ok(service.list());
    }
}
```

- [ ] **Step 5: Verifică build + commit**

Run: `./mvnw -q -DskipTests package`
Expected: BUILD SUCCESS.
```bash
git add src/main/java/com/ai/assistant/service/DocumentIngestionService.java \
        src/main/java/com/ai/assistant/knowledge/
git commit -m "feat(knowledge): legislation ingestion + REST endpoints"
```

---

### Task 4: `CompanyContextBuilder` (datele firmei → text)

**Files:**
- Create: `src/main/java/com/ai/assistant/advisor/CompanyContextBuilder.java`
- Test: `src/test/java/com/ai/assistant/advisor/CompanyContextBuilderTest.java`

**Interfaces:**
- Consumes: `CompanyService.get`, `InvoiceService.listForCompany`, `PayrollService.employees`, `PayrollService.expenses`.
- Produces: `String CompanyContextBuilder.build(Long companyId)` — un bloc text cu firma (CUI, tip, regim, plătitor TVA), facturile, angajații și cheltuielile.

- [ ] **Step 1: Scrie testul care eșuează**

`CompanyContextBuilderTest.java`:
```java
package com.ai.assistant.advisor;

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

        String ctx = builder.build(1L);

        assertTrue(ctx.contains("RO123"));
        assertTrue(ctx.contains("MICRO_1"));
        assertTrue(ctx.contains("1190.00"));
        assertTrue(ctx.contains("Ion Pop"));
    }
}
```

- [ ] **Step 2: Rulează testul — trebuie să eșueze**

Run: `./mvnw -q -Dtest=CompanyContextBuilderTest test`
Expected: FAIL — `CompanyContextBuilder` nu există.

- [ ] **Step 3: Scrie builder-ul**

`CompanyContextBuilder.java`:
```java
package com.ai.assistant.advisor;

import com.ai.assistant.company.Company;
import com.ai.assistant.company.CompanyService;
import com.ai.assistant.invoicing.Invoice;
import com.ai.assistant.invoicing.InvoiceService;
import com.ai.assistant.payroll.Employee;
import com.ai.assistant.payroll.Expense;
import com.ai.assistant.payroll.PayrollService;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CompanyContextBuilder {

    private final CompanyService companyService;
    private final InvoiceService invoiceService;
    private final PayrollService payrollService;

    public CompanyContextBuilder(CompanyService companyService,
                                 InvoiceService invoiceService,
                                 PayrollService payrollService) {
        this.companyService = companyService;
        this.invoiceService = invoiceService;
        this.payrollService = payrollService;
    }

    public String build(Long companyId) {
        Company c = companyService.get(companyId);
        StringBuilder sb = new StringBuilder();

        sb.append("FIRMA:\n");
        sb.append("- CUI: ").append(c.getCui()).append("\n");
        sb.append("- Denumire: ").append(c.getName()).append("\n");
        sb.append("- Tip: ").append(c.getCompanyType()).append("\n");
        sb.append("- Regim fiscal: ").append(c.getTaxRegime()).append("\n");
        sb.append("- Plătitor TVA: ").append(c.isVatPayer() ? "DA" : "NU").append("\n\n");

        List<Invoice> invoices = invoiceService.listForCompany(companyId);
        sb.append("FACTURI (").append(invoices.size()).append("):\n");
        for (Invoice inv : invoices) {
            sb.append("- ").append(inv.getDirection())
              .append(" | ").append(inv.getIssueDate())
              .append(" | net ").append(inv.getNetAmount())
              .append(" | TVA ").append(inv.getVatAmount())
              .append(" | brut ").append(inv.getGrossAmount())
              .append(" | categorie ").append(inv.getCategory())
              .append(" | deductibil ").append(inv.isDeductible())
              .append("\n");
        }
        sb.append("\n");

        List<Employee> employees = payrollService.employees(companyId);
        sb.append("ANGAJAȚI (").append(employees.size()).append("):\n");
        for (Employee e : employees) {
            sb.append("- ").append(e.getFullName())
              .append(" | salariu brut ").append(e.getGrossSalary())
              .append(" | activ ").append(e.isActive())
              .append("\n");
        }
        sb.append("\n");

        List<Expense> expenses = payrollService.expenses(companyId);
        sb.append("ALTE CHELTUIELI (").append(expenses.size()).append("):\n");
        for (Expense x : expenses) {
            sb.append("- ").append(x.getDescription())
              .append(" | ").append(x.getCategory())
              .append(" | sumă ").append(x.getAmount())
              .append(" | deductibil ").append(x.isDeductible())
              .append("\n");
        }

        return sb.toString();
    }
}
```

- [ ] **Step 4: Rulează testul — trebuie să treacă**

Run: `./mvnw -q -Dtest=CompanyContextBuilderTest test`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/ai/assistant/advisor/CompanyContextBuilder.java \
        src/test/java/com/ai/assistant/advisor/CompanyContextBuilderTest.java
git commit -m "feat(advisor): company context builder"
```

---

### Task 5: `AdvisorPromptBuilder` (prompt pur, testabil)

**Files:**
- Create: `src/main/java/com/ai/assistant/advisor/AdvisorPromptBuilder.java`
- Test: `src/test/java/com/ai/assistant/advisor/AdvisorPromptBuilderTest.java`

**Interfaces:**
- Produces: `String AdvisorPromptBuilder.build(String question, String conversationContext, String companyContext, List<String> legislationSnippets)` — funcție pură, fără dependențe injectate.

- [ ] **Step 1: Scrie testul care eșuează**

`AdvisorPromptBuilderTest.java`:
```java
package com.ai.assistant.advisor;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AdvisorPromptBuilderTest {

    private final AdvisorPromptBuilder builder = new AdvisorPromptBuilder();

    @Test
    void promptContainsAllSectionsAndDisclaimer() {
        String prompt = builder.build(
                "Ce taxe am de plătit?",
                "User: salut",
                "FIRMA:\n- CUI: RO123\n",
                List.of("Cota impozit micro este 1%.", "Termen depunere: 25 ale lunii."));

        assertTrue(prompt.contains("Ce taxe am de plătit?"));
        assertTrue(prompt.contains("RO123"));
        assertTrue(prompt.contains("1%"));
        assertTrue(prompt.contains("orientativ"));      // disclaimer prezent
        assertTrue(prompt.toLowerCase().contains("data_gaps")); // cere date lipsă
    }

    @Test
    void handlesEmptyLegislationAndContext() {
        String prompt = builder.build("Întrebare", "", "FIRMA:\n", List.of());
        assertTrue(prompt.contains("Întrebare"));
    }
}
```

- [ ] **Step 2: Rulează testul — trebuie să eșueze**

Run: `./mvnw -q -Dtest=AdvisorPromptBuilderTest test`
Expected: FAIL — `AdvisorPromptBuilder` nu există.

- [ ] **Step 3: Scrie builder-ul**

`AdvisorPromptBuilder.java`:
```java
package com.ai.assistant.advisor;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AdvisorPromptBuilder {

    public String build(String question,
                        String conversationContext,
                        String companyContext,
                        List<String> legislationSnippets) {
        StringBuilder p = new StringBuilder();
        p.append("Ești un asistent fiscal pentru firme din România. ");
        p.append("Oferi sugestii și estimări ORIENTATIVE — nu înlocuiești contabilul. ");
        p.append("Folosește regulile de legislație de mai jos și datele firmei pentru a estima ");
        p.append("când și cât are de plătit, și pentru a recomanda optimizări de cheltuieli/taxe.\n");
        p.append("Dacă lipsesc date necesare (ex: salarii, cheltuieli), enumeră-le în câmpul data_gaps.\n\n");

        p.append("=== LEGISLAȚIE RELEVANTĂ ===\n");
        if (legislationSnippets == null || legislationSnippets.isEmpty()) {
            p.append("(niciun fragment găsit)\n");
        } else {
            for (String s : legislationSnippets) {
                p.append("- ").append(s).append("\n");
            }
        }
        p.append("\n");

        p.append("=== DATELE FIRMEI ===\n").append(companyContext).append("\n");

        if (conversationContext != null && !conversationContext.isBlank()) {
            p.append("=== CONVERSAȚIA ANTERIOARĂ ===\n").append(conversationContext).append("\n\n");
        }

        p.append("=== ÎNTREBAREA ===\n").append(question).append("\n\n");
        p.append("Răspunde STRICT în structura cerută (estimari, termene, recomandari, data_gaps, disclaimer). ");
        p.append("Disclaimer-ul trebuie să precizeze că sumele sunt orientative.");
        return p.toString();
    }
}
```

- [ ] **Step 4: Rulează testul — trebuie să treacă**

Run: `./mvnw -q -Dtest=AdvisorPromptBuilderTest test`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/ai/assistant/advisor/AdvisorPromptBuilder.java \
        src/test/java/com/ai/assistant/advisor/AdvisorPromptBuilderTest.java
git commit -m "feat(advisor): prompt builder"
```

---

### Task 6: `AdvisorService` (orchestrator)

**Files:**
- Create: `src/main/java/com/ai/assistant/advisor/AdvisorService.java`
- Test: `src/test/java/com/ai/assistant/advisor/AdvisorServiceTest.java`

**Interfaces:**
- Consumes: `CompanyContextBuilder.build`, `AdvisorPromptBuilder.build`, `EnhancedPineconeClient.searchLegislation` + `fetchLegislation`, `ConversationContext` (getFullContext/addMessage/clear), `ClaudeClient.ask`, `AIResponseHistoryService.logInteraction`.
- Produces:
  - `ClaudeResponse AdvisorService.ask(String sessionId, Long companyId, String question)`
  - `ClaudeResponse AdvisorService.obligations(Long companyId)` — întrebare standard „ce taxe am de plătit și când?".
  - `void AdvisorService.reset(String sessionId)`

- [ ] **Step 1: Scrie testul care eșuează (verifică orchestrarea)**

`AdvisorServiceTest.java`:
```java
package com.ai.assistant.advisor;

import com.ai.assistant.ai.ClaudeClient;
import com.ai.assistant.ai.ClaudeResponse;
import com.ai.assistant.client.EnhancedPineconeClient;
import com.ai.assistant.config.ConversationContext;
import com.ai.assistant.service.AIResponseHistoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdvisorServiceTest {

    @Mock CompanyContextBuilder contextBuilder;
    @Mock AdvisorPromptBuilder promptBuilder;
    @Mock EnhancedPineconeClient pineconeClient;
    @Mock ConversationContext conversationContext;
    @Mock ClaudeClient claudeClient;
    @Mock AIResponseHistoryService historyService;
    @InjectMocks AdvisorService service;

    @Test
    void askOrchestratesPipelineAndReturnsClaudeResponse() throws Exception {
        when(conversationContext.getFullContext("s1")).thenReturn("");
        when(contextBuilder.build(1L)).thenReturn("FIRMA:\n");
        when(pineconeClient.searchLegislation(anyString())).thenReturn(List.of("legis-1"));
        when(pineconeClient.fetchLegislation("legis-1")).thenReturn("Cota micro 1%");
        when(promptBuilder.build(anyString(), anyString(), anyString(), anyList()))
                .thenReturn("PROMPT");
        ClaudeResponse expected = new ClaudeResponse(List.of(), List.of(), List.of(), List.of(), "orientativ");
        when(claudeClient.ask("PROMPT")).thenReturn(expected);

        ClaudeResponse result = service.ask("s1", 1L, "Ce taxe am?");

        assertSame(expected, result);
        verify(pineconeClient).searchLegislation(anyString());
        verify(claudeClient).ask("PROMPT");
        verify(historyService).logInteraction(eq("s1"), eq("Ce taxe am?"), anyString());
        verify(conversationContext).addMessage(eq("s1"), contains("Ce taxe am?"));
    }
}
```

- [ ] **Step 2: Rulează testul — trebuie să eșueze**

Run: `./mvnw -q -Dtest=AdvisorServiceTest test`
Expected: FAIL — `AdvisorService` nu există.

- [ ] **Step 3: Scrie service-ul**

`AdvisorService.java`:
```java
package com.ai.assistant.advisor;

import com.ai.assistant.ai.ClaudeClient;
import com.ai.assistant.ai.ClaudeResponse;
import com.ai.assistant.client.EnhancedPineconeClient;
import com.ai.assistant.config.ConversationContext;
import com.ai.assistant.service.AIResponseHistoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class AdvisorService {

    private final CompanyContextBuilder contextBuilder;
    private final AdvisorPromptBuilder promptBuilder;
    private final EnhancedPineconeClient pineconeClient;
    private final ConversationContext conversationContext;
    private final ClaudeClient claudeClient;
    private final AIResponseHistoryService historyService;

    public AdvisorService(CompanyContextBuilder contextBuilder,
                          AdvisorPromptBuilder promptBuilder,
                          EnhancedPineconeClient pineconeClient,
                          ConversationContext conversationContext,
                          ClaudeClient claudeClient,
                          AIResponseHistoryService historyService) {
        this.contextBuilder = contextBuilder;
        this.promptBuilder = promptBuilder;
        this.pineconeClient = pineconeClient;
        this.conversationContext = conversationContext;
        this.claudeClient = claudeClient;
        this.historyService = historyService;
    }

    public ClaudeResponse ask(String sessionId, Long companyId, String question) {
        conversationContext.addMessage(sessionId, "User: " + question);
        String conversation = conversationContext.getFullContext(sessionId);
        String companyContext = contextBuilder.build(companyId);

        List<String> legislation = new ArrayList<>();
        try {
            for (String id : pineconeClient.searchLegislation(question)) {
                String text = pineconeClient.fetchLegislation(id);
                if (text != null && !text.isBlank()) {
                    legislation.add(text);
                }
            }
        } catch (IOException e) {
            log.warn("Căutarea legislației a eșuat: {}", e.getMessage());
        }

        String prompt = promptBuilder.build(question, conversation, companyContext, legislation);
        ClaudeResponse response = claudeClient.ask(prompt);

        conversationContext.addMessage(sessionId, "AI: " + summarize(response));
        historyService.logInteraction(sessionId, question, summarize(response));
        return response;
    }

    public ClaudeResponse obligations(Long companyId) {
        return ask("obligations-" + companyId, companyId,
                "Ce taxe am de plătit și până când? Estimează sumele și termenele.");
    }

    public void reset(String sessionId) {
        conversationContext.clear(sessionId);
    }

    private String summarize(ClaudeResponse r) {
        int est = r.estimari() == null ? 0 : r.estimari().size();
        int rec = r.recomandari() == null ? 0 : r.recomandari().size();
        return est + " estimări, " + rec + " recomandări";
    }
}
```

- [ ] **Step 4: Rulează testul — trebuie să treacă**

Run: `./mvnw -q -Dtest=AdvisorServiceTest test`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/ai/assistant/advisor/AdvisorService.java \
        src/test/java/com/ai/assistant/advisor/AdvisorServiceTest.java
git commit -m "feat(advisor): orchestrator service"
```

---

### Task 7: `AdvisorController` + retragerea vechiului `ChatController`

**Files:**
- Create: `src/main/java/com/ai/assistant/advisor/AdvisorController.java`
- Delete: `src/main/java/com/ai/assistant/controller/ChatController.java`

**Interfaces:**
- Consumes: `AdvisorService.ask/obligations/reset`, `KnowledgeService` (deja expus prin `KnowledgeController`).
- Produces: endpoint-urile `POST /advisor/ask`, `GET /advisor/obligations/{companyId}`, `POST /advisor/reset`.

> `ChatController` conținea fluxul vechi SQL/PDF cuplat la Copilot. Ingestia PDF de legislație e acum în `KnowledgeController`; întrebările trec prin `AdvisorController`. Îl ștergem.

- [ ] **Step 1: Scrie controllerul advisor**

`AdvisorController.java`:
```java
package com.ai.assistant.advisor;

import com.ai.assistant.ai.ClaudeResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/advisor")
public class AdvisorController {

    private final AdvisorService service;

    public AdvisorController(AdvisorService service) {
        this.service = service;
    }

    @PostMapping("/ask")
    public ResponseEntity<ClaudeResponse> ask(
            @RequestHeader("X-Session-ID") String sessionId,
            @RequestBody Map<String, Object> request) {
        Long companyId = Long.valueOf(String.valueOf(request.get("companyId")));
        String question = String.valueOf(request.get("question"));
        return ResponseEntity.ok(service.ask(sessionId, companyId, question));
    }

    @GetMapping("/obligations/{companyId}")
    public ResponseEntity<ClaudeResponse> obligations(@PathVariable Long companyId) {
        return ResponseEntity.ok(service.obligations(companyId));
    }

    @PostMapping("/reset")
    public ResponseEntity<Void> reset(@RequestHeader("X-Session-ID") String sessionId) {
        service.reset(sessionId);
        return ResponseEntity.noContent().build();
    }
}
```

- [ ] **Step 2: Șterge vechiul `ChatController`**

```bash
git rm src/main/java/com/ai/assistant/controller/ChatController.java
```

- [ ] **Step 3: Verifică build-ul complet**

Run: `./mvnw -q -DskipTests package`
Expected: BUILD SUCCESS. Dacă `ChatController` era singurul consumator al unor metode din `EnhancedPineconeClient` (ex. `searchPdfDocuments`/`searchSqlSchema`), acele metode pot rămâne nefolosite — e acceptabil; nu le șterge în acest plan.

- [ ] **Step 4: Rulează toate testele**

Run: `./mvnw -q test`
Expected: toate testele PASS (cele de rețea sunt SKIPPED dacă nu sunt setate cheile).

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat(advisor): REST endpoints; remove legacy ChatController"
```

---

## Self-Review

- **Spec coverage:** ingestia legislației + Pinecone (Task 2-3), `knowledge_document` și `ai_interaction`/`ai_response_history` (Task 1), orchestratorul advisor cu datele firmei + RAG + Claude JSON (Task 4-6), endpoint-urile `/advisor/ask|obligations|reset` și `/knowledge/upload|documents` (Task 3, 7), disclaimer orientativ injectat (Task 5), `data_gaps` în răspuns (Task 5 prompt + `ClaudeResponse` din Plan 1).
- **Placeholder scan:** fără TBD; cod complet.
- **Type consistency:** `ClaudeResponse` (Plan 1) folosit ca retur peste tot; `searchLegislation`/`fetchLegislation` definite în Task 2 și consumate în Task 6; `CompanyContextBuilder.build`/`AdvisorPromptBuilder.build` semnături identice între definire (Task 4/5) și consum (Task 6).
- **Dependență externă:** Task 3 reutilizează `DocumentIngestionService` (Plan 1 Task 6 i-a schimbat dependența pe `EmbeddingClient`); numele câmpurilor se confirmă la implementare.
- **Notă de risc:** `ClaudeClient.ask` (output structurat) depinde de numele de tip din SDK confirmat în Plan 1 Task 5; dacă acolo s-a ales fallback, advisor-ul rămâne neschimbat (consumă `ClaudeResponse`).
