import { useState } from 'react';
import { Card, CardContent, Typography, Chip, Box, Collapse, IconButton } from '@mui/material';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import type { Citation } from '../types';

interface CitationCardProps {
  citation: Citation;
}

export default function CitationCard({ citation }: CitationCardProps) {
  const [expanded, setExpanded] = useState(false);
  const isLong = citation.text.length > 150;

  const scoreColor = citation.relevanceScore >= 0.7 ? 'success' : citation.relevanceScore >= 0.4 ? 'warning' : 'error';

  return (
    <Card variant="outlined" sx={{ borderLeft: 4, borderLeftColor: `${scoreColor}.main` }}>
      <CardContent>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
          <Chip label={citation.section || 'General'} size="small" color="primary" variant="outlined" />
          <Chip label={`${(citation.relevanceScore * 100).toFixed(0)}%`} size="small" color={scoreColor} />
        </Box>
        <Typography variant="body2" color="text.secondary">
          {isLong && !expanded ? citation.text.substring(0, 150) + '...' : citation.text}
        </Typography>
        {isLong && (
          <Box sx={{ display: 'flex', justifyContent: 'flex-end' }}>
            <IconButton size="small" onClick={() => setExpanded(!expanded)}>
              <ExpandMoreIcon sx={{ transform: expanded ? 'rotate(180deg)' : 'none' }} />
            </IconButton>
          </Box>
        )}
      </CardContent>
    </Card>
  );
}
