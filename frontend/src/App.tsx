import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { ThemeProvider, createTheme, CssBaseline, AppBar, Toolbar, Typography, Tabs, Tab, Box, Container } from '@mui/material';
import SearchIcon from '@mui/icons-material/Search';
import HistoryIcon from '@mui/icons-material/History';
import { Link, useLocation } from 'react-router-dom';
import SearchPage from './pages/SearchPage';
import AuditPage from './pages/AuditPage';

const theme = createTheme({
  palette: {
    primary: { main: '#0d47a1' },
    secondary: { main: '#ff6f00' },
  },
});

function NavTabs() {
  const location = useLocation();
  const value = location.pathname === '/audit' ? 1 : 0;

  return (
    <AppBar position="static">
      <Toolbar>
        <Typography variant="h6" sx={{ mr: 4 }}>
          IATA Baggage Search
        </Typography>
        <Tabs value={value} textColor="inherit" indicatorColor="secondary">
          <Tab icon={<SearchIcon />} label="Search" component={Link} to="/" />
          <Tab icon={<HistoryIcon />} label="Audit Log" component={Link} to="/audit" />
        </Tabs>
      </Toolbar>
    </AppBar>
  );
}

export default function App() {
  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <BrowserRouter>
        <NavTabs />
        <Container maxWidth="lg" sx={{ mt: 4 }}>
          <Routes>
            <Route path="/" element={<SearchPage />} />
            <Route path="/audit" element={<AuditPage />} />
          </Routes>
        </Container>
      </BrowserRouter>
    </ThemeProvider>
  );
}
