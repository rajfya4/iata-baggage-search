# Development Prompts — IATA Baggage Policy Search

## How to Use
Each phase contains numbered task prompts. Execute them **sequentially**. Each prompt is self-contained and assumes prior prompts in the same phase have been completed. After each prompt, run the application to verify before moving to the next.

---

## Phase 1 — Project Scaffolding

### Prompt 1.1 — Create Maven Project with POM
```
Create a Maven project at backend/pom.xml for Spring Boot 4 with Java 25.
- Group: com.iata, Artifact: iata-baggage-search
- Parent: org.springframework.boot:spring-boot-starter-parent:4.0.0
- Java version: 25
- Dependencies:
  - spring-boot-starter-web
  - spring-boot-starter-data-jpa
  - com.h2database:h2 (runtime scope)
  - spring-boot-starter-validation
  - org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0
  - org.projectlombok:lombok (optional, annotation processor)
  - org.springframework.boot:spring-boot-starter-test (test scope)
- Build plugin: spring-boot-maven-plugin
- Ensure Maven wrapper (mvnw) is present for Maven 3.9.15
```

### Prompt 1.2 — Main Application Class
```
Create IataSearchApplication.java in backend/src/main/java/com/iata/search/
- Standard @SpringBootApplication class
- Implement CommandLineRunner to log "IATA Baggage Search started on port 8080" on startup
- Add main() method calling SpringApplication.run()
```

### Prompt 1.3 — Application Configuration
```
Create application.yml in backend/src/main/resources/
- Server port: 8080
- H2 datasource: jdbc:h2:mem:iata_search;DB_CLOSE_DELAY=-1
- H2 console enabled at /h2-console
- JPA: hibernate.ddl-auto=none, show-sql=false, dialect=H2Dialect
- sql.init.mode=always, schema-locations=classpath:schema.sql
- Placeholder section (commented) for LLM config: api-key, model, provider
```

### Prompt 1.4 — Database Schema
```
Create schema.sql in backend/src/main/resources/
- Table: audit_log
- Columns:
  - id BIGINT AUTO_INCREMENT PRIMARY KEY
  - user_id VARCHAR(100) NOT NULL
  - query_text CLOB NOT NULL
  - response_text CLOB NOT NULL
  - citations VARCHAR(2000)
  - created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
- Drop table if exists first for idempotency
```

### Prompt 1.5 — Create React Frontend Project
```
Create a React + TypeScript + Vite frontend project at frontend/
- Use Vite 6 with react-ts template
- Dependencies: axios, react-router-dom, @mui/material, @emotion/react, @emotion/styled, @mui/icons-material
- Dev dependencies: @types/react, @types/react-dom, typescript, vite, @vitejs/plugin-react
- vite.config.ts: proxy /api to http://localhost:8080
- index.html: proper title "IATA Baggage Policy Search"
- Clean up default Vite boilerplate (remove default counter, logo, etc.)
```

### Prompt 1.6 — TypeScript Types
```
Create frontend/src/types/index.ts:
- QueryRequest: { userId: string, query: string }
- Citation: { section: string, text: string, relevanceScore: number }
- QueryResponse: { answer: string, citations: Citation[], timestamp: string }
- AuditEntry: { id: number, userId: string, queryText: string, response: string, citations: string, createdAt: string }
```

### Prompt 1.7 — API Client Layer
```
Create frontend/src/api/searchApi.ts:
- Create axios instance with baseURL '/api'
- searchQuery(userId: string, query: string): Promise<QueryResponse>
  → POST /api/query with QueryRequest body
- fetchAuditLogs(): Promise<AuditEntry[]>
  → GET /api/audit
- Add response interceptor that transforms axios errors into user-friendly messages
```

**Verify:** Run `mvn compile` in backend and `npm run build` in frontend. Both should succeed.

---

## Phase 2 — Backend Data Layer

### Prompt 2.1 — JPA Entity
```
Create AuditLog.java in backend/src/main/java/com/iata/search/entity/
- @Entity, @Table(name = "audit_log")
- Fields (with proper JPA annotations):
  - Long id, @Id @GeneratedValue(strategy = IDENTITY)
  - String userId, @Column(name = "user_id", nullable = false)
  - String queryText, @Column(name = "query_text", nullable = false, columnDefinition = "CLOB")
  - String responseText, @Column(name = "response_text", nullable = false, columnDefinition = "CLOB")
  - String citations, @Column(length = 2000)
  - LocalDateTime createdAt, @Column(name = "created_at", nullable = false)
- Use @PrePersist to set createdAt before persist
```

### Prompt 2.2 — Spring Data Repository
```
Create AuditLogRepository.java in backend/src/main/java/com/iata/search/repository/
- Extends JpaRepository<AuditLog, Long>
- Method: List<AuditLog> findAllByOrderByCreatedAtDesc()
```

### Prompt 2.3 — DTOs
```
Create QueryRequest.java in backend/src/main/java/com/iata/search/dto/:
- String userId (not blank)
- String query (not blank)
- Use Jakarta Bean Validation annotations

Create QueryResponse.java:
- String answer
- List<CitationDto> citations (inner/nested class CitationDto with String section, String text, double relevanceScore)
- String timestamp (ISO-8601 instant string)

Create AuditEntryDto.java:
- Long id, String userId, String queryText, String response, String citations, String createdAt
```

### Prompt 2.4 — Audit Service
```
Create AuditService.java in backend/src/main/java/com/iata/search/service/
- @Service, inject AuditLogRepository
- Method: logAudit(userId, queryText, responseText, citations) → AuditLog
  - Build AuditLog entity, save, return
- Method: getAllAudits() → List<AuditEntryDto>
  - Fetch all logs ordered by createdAt desc, map to DTOs, return
- Method: getAuditById(id) → AuditEntryDto (throws if not found, return 404)
```

**Verify:** Write a small test in AuditServiceTest that saves an entry and retrieves it.

---

## Phase 3 — RAG & LLM Integration

### Prompt 3.1 — System Prompt Resource
```
Create backend/src/main/resources/prompts/system-prompt.txt:
"You are an IATA baggage policy expert for interline baggage standards.
You MUST ONLY answer using the provided context below.
If the context does not contain enough information to answer, respond with EXACTLY:
'I cannot answer this question based on the available IATA guidance document.'
Do NOT use any external knowledge or make up information.
Cite the specific section or paragraph number when possible.
Keep answers concise and directly address the user's question."
```

### Prompt 3.2 — AI Configuration
```
Create AiConfig.java in backend/src/main/java/com/iata/search/config/:
- @Configuration
- @Bean for reading system prompt from classpath:prompts/system-prompt.txt into a String
- @Bean for an in-memory list-based document store (List<DocumentChunk>)
  where DocumentChunk is a simple record: String id, String text, String sectionRef, float[] embedding
- @Value for LLM API key, model name, provider from application config
```

### Prompt 3.3 — Document Ingestion Service
```
Create RagService.java in backend/src/main/java/com/iata/search/service/:
- @Service, inject DocumentChunk store (list)
- @PostConstruct method: loadDocument()
  - Read "guidance-document-on-baggage-standards-for-interline.pdf" from classpath
  - Parse using Apache PDFBox (PDDocument.load from ClassPathResource)
  - Extract text, split into chunks of ~500 chars with 50-char overlap
  - For each chunk: compute a simple embedding (or store raw text for MVP)
  - Store in the in-memory list
- Method: retrieveRelevantChunks(query, topK=5) → List<ScoredChunk>
  - Simple keyword/TF-IDF scoring against query terms (MVP approach)
  - Return top K chunks sorted by relevance score
- Define inner record: ScoredChunk(String text, String sectionRef, double score)
```

### Prompt 3.4 — LLM Service
```
Create LlmService.java in backend/src/main/java/com/iata/search/service/:
- @Service
- Method: generateResponse(String userQuery, List<String> contextChunks, String systemPrompt) → String
  - Build full prompt: systemPrompt + "\n\nCONTEXT:\n" + contextChunks joined by "\n---\n" + "\n\nUSER QUERY: " + userQuery
  - If contextChunks is empty, skip LLM call and return: "No relevant information found in the IATA guidance document."
  - Call LLM API via Spring AI's ChatClient or direct REST call
  - Return the response text
- Implement a simple retry (1 retry on failure) with exponential backoff
```

### Prompt 3.5 — Query Service (Orchestrator)
```
Create QueryService.java in backend/src/main/java/com/iata/search/service/:
- @Service, inject RagService, LlmService, AuditService
- Method: processQuery(QueryRequest request) → QueryResponse
  - Validate inputs
  - Call ragService.retrieveRelevantChunks(request.query())
  - Call llmService.generateResponse(request.query(), chunks, systemPrompt)
  - Build citations from chunk metadata
  - Call auditService.logAudit(request.userId(), request.query(), answer, citationsJson)
  - Build and return QueryResponse
- @Transactional to ensure audit log is committed before response
```

**Verify:** Startup backend. H2 console should be accessible at /h2-console.

---

## Phase 4 — REST Controllers

### Prompt 4.1 — Global Exception Handler
```
Create GlobalExceptionHandler.java in backend/src/main/java/com/iata/search/exception/:
- @RestControllerAdvice
- Handle MethodArgumentNotValidException → 400 with field errors
- Handle NoResourceFoundException → 404
- Handle LLM timeout/connection exceptions → 503
- Handle generic Exception → 500
- All return ProblemDetail (RFC 7807) format
```

### Prompt 4.2 — Web Configuration (CORS)
```
Create WebConfig.java in backend/src/main/java/com/iata/search/config/:
- @Configuration implements WebMvcConfigurer
- addCorsMappings: allow origin http://localhost:5173 (Vite dev server)
- Allow methods: GET, POST, OPTIONS
- Allow headers: Content-Type, Authorization
```

### Prompt 4.3 — Query Controller
```
Create QueryController.java in backend/src/main/java/com/iata/search/controller/:
- @RestController, @RequestMapping("/api/query")
- POST endpoint:
  - @RequestBody @Valid QueryRequest
  - Call queryService.processQuery()
  - Return ResponseEntity<QueryResponse> with 200 OK
  - Log request/response timing via SLF4J
- GET endpoint (optional): return simple message or Swagger redirect
```

### Prompt 4.4 — Audit Controller
```
Create AuditController.java in backend/src/main/java/com/iata/search/controller/:
- @RestController, @RequestMapping("/api/audit")
- GET / → List<AuditEntryDto>
  - Call auditService.getAllAudits()
  - Return 200 with list
- GET /{id} → AuditEntryDto
  - Call auditService.getAuditById(id)
  - Return 200 or 404 if not found
```

### Prompt 4.5 — OpenAPI Config
```
Create OpenApiConfig.java in backend/src/main/java/com/iata/search/config/:
- @Configuration
- @Bean returning OpenAPI object
- Title: "IATA Baggage Policy Search API"
- Version: "1.0"
- Description: "AI-powered search over IATA baggage guidelines"
```

**Verify:** Start backend. `curl -X POST http://localhost:8080/api/query -H "Content-Type: application/json" -d "{\"userId\":\"test\",\"query\":\"checked bag fee\"}"` should return a response (even if "no relevant information").

---

## Phase 5 — Frontend Components

### Prompt 5.1 — App Shell with Routing
```
Create/update frontend/src/App.tsx:
- BrowserRouter with routes:
  - "/" → SearchPage
  - "/audit" → AuditPage
- AppBar/navigation with two tabs: "Search" | "Audit Log"
- Use MUI ThemeProvider with a custom theme
- Responsive container for page content
```

### Prompt 5.2 — SearchBar Component
```
Create frontend/src/components/SearchBar.tsx:
- Props: onSearch(query: string) => void, disabled: boolean
- Material UI TextField (multiline, maxRows=4, fullWidth)
- Submit button with loading spinner
- Character count display (max 1000 chars)
- Disable button when empty or over limit
- Enter key submits (Shift+Enter for newline)
```

### Prompt 5.3 — CitationCard Component
```
Create frontend/src/components/CitationCard.tsx:
- Props: section: string, text: string, score: number
- MUI Card with a subtle border
- Section badge/chip at top
- Truncated text with "Show more" expand
- Relevance score as a small progress bar or pill
- Accent color based on score (green > 0.7, yellow > 0.4, red < 0.4)
```

### Prompt 5.4 — SearchResult Component
```
Create frontend/src/components/SearchResult.tsx:
- Props: answer: string, citations: Citation[], loading: boolean, error: string | null
- Loading: MUI Skeleton placeholders
- Error: MUI Alert with error message
- Success: Typography for answer with markdown-like formatting
- Citations section: grid of CitationCards (2 columns)
- "No results" state when answer indicates refusal
```

### Prompt 5.5 — SearchPage
```
Create frontend/src/pages/SearchPage.tsx:
- State: query, result (QueryResponse | null), loading, error
- On submit: call searchApi.searchQuery("anonymous", query)
- Handle loading states, errors, empty results
- Display SearchBar at top
- Display SearchResult below
- Track request timing and show response time
```

### Prompt 5.6 — AuditLogTable Component
```
Create frontend/src/components/AuditLogTable.tsx:
- Props: entries: AuditEntry[], loading: boolean
- MUI Table with columns: ID, User, Query, Response, Citations, Timestamp
- Query/Response cells: truncated with expandable detail
- Responsive: horizontal scroll on mobile
- Loading state: skeleton rows
- Empty state: "No audit entries yet" message
```

### Prompt 5.7 — AuditPage
```
Create frontend/src/pages/AuditPage.tsx:
- On mount: fetch audit logs
- State: entries, loading, error
- Display AuditLogTable
- Auto-refresh button
- Error handling with retry option
- Search/filter by user ID (bonus)
```

**Verify:** `npm run dev` in frontend. Both Search and Audit pages render. API proxy works.

---

## Phase 6 — Hallucination Prevention & Edge Cases

### Prompt 6.1 — Strict Grounding Validation (Backend)
```
In LlmService.java, add post-processing after LLM response:
- Parse the response to check for citation markers [Section X]
- If no section reference found AND answer is not the refusal message:
  - Flag as potentially ungrounded
- If confidence < threshold, prepend warning: "Note: This answer may not be fully covered by the available documentation."
- Keep the refusal message detection: if response contains "cannot answer this question", propagate as-is
```

### Prompt 6.2 — Empty/Safety Responses
```
In QueryService.java, add these checks:
- If query contains profanity or abuse → return "I cannot process this query." without calling LLM
- If retrieved chunks count is 0:
  - Skip LLM call entirely
  - Return QueryResponse with answer "No relevant information found in the IATA guidance document."
  - Empty citations list
  - Still log to audit
- If query exceeds 1000 characters → return 400 validation error
```

### Prompt 6.3 — Rate Limiting
```
Add rate limiting to QueryController:
- Use spring-boot-starter-actuator + bucket4j or simple in-memory rate limiter
- Limit: 10 requests per minute per userId
- Return 429 Too Many Requests with Retry-After header when exceeded
- Add dependency if needed
```

### Prompt 6.4 — Graceful LLM Failure
```
In QueryService, wrap LLM call in try-catch:
- On timeout: return fallback answer "The AI service is temporarily unavailable. Please try again."
- On API error: log the error, return fallback answer
- On malformed response: return "I encountered an error processing your request."
- Always write to audit log even on failure, marking response as "[ERROR] ..." for compliance
```

**Verify:** Send query with profanity → expect refusal. Send query with irrelevant topic ("what is the weather") → expect "no relevant information". Disconnect network → expect graceful fallback.

---

## Phase 7 — Testing

### Prompt 7.1 — Audit Service Test
```
Create AuditServiceTest.java in backend/src/test/java/com/iata/search/service/:
- @SpringBootTest with @AutoConfigureTestDatabase(replace = NONE) — uses H2
- Test: saveAuditLog_ShouldPersistAndReturnEntry()
  - Create and save an audit entry
  - Assert ID is generated, timestamp is set, fields match
- Test: getAllAudits_ShouldReturnDescendingOrder()
  - Save 3 entries with simulated delays
  - Assert returned list is ordered by createdAt desc
- Test: getAuditById_NotFound_ShouldThrow()
  - Assert exception thrown for non-existent ID
```

### Prompt 7.2 — Query Controller Test
```
Create QueryControllerTest.java in backend/src/test/java/com/iata/search/controller/:
- @WebMvcTest(QueryController.class) with mock QueryService
- Test: postQuery_ValidRequest_Returns200()
  - Mock service to return QueryResponse
  - POST valid request → Assert 200 + correct response body
- Test: postQuery_EmptyQuery_Returns400()
  - POST with empty query → Assert 400
- Test: postQuery_NullUserId_Returns400()
  - POST with null userId → Assert 400
- Test: postQuery_ServiceThrows_ReturnsError()
  - Mock service to throw exception → Assert appropriate error status
```

### Prompt 7.3 — Frontend Component Tests
```
For SearchBar.test.tsx:
- Render with required props
- Submit button disabled when input is empty
- Typing enables submit button
- On submit, callback fires with correct query

For SearchResult.test.tsx:
- Loading state shows skeleton
- Error state shows alert with message
- Success state shows answer text and citations
- Empty/refusal state shows appropriate message

For AuditLogTable.test.tsx:
- Loading state shows skeleton rows
- Empty state shows "no entries" message
- Populated state shows correct number of rows
```

**Verify:** `mvn test` in backend — all pass. `npm test` in frontend — all pass.

---

## Phase 8 — Integration & Smoke Test

### Prompt 8.1 — Full Integration Test
```
Create IataSearchApplicationIntegrationTest.java:
- @SpringBootTest(webEnvironment = RANDOM_PORT)
- @Testcontainers or fully embedded (H2, in-memory vector store)
- Test full flow:
  1. POST /api/query with a known query → expect 200
  2. Verify response has answer field populated
  3. Verify audit entry was created
  4. GET /api/audit → expect at least 1 entry
  5. Verify the audit entry matches the query just submitted
```

### Prompt 8.2 — Frontend-Backend Integration Check
```
Manual smoke test checklist (document in README or test file):
1. Start backend: mvn spring-boot:run
2. Start frontend: npm run dev
3. Open browser at http://localhost:5173
4. Submit a query → verify response appears
5. Check H2 console at http://localhost:8080/h2-console
6. Navigate to /audit → verify the query is logged
7. Submit another query → verify both appear in audit log
8. Submit empty query → verify validation error
9. Stop backend → verify frontend shows error gracefully
```

**Verify:** Full end-to-end flow works. Audit trail is complete.

---

## Phase 9 — Documentation & Delivery

### Prompt 9.1 — OpenAPI Annotations
```
Add @Operation and @ApiResponse annotations to both controllers:
- QueryController.POST: summary "Submit a baggage policy query", description "..."
- QueryController.GET: summary "Health check"
- AuditController.GET: summary "Retrieve all audit logs"
- Each method: describe possible 200, 400, 500 responses
- Use @Schema annotations on DTOs for field descriptions
```

### Prompt 9.2 — README
```
Create README.md in project root with:
- Project description
- Tech stack: Java 25, Spring Boot 4, Maven 3.9.15, H2, React 19
- Prerequisites: JDK 25, Node.js 22+, Maven 3.9.15
- Quick start (backup + frontend commands)
- API endpoints table
- H2 console instructions
- Architecture overview (1 paragraph)
```

### Prompt 9.3 — Application Final Polish
```
Update application.yml with:
- Server: tomcat.threads.max=10, graceful shutdown enabled
- Logging: com.iata.search=DEBUG (file appender for audit compliance)
- Management endpoints: health, info (via actuator, add dependency if needed)
Add banner.txt with "IATA Baggage Policy Search" ASCII art
```

**Final check:** Clear audit log by restarting (H2 is in-memory). Run full flow once more. All tests pass.

---

## Execution Checklist

| Phase | # | Task | Done |
|-------|---|------|------|
| 1 | 1.1 | Maven POM | ☐ |
| 1 | 1.2 | Main Application Class | ☐ |
| 1 | 1.3 | application.yml | ☐ |
| 1 | 1.4 | schema.sql | ☐ |
| 1 | 1.5 | React Frontend Project | ☐ |
| 1 | 1.6 | TypeScript Types | ☐ |
| 1 | 1.7 | API Client Layer | ☐ |
| 2 | 2.1 | JPA Entity | ☐ |
| 2 | 2.2 | Repository | ☐ |
| 2 | 2.3 | DTOs | ☐ |
| 2 | 2.4 | Audit Service | ☐ |
| 3 | 3.1 | System Prompt | ☐ |
| 3 | 3.2 | AI Config | ☐ |
| 3 | 3.3 | Document Ingestion (RagService) | ☐ |
| 3 | 3.4 | LLM Service | ☐ |
| 3 | 3.5 | Query Service (Orchestrator) | ☐ |
| 4 | 4.1 | Global Exception Handler | ☐ |
| 4 | 4.2 | Web Config (CORS) | ☐ |
| 4 | 4.3 | Query Controller | ☐ |
| 4 | 4.4 | Audit Controller | ☐ |
| 4 | 4.5 | OpenAPI Config | ☐ |
| 5 | 5.1 | App Shell + Routing | ☐ |
| 5 | 5.2 | SearchBar | ☐ |
| 5 | 5.3 | CitationCard | ☐ |
| 5 | 5.4 | SearchResult | ☐ |
| 5 | 5.5 | SearchPage | ☐ |
| 5 | 5.6 | AuditLogTable | ☐ |
| 5 | 5.7 | AuditPage | ☐ |
| 6 | 6.1 | Strict Grounding Validation | ☐ |
| 6 | 6.2 | Empty/Safety Responses | ☐ |
| 6 | 6.3 | Rate Limiting | ☐ |
| 6 | 6.4 | Graceful LLM Failure | ☐ |
| 7 | 7.1 | Audit Service Test | ☐ |
| 7 | 7.2 | Query Controller Test | ☐ |
| 7 | 7.3 | Frontend Component Tests | ☐ |
| 8 | 8.1 | Full Integration Test | ☐ |
| 8 | 8.2 | Manual Smoke Test | ☐ |
| 9 | 9.1 | OpenAPI Annotations | ☐ |
| 9 | 9.2 | README | ☐ |
| 9 | 9.3 | Final Polish | ☐ |
