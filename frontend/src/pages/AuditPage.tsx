import { useState, useEffect, useCallback } from 'react';
import { Box, Typography, Button, Alert } from '@mui/material';
import RefreshIcon from '@mui/icons-material/Refresh';
import AuditLogTable from '../components/AuditLogTable';
import { fetchAuditLogs } from '../api/searchApi';
import type { AuditEntry } from '../types';

export default function AuditPage() {
  const [entries, setEntries] = useState<AuditEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadAudits = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await fetchAuditLogs();
      setEntries(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load audit logs');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadAudits();
  }, [loadAudits]);

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Typography variant="h5">Audit Log</Typography>
        <Button startIcon={<RefreshIcon />} onClick={loadAudits} disabled={loading}>
          Refresh
        </Button>
      </Box>
      <Typography variant="body2" color="text.secondary" gutterBottom>
        All queries and AI responses are logged for compliance and review.
      </Typography>

      {error && (
        <Alert severity="error" sx={{ mt: 2 }} action={<Button onClick={loadAudits} size="small">Retry</Button>}>
          {error}
        </Alert>
      )}

      <AuditLogTable entries={entries} loading={loading} />
    </Box>
  );
}
