export interface QueryRequest {
  userId: string;
  query: string;
}

export interface Citation {
  section: string;
  text: string;
  relevanceScore: number;
}

export interface QueryResponse {
  answer: string;
  citations: Citation[];
  timestamp: string;
}

export interface AuditEntry {
  id: number;
  userId: string;
  queryText: string;
  response: string;
  citations: string;
  createdAt: string;
}
