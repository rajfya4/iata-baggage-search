import { useState, useCallback } from 'react';
import { Box, Typography } from '@mui/material';
import SearchBar from '../components/SearchBar';
import SearchResult from '../components/SearchResult';
import { searchQuery } from '../api/searchApi';
import type { QueryResponse } from '../types';

export default function SearchPage() {
  const [result, setResult] = useState<QueryResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSearch = useCallback(async (query: string) => {
    setLoading(true);
    setError(null);
    setResult(null);
    try {
      const data = await searchQuery('agent', query);
      setResult(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'An unexpected error occurred');
    } finally {
      setLoading(false);
    }
  }, []);

  return (
    <Box>
      <Typography variant="h5" gutterBottom>
        IATA Baggage Policy Search
      </Typography>
      <Typography variant="body2" color="text.secondary" gutterBottom>
        Ask questions about IATA interline baggage standards. Answers are strictly based on the official guidance document.
      </Typography>
      <Box sx={{ mt: 2 }}>
        <SearchBar onSearch={handleSearch} disabled={loading} />
      </Box>
      <SearchResult result={result} loading={loading} error={error} />
    </Box>
  );
}
