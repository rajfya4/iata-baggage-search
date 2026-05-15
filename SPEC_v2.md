# SPECIFICATION: IATA Baggage Policy Search Workflow

## 1. Overview
The IATA Baggage Policy Search Workflow is an AI-powered enterprise search tool designed for Customer Support Agents and Operations Managers. It allows staff to query baggage fee rules and policies strictly against official IATA guidelines, eliminating the need to manually search through hundreds of manual pages.

## 2. Core Business Objectives
* Provide instant, highly accurate answers to baggage policy queries.
* Ensure 100% compliance by restricting the AI's knowledge base exclusively to the provided IATA Guidance Document.
* Maintain a comprehensive, immutable audit log of all queries and AI responses for management review without requiring a complex database server installation.

## 3. User Roles
* **Customer Support Agent / Operations Manager:** Submits queries through the UI, receives AI-generated summaries, and views the source citations.
* **System Administrator / Auditor:** Reviews the audit logs in the database to ensure AI accuracy and compliance.

## 4. Key Capabilities & Workflow
1. **Document Ingestion (RAG):** The system will utilize the `guidance-document-on-baggage-standards-for-interline.pdf` as its sole knowledge base. The document is chunked and stored in a vector database/store.
2. **Strict Grounding:** The LLM is instructed via system prompt to *only* use the retrieved context to answer. If the answer is not in the document, it must refuse to answer.
3. **Query Resolution:** The user inputs a query in the frontend UI. The backend retrieves the most relevant document sections, passes them to the LLM, and returns the summarized answer to the user.
4. **Audit Logging:** Before the HTTP response is closed, the system writes the User ID, Timestamp, User Query, and the AI Response into a lightweight RDBMS database (SQLite).

## 5. Strict Constraints
* **No JSON Storage:** The database must use strict relational columns (RDBMS). SQLite is used to satisfy this constraint while remaining serverless.
* **No Streamlit/Gradio:** The UI must be built using a custom web framework (React).
* **Hallucination Prevention:** The AI agent must fail gracefully if the source document lacks the requested information.
