import { useState, KeyboardEvent } from 'react';
import { Box, TextField, Button, CircularProgress, Typography } from '@mui/material';
import SendIcon from '@mui/icons-material/Send';

interface SearchBarProps {
  onSearch: (query: string) => void;
  disabled: boolean;
}

export default function SearchBar({ onSearch, disabled }: SearchBarProps) {
  const [query, setQuery] = useState('');
  const maxChars = 1000;

  const handleSubmit = () => {
    const trimmed = query.trim();
    if (trimmed && trimmed.length <= maxChars) {
      onSearch(trimmed);
    }
  };

  const handleKeyDown = (e: KeyboardEvent<HTMLDivElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSubmit();
    }
  };

  const charCount = query.length;
  const overLimit = charCount > maxChars;
  const canSubmit = query.trim().length > 0 && !overLimit && !disabled;

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
      <TextField
        fullWidth
        multiline
        maxRows={4}
        placeholder="Ask about IATA baggage policy (e.g., 'What is the checked baggage allowance for international flights?')"
        value={query}
        onChange={(e) => setQuery(e.target.value)}
        onKeyDown={handleKeyDown}
        disabled={disabled}
        error={overLimit}
        helperText={overLimit ? `Query must not exceed ${maxChars} characters` : `${charCount}/${maxChars}`}
        variant="outlined"
      />
      <Box sx={{ display: 'flex', justifyContent: 'flex-end' }}>
        <Button
          variant="contained"
          endIcon={disabled ? <CircularProgress size={20} color="inherit" /> : <SendIcon />}
          onClick={handleSubmit}
          disabled={!canSubmit}
        >
          {disabled ? 'Searching...' : 'Search'}
        </Button>
      </Box>
    </Box>
  );
}
