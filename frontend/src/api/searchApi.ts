import axios from 'axios';
import type { QueryRequest, QueryResponse, AuditEntry } from '../types';

const client = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
});

client.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response) {
      const msg = error.response.data?.detail || error.response.statusText;
      return Promise.reject(new Error(`Server error (${error.response.status}): ${msg}`));
    }
    if (error.request) {
      return Promise.reject(new Error('No response from server. Please check your connection.'));
    }
    return Promise.reject(new Error(error.message));
  }
);

export async function searchQuery(userId: string, query: string): Promise<QueryResponse> {
  const payload: QueryRequest = { userId, query };
  const { data } = await client.post<QueryResponse>('/query', payload);
  return data;
}

export async function fetchAuditLogs(): Promise<AuditEntry[]> {
  const { data } = await client.get<AuditEntry[]>('/audit');
  return data;
}
