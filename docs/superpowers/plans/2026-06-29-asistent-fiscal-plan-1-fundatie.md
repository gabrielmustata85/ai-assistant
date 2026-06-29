# Plan 1 — Fundație & Clienți AI (Claude + Voyage)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Scoatem GitHub Copilot și Vaadin; adăugăm un client Claude (răspunsuri + JSON structurat) și un client de embeddings Voyage, izolate în spatele unor interfețe, și le legăm la fluxul RAG existent (Pinecone).

**Architecture:** Spring Boot 4 monolit. `ai` devine modulul de integrare AI: o interfață `EmbeddingClient` (impl. Voyage prin OkHttp) și un `ClaudeClient` (SDK oficial `anthropic-java`). `DocumentIngestionService` și `EnhancedPineconeClient` consumă `EmbeddingClient` în loc de `CopilotClient`. `CopilotClient` se șterge.

**Tech Stack:** Java 21, Spring Boot 4.0.2, `com.anthropic:anthropic-java:2.34.0`, OkHttp 4.9.x, Voyage AI embeddings API, Pinecone, PostgreSQL, JUnit 5 (spring-boot-starter-test).

## Global Constraints

- Java version: **21** (din `pom.xml`).
- Model Claude: **`claude-opus-4-8`** exact (folosit ca `Model.CLAUDE_OPUS_4_8`). Nu adăuga sufixe de dată.
- Thinking: **adaptiv** (`ThinkingConfigAdaptive`). NU folosi `budget_tokens` (dă 400 pe Opus 4.8).
- `max_tokens` non-streaming: **16000**.
- Cheile API se citesc DOAR din variabile de mediu: `ANTHROPIC_API_KEY`, `VOYAGE_API_KEY`, `PINECONE_API_KEY`. Niciun secret în `application.properties`.
- Embedding model Voyage: **`voyage-3.5`**, dimensiune **1024**. Indexul Pinecone trebuie să aibă dimension=1024.
- Comenzi de test: `./mvnw -q -Dtest=<ClassName> test`. Build: `./mvnw -q -DskipTests package`.
- Testele unitare NU apelează API-uri reale. Testele care ating rețeaua se adnotează `@EnabledIfEnvironmentVariable` și se exclud din build-ul normal.

---

### Task 1: Curăță `pom.xml` — scoate Vaadin, adaugă Anthropic SDK

**Files:**
- Modify: `pom.xml`

**Interfaces:**
- Consumes: nimic.
- Produces: dependența `com.anthropic:anthropic-java` disponibilă pe classpath; Vaadin eliminat.

- [ ] **Step 1: Scoate dependențele și plugin-ul Vaadin, adaugă SDK Anthropic**

În `pom.xml`, șterge cele trei blocuri Vaadin din `<dependencies>`:
```xml
<dependency>
    <groupId>com.vaadin</groupId>
    <artifactId>vaadin-dev</artifactId>
    <optional>true</optional>
</dependency>
<dependency>
    <groupId>com.vaadin</groupId>
    <artifactId>vaadin-spring-boot-starter</artifactId>
</dependency>
```
Șterge din `<dependencyManagement>` blocul `vaadin-bom`. Șterge din `<build><plugins>` blocul `vaadin-maven-plugin`. Șterge proprietatea `<vaadin.version>25.0.4</vaadin.version>`.

Adaugă în `<dependencies>` SDK-ul Anthropic:
```xml
<dependency>
    <groupId>com.anthropic</groupId>
    <artifactId>anthropic-java</artifactId>
    <version>2.34.0</version>
</dependency>
```

- [ ] **Step 2: Verifică build-ul (compilează încă cu Copilot prezent)**

Run: `./mvnw -q -DskipTests package`
Expected: BUILD SUCCESS. (Codul Copilot încă există; îl scoatem în Task 6.)

- [ ] **Step 3: Commit**

```bash
git add pom.xml
git commit -m "build: drop Vaadin, add anthropic-java SDK"
```

---

### Task 2: Interfața `EmbeddingClient`

**Files:**
- Create: `src/main/java/com/ai/assistant/ai/EmbeddingClient.java`

**Interfaces:**
- Produces: `interface EmbeddingClient { List<Float> embed(String text) throws IOException; int dimension(); }`

- [ ] **Step 1: Scrie interfața**

```java
package com.ai.assistant.ai;

import java.io.IOException;
import java.util.List;

/**
 * Abstracție peste furnizorul de embeddings. Claude nu are API de embeddings,
 * deci folosim Voyage; interfața izolează providerul ca să fie ușor de schimbat.
 */
public interface EmbeddingClient {

    /** Returnează vectorul de embedding pentru textul dat. */
    List<Float> embed(String text) throws IOException;

    /** Dimensiunea vectorilor produși (trebuie să fie egală cu dimensiunea indexului Pinecone). */
    int dimension();
}
```

- [ ] **Step 2: Verifică compilarea**

Run: `./mvnw -q -DskipTests compile`
Expected: BUILD SUCCESS.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/ai/assistant/ai/EmbeddingClient.java
git commit -m "feat(ai): add EmbeddingClient interface"
```

---

### Task 3: `VoyageEmbeddingClient` (implementare prin OkHttp)

**Files:**
- Create: `src/main/java/com/ai/assistant/ai/VoyageEmbeddingClient.java`
- Test: `src/test/java/com/ai/assistant/ai/VoyageEmbeddingClientTest.java`

**Interfaces:**
- Consumes: `EmbeddingClient`.
- Produces: `@Service VoyageEmbeddingClient implements EmbeddingClient`. Constructor `VoyageEmbeddingClient(@Value("${voyage.api.key}") String apiKey)`. Metodă pachet-privată `List<Float> parseEmbedding(String responseJson)` pentru testare fără rețea.

- [ ] **Step 1: Scrie testul care eșuează (parsarea răspunsului Voyage)**

```java
package com.ai.assistant.ai;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VoyageEmbeddingClientTest {

    @Test
    void parsesEmbeddingFromVoyageResponse() {
        VoyageEmbeddingClient client = new VoyageEmbeddingClient("test-key");
        String json = "{\"object\":\"list\",\"data\":[{\"object\":\"embedding\","
                + "\"embedding\":[0.1,0.2,0.3],\"index\":0}],\"model\":\"voyage-3.5\"}";

        List<Float> embedding = client.parseEmbedding(json);

        assertEquals(3, embedding.size());
        assertEquals(0.1f, embedding.get(0), 1e-6);
        assertEquals(0.3f, embedding.get(2), 1e-6);
    }
}
```

- [ ] **Step 2: Rulează testul — trebuie să eșueze**

Run: `./mvnw -q -Dtest=VoyageEmbeddingClientTest test`
Expected: FAIL — `VoyageEmbeddingClient` nu există încă (compile error / cannot find symbol).

- [ ] **Step 3: Scrie implementarea minimă**

```java
package com.ai.assistant.ai;

import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class VoyageEmbeddingClient implements EmbeddingClient {

    private static final String VOYAGE_URL = "https://api.voyageai.com/v1/embeddings";
    private static final String MODEL = "voyage-3.5";
    private static final int DIMENSION = 1024;

    private final String apiKey;
    private final OkHttpClient client;

    public VoyageEmbeddingClient(@Value("${voyage.api.key}") String apiKey) {
        this.apiKey = apiKey;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public List<Float> embed(String text) throws IOException {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Text cannot be null or empty");
        }

        JSONObject body = new JSONObject()
                .put("model", MODEL)
                .put("input", new JSONArray().put(text));

        Request request = new Request.Builder()
                .url(VOYAGE_URL)
                .post(RequestBody.create(body.toString(), MediaType.get("application/json")))
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            ResponseBody respBody = response.body();
            String payload = respBody != null ? respBody.string() : "";
            if (!response.isSuccessful()) {
                throw new IOException("Voyage request failed with code "
                        + response.code() + ": " + payload);
            }
            return parseEmbedding(payload);
        }
    }

    @Override
    public int dimension() {
        return DIMENSION;
    }

    List<Float> parseEmbedding(String responseJson) {
        JSONObject json = new JSONObject(responseJson);
        JSONArray data = json.getJSONArray("data");
        if (data.isEmpty()) {
            throw new IllegalStateException("No embedding data in Voyage response");
        }
        JSONArray arr = data.getJSONObject(0).getJSONArray("embedding");
        List<Float> embedding = new ArrayList<>(arr.length());
        for (int i = 0; i < arr.length(); i++) {
            embedding.add((float) arr.getDouble(i));
        }
        return embedding;
    }
}
```

- [ ] **Step 4: Rulează testul — trebuie să treacă**

Run: `./mvnw -q -Dtest=VoyageEmbeddingClientTest test`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/ai/assistant/ai/VoyageEmbeddingClient.java \
        src/test/java/com/ai/assistant/ai/VoyageEmbeddingClientTest.java
git commit -m "feat(ai): add Voyage embedding client"
```

---

### Task 4: `ClaudeResponse` (record pentru răspunsul structurat JSON)

**Files:**
- Create: `src/main/java/com/ai/assistant/ai/ClaudeResponse.java`

**Interfaces:**
- Produces: `record ClaudeResponse(List<Estimare> estimari, List<Termen> termene, List<Recomandare> recomandari, List<String> dataGaps, String disclaimer)` cu sub-record-urile `Estimare`, `Termen`, `Recomandare`. Folosit ca schema de output structurat al lui Claude în Plan 3 și ca tip de retur al `ClaudeClient.ask(...)`.

- [ ] **Step 1: Scrie record-ul**

```java
package com.ai.assistant.ai;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

/** Răspunsul structurat al asistentului fiscal. Servește și ca schemă pentru output structurat Claude. */
public record ClaudeResponse(
        @JsonPropertyDescription("Estimări de taxe de plată") List<Estimare> estimari,
        @JsonPropertyDescription("Termene/scadențe viitoare") List<Termen> termene,
        @JsonPropertyDescription("Recomandări de optimizare") List<Recomandare> recomandari,
        @JsonPropertyDescription("Date lipsă pe care asistentul trebuie să le ceară userului") List<String> dataGaps,
        @JsonPropertyDescription("Disclaimer: sumele sunt orientative") String disclaimer) {

    public record Estimare(String tipTaxa, double suma, String perioada, String explicatie) {}

    public record Termen(String obligatie, String scadenta) {}

    public record Recomandare(String text, String impactEstimat) {}
}
```

- [ ] **Step 2: Verifică compilarea**

Run: `./mvnw -q -DskipTests compile`
Expected: BUILD SUCCESS.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/ai/assistant/ai/ClaudeResponse.java
git commit -m "feat(ai): add ClaudeResponse structured DTO"
```

---

### Task 5: `ClaudeClient` (SDK anthropic-java)

**Files:**
- Create: `src/main/java/com/ai/assistant/ai/ClaudeClient.java`
- Test: `src/test/java/com/ai/assistant/ai/ClaudeClientNetworkTest.java`

**Interfaces:**
- Consumes: `ClaudeResponse`, SDK `com.anthropic.*`.
- Produces: `@Service ClaudeClient` cu:
  - `String generateText(String prompt)` — răspuns text liber (folosit ca înlocuitor 1:1 al `CopilotClient.generateResponse`).
  - `ClaudeResponse ask(String prompt)` — răspuns structurat JSON (folosit de advisor în Plan 3).
  - Constructor fără argumente care folosește `AnthropicOkHttpClient.fromEnv()` (citește `ANTHROPIC_API_KEY`).

- [ ] **Step 1: Scrie testul de rețea (gated pe env var)**

```java
package com.ai.assistant.ai;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfEnvironmentVariable(named = "ANTHROPIC_API_KEY", matches = ".+")
class ClaudeClientNetworkTest {

    @Test
    void generatesTextResponse() {
        ClaudeClient client = new ClaudeClient();
        String reply = client.generateText("Spune exact cuvântul: salut");
        assertNotNull(reply);
        assertTrue(reply.toLowerCase().contains("salut"));
    }
}
```

- [ ] **Step 2: Rulează testul — eșuează la compilare (clasa lipsește)**

Run: `./mvnw -q -Dtest=ClaudeClientNetworkTest test`
Expected: FAIL — `ClaudeClient` cannot be found. (Dacă `ANTHROPIC_API_KEY` nu e setat, testul oricum nu rulează, dar compilarea TREBUIE să eșueze acum.)

- [ ] **Step 3: Scrie implementarea**

```java
package com.ai.assistant.ai;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.Model;
import com.anthropic.models.messages.StructuredMessageCreateParams;
import com.anthropic.models.messages.ThinkingConfigAdaptive;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Slf4j
@Service
public class ClaudeClient {

    private static final long MAX_TOKENS = 16000L;

    private final AnthropicClient client;

    public ClaudeClient() {
        // Citește ANTHROPIC_API_KEY din mediu.
        this.client = AnthropicOkHttpClient.fromEnv();
    }

    /** Răspuns text liber (înlocuiește CopilotClient.generateResponse). */
    public String generateText(String prompt) {
        MessageCreateParams params = MessageCreateParams.builder()
                .model(Model.CLAUDE_OPUS_4_8)
                .maxTokens(MAX_TOKENS)
                .thinking(ThinkingConfigAdaptive.builder().build())
                .addUserMessage(prompt)
                .build();

        Message response = client.messages().create(params);
        return response.content().stream()
                .flatMap(block -> block.text().stream())
                .map(textBlock -> textBlock.text())
                .collect(Collectors.joining("\n"));
    }

    /** Răspuns structurat JSON conform ClaudeResponse. */
    public ClaudeResponse ask(String prompt) {
        StructuredMessageCreateParams<ClaudeResponse> params = MessageCreateParams.builder()
                .model(Model.CLAUDE_OPUS_4_8)
                .maxTokens(MAX_TOKENS)
                .thinking(ThinkingConfigAdaptive.builder().build())
                .outputConfig(ClaudeResponse.class)
                .addUserMessage(prompt)
                .build();

        return client.messages().create(params).content().stream()
                .flatMap(block -> block.text().stream())
                .map(typed -> typed.text())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Claude nu a returnat un răspuns structurat"));
    }
}
```

- [ ] **Step 4: Verifică compilarea (și testul, dacă ai cheia)**

Run: `./mvnw -q -DskipTests compile` apoi `./mvnw -q -Dtest=ClaudeClientNetworkTest test`
Expected: compile BUILD SUCCESS. Dacă `ANTHROPIC_API_KEY` e setat → testul PASS; altfel testul e SKIPPED (disabled), ceea ce e acceptat.

> Notă pentru implementator: dacă `javac` reclamă un nume de simbol din SDK (ex. `StructuredMessageCreateParams` sau o semnătură de builder), confirmă numele cu `jar tf ~/.m2/.../anthropic-java-2.34.0.jar | grep -i <term>` și ajustează — restul logicii rămâne identic. Nu schimba modelul sau parametrul de thinking.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/ai/assistant/ai/ClaudeClient.java \
        src/test/java/com/ai/assistant/ai/ClaudeClientNetworkTest.java
git commit -m "feat(ai): add Claude client (text + structured JSON)"
```

---

### Task 6: Rewire ingestie + Pinecone pe `EmbeddingClient`, șterge `CopilotClient`

**Files:**
- Modify: `src/main/java/com/ai/assistant/service/DocumentIngestionService.java`
- Modify: `src/main/java/com/ai/assistant/client/EnhancedPineconeClient.java`
- Modify: `src/main/java/com/ai/assistant/controller/ChatController.java`
- Delete: `src/main/java/com/ai/assistant/client/CopilotClient.java`

**Interfaces:**
- Consumes: `EmbeddingClient.embed(String)`, `ClaudeClient.generateText(String)`.
- Produces: niciun consumator rămas al lui `CopilotClient`.

- [ ] **Step 1: Înlocuiește dependența în `DocumentIngestionService`**

Schimbă câmpul și constructorul ca să folosească `EmbeddingClient` în loc de `CopilotClient`. În `DocumentIngestionService`:
- înlocuiește `import com.ai.assistant.client.CopilotClient;` cu `import com.ai.assistant.ai.EmbeddingClient;`
- înlocuiește câmpul `private final CopilotClient copilotClient;` cu `private final EmbeddingClient embeddingClient;`
- în constructor, înlocuiește parametrul `CopilotClient copilotClient` cu `EmbeddingClient embeddingClient` și atribuirea corespunzătoare
- în `processPdfChunk` și `processSqlStatement`, înlocuiește `copilotClient.createEmbedding(...)` cu `embeddingClient.embed(...)`

- [ ] **Step 2: Înlocuiește dependența în `EnhancedPineconeClient`**

- înlocuiește `import com.ai.assistant.client.CopilotClient;` cu `import com.ai.assistant.ai.EmbeddingClient;`
- înlocuiește câmpul `private final CopilotClient copilotClient;` cu `private final EmbeddingClient embeddingClient;`
- în constructor, înlocuiește parametrul și atribuirea
- înlocuiește toate apelurile `copilotClient.createEmbedding(...)` cu `embeddingClient.embed(...)` (în `searchSqlSchema` și `searchPdfDocuments`)

- [ ] **Step 3: Înlocuiește dependența în `ChatController`**

- înlocuiește `import com.ai.assistant.client.CopilotClient;` cu `import com.ai.assistant.ai.ClaudeClient;`
- înlocuiește câmpul `private final CopilotClient copilotClient;` cu `private final ClaudeClient claudeClient;`
- în constructor, înlocuiește parametrul și atribuirea
- în `generateSqlQueryResponse` și `generatePdfQueryResponse`, înlocuiește `copilotClient.generateResponse(...)` cu `claudeClient.generateText(...)` (scoate `throws IOException` dacă devine nefolosit — `generateText` nu aruncă `IOException`)

- [ ] **Step 4: Șterge `CopilotClient`**

```bash
git rm src/main/java/com/ai/assistant/client/CopilotClient.java
```

- [ ] **Step 5: Verifică build-ul complet**

Run: `./mvnw -q -DskipTests package`
Expected: BUILD SUCCESS, fără referințe rămase la `CopilotClient`.
Dacă apare `cannot find symbol CopilotClient`, caută `grep -rn CopilotClient src/` și curăță importurile/parametrii rămași.

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "refactor(ai): replace Copilot with EmbeddingClient + ClaudeClient"
```

---

### Task 7: Config pe variabile de mediu + indexul Pinecone la 1024

**Files:**
- Modify: `src/main/resources/application.properties`

**Interfaces:**
- Consumes: proprietățile `voyage.api.key`, `pinecone.api.key`; SDK Claude citește `ANTHROPIC_API_KEY` direct din mediu.
- Produces: config fără secrete în clar; placeholder pentru dimensiunea 1024.

- [ ] **Step 1: Mută cheile pe variabile de mediu**

În `application.properties`:
- șterge liniile `copilot.api.key=...`
- înlocuiește `pinecone.api.key=your-pinecone-api-key` cu `pinecone.api.key=${PINECONE_API_KEY}`
- adaugă `voyage.api.key=${VOYAGE_API_KEY}`
- înlocuiește credențialele DB hardcodate cu `spring.datasource.username=${DB_USER}` și `spring.datasource.password=${DB_PASSWORD}`
- adaugă un comentariu: `# IMPORTANT: indexul Pinecone trebuie creat cu dimension=1024 (Voyage voyage-3.5). Indexul vechi ada-002 (1536) nu mai e compatibil.`

- [ ] **Step 2: Verifică pornirea contextului Spring (fără rețea)**

Run: `./mvnw -q -DskipTests package`
Expected: BUILD SUCCESS. (Pornirea completă a aplicației necesită env vars + DB; nu o cerem aici.)

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/application.properties
git commit -m "config: move secrets to env vars, note Pinecone dim=1024"
```

---

## Self-Review

- **Spec coverage:** Plan 1 acoperă din spec: înlocuirea Copilot→Claude (Task 5,6), embeddings Voyage izolat (Task 2,3), RAG păstrat (rewire Task 6), config pe env vars (Task 7), scoatere Vaadin (Task 1). Modelul de date, domeniul (company/invoice/employee), advisor-ul și endpoint-urile `/advisor` sunt în Plan 2 și Plan 3 — în afara acestui plan, intenționat.
- **Placeholder scan:** fără TBD/TODO; fiecare pas are cod sau comandă concretă.
- **Type consistency:** `EmbeddingClient.embed(String)` folosit identic în Task 3/6; `ClaudeClient.generateText`/`ask` definite în Task 5 și consumate în Task 6; `ClaudeResponse` definit în Task 4 și folosit în Task 5.
- **Risc cunoscut (notat în plan):** numele exacte ale tipurilor SDK (`StructuredMessageCreateParams`, builder-ele) se confirmă cu `jar tf`/`javap` dacă `javac` se plânge — logica nu se schimbă.
