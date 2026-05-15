import { Box, Typography, Alert, Skeleton, Grid } from '@mui/material';
import CitationCard from './CitationCard';
import type { QueryResponse } from '../types';

interface SearchResultProps {
  result: QueryResponse | null;
  loading: boolean;
  error: string | null;
}

export default function SearchResult({ result, loading, error }: SearchResultProps) {
  if (loading) {
    return (
      <Box sx={{ mt: 3 }}>
        <Skeleton variant="rectangular" height={100} sx={{ mb: 2, borderRadius: 1 }} />
        <Skeleton variant="rectangular" height={80} sx={{ borderRadius: 1 }} />
      </Box>
    );
  }

  if (error) {
    return (
      <Alert severity="error" sx={{ mt: 3 }}>
        {error}
      </Alert>
    );
  }

  if (!result) return null;

  const isRefusal = result.answer.includes('cannot answer this question')
    || result.answer.includes('No relevant information');

  return (
    <Box sx={{ mt: 3 }}>
      {isRefusal ? (
        <Alert severity="info" sx={{ mb: 2 }}>
          {result.answer}
        </Alert>
      ) : (
        <>
          <Typography variant="h6" gutterBottom>
            Answer
          </Typography>
          <Typography variant="body1" paragraph sx={{ whiteSpace: 'pre-wrap' }}>
            {result.answer}
          </Typography>
        </>
      )}

      {result.citations.length > 0 && (
        <>
          <Typography variant="h6" gutterBottom sx={{ mt: 2 }}>
            Sources ({result.citations.length})
          </Typography>
          <Grid container spacing={2}>
            {result.citations.map((c, i) => (
              <Grid item xs={12} md={6} key={i}>
                <CitationCard citation={c} />
              </Grid>
            ))}
          </Grid>
        </>
      )}
    </Box>
  );
}
