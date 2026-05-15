# Implementation Plan: IATA Baggage Policy Search Workflow

**Stack:** ReactJS | Java 25 | Spring Boot 4 | Maven 3.9.15 | H2 In-Memory DB

---

## 1. Project Structure

```
iata-baggage-search/
├── backend/                          # Maven project (Spring Boot 4 + Java 25)
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/iata/search/
│       │   │   ├── IataSearchApplication.java
│       │   │   ├── config/
│       │   │   │   ├── WebConfig.java              # CORS, static resources
│       │   │   │   ├── OpenApiConfig.java           # SpringDoc OpenAPI
│       │   │   │   └── AiConfig.java                # LLM/RAG configuration
│       │   │   ├── controller/
│       │   │   │   ├── QueryController.java         # POST /api/query
│       │   │   │   └── AuditController.java         # GET /api/audit
│       │   │   ├── dto/
│       │   │   │   ├── QueryRequest.java
│       │   │   │   ├── QueryResponse.java
│       │   │   │   └── AuditEntryDto.java
│       │   │   ├── entity/
│       │   │   │   └── AuditLog.java                # JPA entity
│       │   │   ├── repository/
│       │   │   │   └── AuditLogRepository.java      # Spring Data JPA
│       │   │   ├── service/
│       │   │   │   ├── QueryService.java            # Orchestrator
│       │   │   │   ├── RagService.java              # Vector retrieval
│       │   │   │   ├── LlmService.java              # LLM interaction
│       │   │   │   └── AuditService.java            # Audit logging
│       │   │   └── exception/
│       │   │       └── GlobalExceptionHandler.java
│       │   └── resources/
│       │       ├── application.yml
│       │       ├── schema.sql                       # H2 schema
│       │       └── prompts/
│       │           └── system-prompt.txt            # Strict grounding prompt
│       └── test/
│           └── java/com/iata/search/
│               ├── service/
│               │   └── QueryServiceTest.java
│               └── controller/
│                   └── QueryControllerTest.java
├── frontend/                         # React app
│   ├── package.json
│   ├── tsconfig.json
│   ├── vite.config.ts
│   ├── index.html
│   └── src/
│       ├── main.tsx
│       ├── App.tsx
│       ├── api/
│       │   └── searchApi.ts
│       ├── components/
│       │   ├── SearchBar.tsx
│       │   ├── SearchResult.tsx
│       │   ├── CitationCard.tsx
│       │   └── AuditLogTable.tsx
│       ├── pages/
│       │   ├── SearchPage.tsx
│       │   └── AuditPage.tsx
│       └── types/
│           └── index.ts
└── docs/
    └── guidance-document-on-baggage-standards-for-interline.pdf  # RAG source
```

---

## 2. Phase 1 — Backend Foundation

### 2.1 Maven POM (`pom.xml`)
- **Parent:** `spring-boot-starter-parent:4.0.0` (Spring Boot 4)
- **Java version:** 25
- **Dependencies:**
  - `spring-boot-starter-web` — REST API
  - `spring-boot-starter-data-jpa` — JPA/Hibernate
  - `h2` (runtime) — In-memory DB
  - `spring-boot-starter-validation` — Bean Validation
  - `springdoc-openapi-starter-webmvc-ui` — OpenAPI / Swagger
  - `spring-ai-openai-spring-boot-starter` — LLM integration (or similar)
  - `lombok` — boilerplate reduction
  - `spring-boot-starter-test` — testing

### 2.2 Application Configuration (`application.yml`)
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:iata_search;DB_CLOSE_DELAY=-1
    driver-class-name: org.h2.Driver
    username: sa
    password:
  h2:
    console:
      enabled: true
      path: /h2-console
  jpa:
    hibernate:
      ddl-auto: none        # schema.sql controls DDL
    show-sql: false
    database-platform: org.hibernate.dialect.H2Dialect
  sql:
    init:
      mode: always
      schema-locations: classpath:schema.sql

ai:
  llm:
    provider: openai        # or ollama/local
    model: gpt-4o
    api-key: ${OPENAI_API_KEY}
  vector-store:
    type: simple            # in-memory vector store for MVP

server:
  port: 8080
```

### 2.3 Schema (`schema.sql`)
```sql
CREATE TABLE IF NOT EXISTS audit_log (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     VARCHAR(100)  NOT NULL,
    query_text  CLOB          NOT NULL,
    response    CLOB          NOT NULL,
    citations   VARCHAR(2000),
    created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

### 2.4 JPA Entity (`AuditLog.java`)
- `@Entity` mapped to `audit_log`, fields: `id`, `userId`, `queryText`, `response`, `citations`, `createdAt`

### 2.5 Repository (`AuditLogRepository.java`)
- `extends JpaRepository<AuditLog, Long>`
- Custom: `findAllByOrderByCreatedAtDesc()`

---

## 3. Phase 2 — RAG & LLM Service

### 3.1 Document Ingestion (startup)
- On `@PostConstruct`, parse `guidance-document-on-baggage-standards-for-interline.pdf`
- Chunk text into segments (e.g. 500 chars with 50-char overlap)
- Store chunks + embeddings in an in-memory vector store (`SimpleVectorStore` or `EmbeddingStore`)

### 3.2 `RagService`
- `retrieveRelevantChunks(query: String, topK: int): List<String>`
- Compute embedding for user query
- Cosine-similarity search against stored chunks
- Return top-K most relevant text segments

### 3.3 `LlmService`
- `generateResponse(query: String, context: List<String>, systemPrompt: String): String`
- Construct prompt: system prompt + retrieved context + user query
- Call LLM API (OpenAI / Ollama / Azure OpenAI)
- Return generated answer text

### 3.4 System Prompt (`system-prompt.txt`)
```
You are an IATA baggage policy expert.
You MUST ONLY answer using the provided context below.
If the context does not contain the answer, respond with:
"I cannot answer this question based on the available IATA guidance document."
Do NOT use any external knowledge. Cite the relevant section numbers when possible.
```

### 3.5 `QueryService` (orchestrator)
1. Receive `QueryRequest` (userId, queryText)
2. Call `RagService.retrieveRelevantChunks(query)`
3. Call `LlmService.generateResponse(query, chunks, systemPrompt)`
4. Extract citations from chunk metadata
5. Build `QueryResponse` (answer, citations)
6. **Before returning:** call `AuditService.log(userId, query, response, citations)`
7. Return `QueryResponse`

### 3.6 `AuditService`
- `log(userId, queryText, response, citations)` — saves `AuditLog` entity to H2
- `getAllAudits()` — returns all entries ordered by timestamp desc

---

## 4. Phase 3 — REST Controllers

### 4.1 `QueryController`
| Method | Path           | Body               | Response          |
|--------|----------------|--------------------|-------------------|
| POST   | `/api/query`   | `QueryRequest`     | `QueryResponse`   |
| GET    | `/api/query`   | —                  | Swagger UI        |

- Validate `QueryRequest` (not null, non-blank, user ID present)
- Return 400 on validation failure

### 4.2 `AuditController`
| Method | Path              | Response                |
|--------|-------------------|-------------------------|
| GET    | `/api/audit`      | `List<AuditEntryDto>`   |

- Admin-only in production (skip auth for MVP)
- Returns all audit logs with pagination

### 4.3 DTOs
- `QueryRequest`: `userId`, `query`
- `QueryResponse`: `answer`, `citations[]`, `timestamp`
- `AuditEntryDto`: `id`, `userId`, `query`, `response`, `citations`, `timestamp`

---

## 5. Phase 4 — Frontend (React + TypeScript + Vite)

### 5.1 Setup
- `npm create vite@latest frontend -- --template react-ts`
- Dependencies: `axios`, `react-router-dom`, `@mui/material` (or Tailwind)

### 5.2 Component Tree
```
<App>
  <Router>
    <Route "/" -> <SearchPage>
      <SearchBar onSubmit={handleSearch} />
      <SearchResult answer={...} citations={...} />
        <CitationCard v-for="citation in citations" />
    </Route>
    <Route "/audit" -> <AuditPage>
      <AuditLogTable entries={auditLogs} />
    </Route>
  </Router>
</App>
```

### 5.3 `SearchPage`
- State: `query`, `answer`, `citations`, `loading`, `error`
- On submit: `POST /api/query` via `searchApi.ts`
- Display answer text + citation cards with source section references
- Show error state if API returns 4xx/5xx

### 5.4 `AuditPage`
- On mount: `GET /api/audit`
- Render table with columns: User ID, Timestamp, Query, Response (truncated), Citations

### 5.5 `searchApi.ts`
- `searchQuery(userId, query)` → `POST /api/query`
- `fetchAuditLogs()` → `GET /api/audit`

---

## 6. Phase 5 — Hallucination Prevention & Edge Cases

### 6.1 Strict Grounding Logic
- In `LlmService`, if LLM returns an answer not grounded in context (detected via missing citation markers or explicit "I cannot answer" check), fall back to refusal message
- In `system-prompt.txt`, enforce strict refusal instruction

### 6.2 Error Handling
- `GlobalExceptionHandler` maps all exceptions to structured `ProblemDetail` (RFC 7807)
- Cases: LLM timeout → 503, no results → 404, validation → 400, unexpected → 500

### 6.3 Empty Result Handling
- If `RagService` returns zero chunks (no relevant context found), skip LLM call entirely and return: *"No relevant information found in the IATA guidance document."*

---

## 7. Phase 6 — Testing

### 7.1 Backend Tests
| Test Scope           | What to Test                                                |
|----------------------|-------------------------------------------------------------|
| `QueryServiceTest`   | Happy path, zero chunks, LLM failure, audit logging occurs  |
| `RagServiceTest`     | Chunk retrieval returns correct chunks for known query      |
| `AuditServiceTest`   | Audit entry is persisted and retrievable                    |
| `QueryControllerTest`| 200/400/503 responses, request validation                   |

### 7.2 Frontend Tests
- `SearchPage` renders results correctly
- `SearchBar` calls API on submit
- Error states display correctly
- `AuditPage` fetches and displays logs

---

## 8. Phase 7 — Running the Application

### 8.1 Start Backend
```bash
cd backend
mvn clean spring-boot:run
# Runs on http://localhost:8080
# H2 Console at http://localhost:8080/h2-console
# Swagger UI at http://localhost:8080/swagger-ui.html
```

### 8.2 Start Frontend
```bash
cd frontend
npm install
npm run dev
# Runs on http://localhost:5173
```

### 8.3 Database Verification
- H2 in-memory DB: data persists only during JVM runtime
- `DB_CLOSE_DELAY=-1` keeps DB alive as long as the connection is open
- For debugging: `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:iata_search`)

---

## 9. Architecture Diagram (ASCII)

```
[React Frontend :5173]
       │ POST /api/query {userId, query}
       ▼
[QueryController]
       │
       ▼
[QueryService] ────→ [RagService] ────→ [Vector Store (in-memory)]
       │                    │                  ↑
       │                    └── chunk texts ───┘
       │
       ├──→ [LlmService] ───→ [LLM API (OpenAI / Ollama)]
       │
       └──→ [AuditService] ───→ [AuditLogRepository] ───→ [H2 (in-memory)]
                                                                  │
                                    [AuditController] ←────────────┘
                                            │
                                    [React /audit page]
```

---

## 10. Key Technology Notes

| Component     | Version / Tool                        |
|---------------|---------------------------------------|
| Java          | 25 (latest LTS-style feature release) |
| Spring Boot   | 4.x (with Java 25, virtual threads)   |
| Maven         | 3.9.15                                |
| Database      | H2 2.x (in-memory, `MODE=PostgreSQL` optional) |
| React         | 19 + TypeScript 5.x                   |
| Build Tool    | Vite 6.x                              |
| API Docs      | SpringDoc OpenAPI 3.x                 |
| LLM SDK       | Spring AI 1.x                         |
| PDF Parsing   | Apache PDFBox or Spring AI document reader |

---

## 11. Deviation from Original Spec

| Original Spec      | Actual Implementation             | Reason                                        |
|--------------------|------------------------------------|-----------------------------------------------|
| SQLite database    | H2 in-memory database              | User requirement (fits Spring Boot ecosystem)  |
| Python/Streamlit   | Java 25 + Spring Boot 4 + React    | User requirement                               |
