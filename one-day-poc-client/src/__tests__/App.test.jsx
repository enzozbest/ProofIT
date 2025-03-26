import { render, screen } from '@testing-library/react';
import App from '../App';
import { MemoryRouter, BrowserRouter } from 'react-router-dom';
import { vi, test, expect } from 'vitest';
import { createRoot } from 'react-dom/client';
import '../main';

vi.mock('react-dom/client', () => ({
  createRoot: vi.fn(() => ({
    render: vi.fn(),
  })),
}));

test('Renders base page', () => {
  render(
    <MemoryRouter>
      <App />
    </MemoryRouter>
  );
  expect(screen.getByText(/Enabling you from/i)).toBeInTheDocument();
});

test('Main component renders', () => {
  expect(createRoot).toHaveBeenCalled();
});
