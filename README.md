# IATA Baggage Policy Search

AI-powered enterprise search tool for IATA baggage policy queries. Customer support agents can ask natural language questions and receive grounded answers strictly from the official IATA guidance document.

## Tech Stack

| Component   | Technology                     |
|-------------|--------------------------------|
| Backend     | Java 25, Spring Boot 4         |
| Build       | Maven 3.9.15                   |
| Database    | H2 (in-memory)                 |
| Frontend    | React 19, TypeScript, Vite 6   |
| UI Library  | MUI 6                          |
| AI/LLM      | OpenAI API (pluggable)         |
| PDF Parsing | Apache PDFBox 3                |

## Prerequisites

- JDK 25
- Node.js 22+
- Maven 3.9.15 (or use `mvnw` wrapper)

## Quick Start

### 1. Start Backend
```bash
cd backend
mvn clean spring-boot:run
```
Runs on http://localhost:8080

### 2. Start Frontend
```bash
cd frontend
npm install
npm run dev
```
Runs on http://localhost:5173

## API Endpoints

| Method | Path          | Description                    |
|--------|---------------|--------------------------------|
| POST   | `/api/query`  | Submit a baggage policy query  |
| GET    | `/api/query`  | Health check                   |
| GET    | `/api/audit`  | Retrieve all audit logs        |
| GET    | `/api/audit/{id}` | Retrieve single audit entry |

### POST /api/query

Request:
```json
{
  "userId": "agent123",
  "query": "What is the checked baggage allowance for international flights?"
}
```

Response:
```json
{
  "answer": "Based on Section 3.2 of the IATA guidance document...",
  "citations": [
    {
      "section": "Section 3.2",
      "text": "The standard checked baggage allowance for international...",
      "relevanceScore": 0.85
    }
  ],
  "timestamp": "2026-05-15T12:00:00Z"
}
```

## H2 Console

Accessible at http://localhost:8080/h2-console
- JDBC URL: `jdbc:h2:mem:iata_search`
- User: `sa`
- Password: *(blank)*

## Swagger UI

API documentation at http://localhost:8080/swagger-ui.html

## Architecture

```
React Frontend → REST API → QueryService → RagService (vector retrieval)
                                           → LlmService (AI generation)
                                           → AuditService → H2 Database
```

## LLM Configuration

Set environment variable or edit `application.yml`:
```yaml
ai:
  llm:
    api-key: ${OPENAI_API_KEY}
    model: gpt-4o
    endpoint: https://api.openai.com/v1/chat/completions
```

Without an API key, the system runs in mock mode for development/testing.
