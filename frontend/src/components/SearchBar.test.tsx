import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import SearchBar from './SearchBar';

describe('SearchBar', () => {
  it('submit button is disabled when input is empty', () => {
    render(<SearchBar onSearch={() => {}} disabled={false} />);
    const btn = screen.getByRole('button', { name: /search/i });
    expect(btn).toBeDisabled();
  });

  it('typing enables submit button', () => {
    render(<SearchBar onSearch={() => {}} disabled={false} />);
    const input = screen.getByRole('textbox');
    fireEvent.change(input, { target: { value: 'baggage fee' } });
    const btn = screen.getByRole('button', { name: /search/i });
    expect(btn).not.toBeDisabled();
  });

  it('on submit callback fires with correct query', () => {
    const onSearch = vi.fn();
    render(<SearchBar onSearch={onSearch} disabled={false} />);
    const input = screen.getByRole('textbox');
    fireEvent.change(input, { target: { value: 'test query' } });
    fireEvent.click(screen.getByRole('button', { name: /search/i }));
    expect(onSearch).toHaveBeenCalledWith('test query');
  });

  it('shows loading state when disabled', () => {
    render(<SearchBar onSearch={() => {}} disabled={true} />);
    expect(screen.getByText('Searching...')).toBeTruthy();
  });
});
