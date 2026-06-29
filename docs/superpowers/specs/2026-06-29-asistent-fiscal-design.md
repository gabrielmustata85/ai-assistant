# Asistent Fiscal pentru Firme — Design

**Data:** 2026-06-29
**Status:** Aprobat pentru planificare

## 1. Rezumat

Pivotăm proiectul existent (asistent RAG pentru PDF/SQL bazat pe Copilot) într-un
**asistent fiscal pentru firme din România**. Firma își introduce datele (facturi
emise/primite, angajați și salarii, alte cheltuieli), iar asistentul — folosind
**Claude** și o bază de cunoștințe de legislație fiscală (RAG cu Pinecone) — estimează
**când și cât** are de plătit la taxe și oferă **recomandări de optimizare** a
cheltuielilor/taxelor.

### Poziționare (important)

Produsul este un **tool de sugestie/asistență**, NU o sursă fiscală autoritară.
Cifrele sunt **orientative** și nu înlocuiesc contabilul. Această poziționare este
asumată explicit prin tot designul (disclaimer atașat fiecărui răspuns).

## 2. Decizii cheie (din brainstorming)

| Subiect | Decizie |
|---|---|
| Regimuri fiscale | Toate tipurile de firme; userul **își declară** tipul firmei și regimul, iar asistentul aplică regulile potrivite |
| Calculul taxelor | **Claude calculează tot** (aritmetică + raționament). Acceptat conștient pentru că e tool de sugestie, nu autoritate. Datele brute se stochează separat ca să se poată adăuga ulterior un motor determinist fără rescriere |
| Intrare date | **Manual prin formulare/API** (MVP). Import ANAF e-Factura / PDF — faza 2 |
| Interfață | **Doar API REST** (scoatem Vaadin) |
| Bază de reguli | **RAG cu Pinecone** — documente de legislație încărcate de noi, căutate semantic, date lui Claude |
| Alerte | **La cerere** — userul întreabă „ce am de plătit?", se calculează pe loc. Fără job proactiv în MVP |
| Răspuns AI | **JSON structurat**: estimări, termene, recomandări, date lipsă |
| Arhitectură | **Monolit modular pe domenii** (Abordarea A) |
| LLM | **Claude** (`claude-opus-4-8` pentru raționament; `claude-sonnet-4-6` ca opțiune de cost) |
| Embeddings | **Voyage AI** (Claude nu are API de embeddings) — izolat în spatele unei interfețe |

### Flux de responsabilitate (rezumat agreat cu userul)

1. **Noi** importăm documentele de legislație → devin „standardul" (RAG).
2. **Clientul** introduce facturile emise/primite.
3. **Asistentul cere activ** userului ce-i mai lipsește (nr. angajați, salarii brute,
   alte cheltuieli) ca să poată analiza cât plătește la stat.
4. Cu toate datele → **estimează când și cât** are de plătit.
5. Oferă **recomandări de optimizare** (ex: „adu facturi de carburant ca să scazi
   impozitul", „mai ai nevoie de X lei cheltuieli deductibile").

## 3. Arhitectură

Monolit modular Spring Boot, organizat pe module de domeniu:

```
com.ai.assistant
├── company/      # Firma: CUI, denumire, tip firmă, regim fiscal, plătitor TVA
├── invoicing/    # Facturi emise + primite
├── payroll/      # Angajați + salarii
├── knowledge/    # Documente legislație → Pinecone (RAG)
├── advisor/      # Orchestrator: date firmă + RAG → Claude → calcule/alerte/sfaturi
├── ai/           # ClaudeClient + EmbeddingClient (Voyage) — izolați în spatele unei interfețe
└── common/       # config, excepții, security, disclaimer
```

Fiecare modul are o responsabilitate unică, interfețe bine definite, și se testează
independent.

### Ce păstrăm din codul existent
- Spring Boot, PostgreSQL + Flyway, Spring Security
- Arhitectura RAG: `PineconeClient` / `EnhancedPineconeClient`, logica de chunking,
  `DocumentIngestionService` (refolosit pentru ingestia legislației),
  `ConversationContext`
- `AIResponseHistory` (istoricul interacțiunilor)

### Ce scoatem
- `CopilotClient` → înlocuit cu `ClaudeClient` (chat) + `EmbeddingClient` (Voyage)
- Vaadin din `pom.xml` (REST-only)
- Partea SQL-assistant din `ChatController` (generare query-uri SQL)

## 4. Modelul de date

```
company
  id, cui (cod fiscal), name, company_type (SRL/PFA/II...),
  tax_regime (micro_1%, micro_3%, profit_16%, pfa...),
  vat_payer (bool), created_at

invoice                # facturi
  id, company_id, direction (ISSUED / RECEIVED),
  invoice_number, partner_name, partner_cui,
  issue_date, due_date, net_amount, vat_amount, gross_amount,
  category (carburant, chirie, utilitati, marfa...),   # pt. optimizare
  deductible (bool), created_at

employee               # angajați
  id, company_id, full_name, cnp (optional),
  gross_salary, position, start_date, active

expense                # alte cheltuieli care nu-s pe factură
  id, company_id, description, category, amount, date, deductible

knowledge_document     # metadate documente legislație (vectorii stau în Pinecone)
  id, title, source, uploaded_at, namespace

ai_interaction         # istoricul Q&A + recomandări (evoluție din AIResponseHistory)
  id, session_id, company_id, user_query, ai_response,
  data_gaps (ce a cerut asistentul), timestamp
```

**Principii:**
- `category` + `deductible` pe facturi/cheltuieli sunt esențiale pentru recomandările
  de optimizare.
- Datele brute (facturi, salarii) se stochează mereu separat de calculele AI — permit
  adăugarea ulterioară a unui motor determinist fără pierderea datelor.
- Vectorii legislației stau în Pinecone; în Postgres ținem doar metadate.

## 5. Flux de date (cerere „ce taxe am de plătit?")

```
1. POST /advisor/ask  { sessionId, companyId, question }
2. Advisor adună din DB: firma (CUI, regim), facturile, angajații, cheltuielile
3. EmbeddingClient (Voyage) → embedding pe întrebare + regim fiscal
4. PineconeClient → fragmentele relevante de legislație (cote, praguri, termene)
5. Advisor construiește prompt pentru Claude:
     - rol: „asistent fiscal, sume orientative"
     - reguli din legislație (RAG)
     - datele firmei (facturi/salarii/cheltuieli)
     - istoricul conversației (ConversationContext)
     - întrebarea
6. ClaudeClient → răspuns JSON: estimări sume + termene + recomandări
7. Dacă lipsesc date → câmpul `data_gaps` → le returnăm userului ca întrebări
8. Salvăm în ai_interaction, returnăm JSON
```

### Contract răspuns Claude (JSON structurat)
```json
{
  "estimari": [ { "tip_taxa": "...", "suma": 0, "perioada": "...", "explicatie": "..." } ],
  "termene":  [ { "obligatie": "...", "scadenta": "YYYY-MM-DD" } ],
  "recomandari": [ { "text": "...", "impact_estimat": "..." } ],
  "data_gaps": [ "ce date lipsesc și trebuie cerute userului" ],
  "disclaimer": "sume orientative, verifică cu contabilul"
}
```

### Integrare AI
- **ClaudeClient** — model `claude-opus-4-8` (opțiune cost: `claude-sonnet-4-6`); cere
  răspuns structurat JSON.
- **EmbeddingClient** — interfață cu implementare Voyage AI; izolat, ușor de schimbat.
- **Disclaimer** injectat mereu în răspuns.

## 6. API REST

```
# Firma
POST   /companies                 # creează firmă (CUI, tip, regim)
GET    /companies/{id}
PATCH  /companies/{id}

# Facturi
POST   /companies/{id}/invoices   # adaugă factură (emisă/primită)
GET    /companies/{id}/invoices
DELETE /invoices/{id}

# Angajați & cheltuieli
POST   /companies/{id}/employees
GET    /companies/{id}/employees
POST   /companies/{id}/expenses
GET    /companies/{id}/expenses

# Legislație (ingestie de către noi)
POST   /knowledge/upload          # PDF legislație → Pinecone
GET    /knowledge/documents

# Asistentul fiscal
POST   /advisor/ask               # întrebarea principală → JSON estimări+recomandări
GET    /advisor/obligations/{companyId}   # „ce am de plătit?" la cerere
POST   /advisor/reset             # resetează conversația
```

## 7. Cross-cutting

- **Securitate / multi-tenancy:** Spring Security stateless cu **JWT** (bearer token).
  Fiecare user (`app_user`) deține una sau mai multe firme (`company.owner_user_id`).
  Izolare strictă: `CompanyService.get(companyId)` e **punctul unic de control** care
  verifică ownership-ul (`CompanyAccessGuard`) — pentru că facturi/angajați/cheltuieli/
  advisor trec prin el, un user nu poate accesa datele altuia (403). Userul curent se ia
  doar din `SecurityContext`, niciodată din URL/body. Detaliat în Plan 4.
- **Tratarea erorilor:** dacă Claude pică sau dă JSON invalid → eroare clară, NU cifre
  inventate. Reîncercări cu `RetryInterceptor` existent.
- **Disclaimer:** atașat fiecărui răspuns de la advisor.
- **Config:** cheile (Claude, Voyage, Pinecone) în variabile de mediu, nu în
  `application.properties`.

## 8. Testare

- **Unit:** serviciile de domeniu (company, invoicing, payroll) — deterministe, fără AI.
- **Integrare:** endpoint-urile REST cu Postgres de test.
- **AI:** `ClaudeClient` / `EmbeddingClient` mock-uite (fără apeluri reale în teste);
  verificăm construcția prompt-ului și parsarea JSON-ului.

## 9. În afara scope-ului (faze viitoare)

- Import ANAF e-Factura (SPV, OAuth, certificat)
- Import CSV/PDF facturi
- Alerte proactive (job programat + email)
- Motor determinist de calcul (datele brute sunt deja pregătite pentru asta)
- UI (Vaadin sau frontend separat)