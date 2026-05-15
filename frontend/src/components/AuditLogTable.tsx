import { useState } from 'react';
import {
  Table, TableBody, TableCell, TableContainer, TableHead, TableRow,
  Paper, Typography, Skeleton, Chip, Box, IconButton, Collapse,
} from '@mui/material';
import KeyboardArrowDownIcon from '@mui/icons-material/KeyboardArrowDown';
import KeyboardArrowUpIcon from '@mui/icons-material/KeyboardArrowUp';
import type { AuditEntry } from '../types';

interface AuditLogTableProps {
  entries: AuditEntry[];
  loading: boolean;
}

function AuditRow({ entry }: { entry: AuditEntry }) {
  const [open, setOpen] = useState(false);

  return (
    <>
      <TableRow hover>
        <TableCell>
          <IconButton size="small" onClick={() => setOpen(!open)}>
            {open ? <KeyboardArrowUpIcon /> : <KeyboardArrowDownIcon />}
          </IconButton>
        </TableCell>
        <TableCell>{entry.id}</TableCell>
        <TableCell>{entry.userId}</TableCell>
        <TableCell sx={{ maxWidth: 200, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
          {entry.queryText}
        </TableCell>
        <TableCell>
          <Chip
            label={entry.response.length > 50 ? entry.response.substring(0, 50) + '...' : entry.response}
            size="small"
            color={entry.response.startsWith('I cannot') ? 'warning' : 'success'}
            variant="outlined"
          />
        </TableCell>
        <TableCell>{new Date(entry.createdAt).toLocaleString()}</TableCell>
      </TableRow>
      <TableRow>
        <TableCell style={{ paddingBottom: 0, paddingTop: 0 }} colSpan={6}>
          <Collapse in={open} timeout="auto" unmountOnExit>
            <Box sx={{ m: 2 }}>
              <Typography variant="subtitle2" gutterBottom>Full Response</Typography>
              <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap' }}>{entry.response}</Typography>
              {entry.citations && (
                <>
                  <Typography variant="subtitle2" gutterBottom sx={{ mt: 1 }}>Citations</Typography>
                  <Typography variant="body2">{entry.citations}</Typography>
                </>
              )}
            </Box>
          </Collapse>
        </TableCell>
      </TableRow>
    </>
  );
}

export default function AuditLogTable({ entries, loading }: AuditLogTableProps) {
  if (loading) {
    return (
      <Box sx={{ mt: 2 }}>
        <Skeleton variant="rectangular" height={48} sx={{ mb: 1, borderRadius: 1 }} />
        <Skeleton variant="rectangular" height={48} sx={{ mb: 1, borderRadius: 1 }} />
        <Skeleton variant="rectangular" height={48} sx={{ borderRadius: 1 }} />
      </Box>
    );
  }

  if (entries.length === 0) {
    return (
      <Typography variant="body1" color="text.secondary" sx={{ mt: 2, textAlign: 'center' }}>
        No audit entries yet. Submit a query to see it here.
      </Typography>
    );
  }

  return (
    <TableContainer component={Paper} sx={{ mt: 2 }}>
      <Table size="small">
        <TableHead>
          <TableRow>
            <TableCell />
            <TableCell>ID</TableCell>
            <TableCell>User</TableCell>
            <TableCell>Query</TableCell>
            <TableCell>Response</TableCell>
            <TableCell>Timestamp</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {entries.map((entry) => (
            <AuditRow key={entry.id} entry={entry} />
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  );
}
