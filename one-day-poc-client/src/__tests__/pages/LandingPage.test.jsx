import { render, screen, waitFor } from '@testing-library/react';
import { vi, test, expect, beforeEach } from 'vitest';
import { MemoryRouter } from 'react-router-dom';
import LandingPage from '../../pages/LandingPage.js';
import { AuthProvider } from '@/contexts/AuthContext.tsx';
import * as ConversationContext from '@/contexts/ConversationContext.tsx';
import React from 'react';
import { mockAuth } from '../mocks/authContext.mock.jsx';
import { useAuth } from '@/contexts/AuthContext.tsx';

vi.mock('../../components/landing/HeroSection', () => ({
  default: () => <div>Enabling you from</div>,
}));

vi.mock('../../components/landing/OldPrompts', () => ({
  default: () => <div data-testid="old-prompts">Mock Old Prompts</div>,
}));

vi.mock('../../components/landing/InputBox', () => ({
  default: () => <div data-testid="input-box">Mock Input Box</div>,
}));

vi.mock('@/contexts/AuthContext.tsx', () => {
  const mockUseAuth = vi.fn();
  mockUseAuth.mockReturnValue({ isAuthenticated: false, login: vi.fn() });

  return {
    useAuth: mockUseAuth,
    AuthProvider: ({ children }) => children,
  };
});

const mockCreateConversation = vi.fn();
const mockFetchConversations = vi.fn();
vi.spyOn(ConversationContext, 'useConversation').mockImplementation(() => ({
  createConversation: mockCreateConversation,
  conversations: [],
  fetchConversations: mockFetchConversations,
  currentConversationId: null,
  setCurrentConversationId: vi.fn(),
}));

beforeEach(() => {
  vi.clearAllMocks();
});

test('Renders landing page', async () => {
  await mockAuth({ isAuthenticated: false });

  render(
    <MemoryRouter>
      <AuthProvider>
        <LandingPage />
      </AuthProvider>
    </MemoryRouter>
  );

  const inputBox = screen.getByTestId('input-box');
  expect(inputBox).toBeInTheDocument();

  const heroText = screen.getByText('Enabling you from');
  expect(heroText).toBeInTheDocument();

  const promptElement = screen.getByText(
    /AI chatbot assistant for customer self-service/i
  );
  expect(promptElement).toBeInTheDocument();
});

test('Authenticated users see new prompts', async () => {
  await mockAuth({ isAuthenticated: true });

  render(
    <MemoryRouter>
      <AuthProvider>
        <LandingPage />
      </AuthProvider>
    </MemoryRouter>
  );

  expect(
    screen.getByText(/AI chatbot assistant for customer self-service/i)
  ).toBeInTheDocument();
  expect(
    screen.getByText(/Dashboard for financial reports/i)
  ).toBeInTheDocument();
  expect(
    screen.getByText(/Intelligent document processing tool/i)
  ).toBeInTheDocument();
  expect(screen.getByTestId('input-box')).toBeInTheDocument();

  const promptButtons = screen.getAllByRole('button');
  expect(promptButtons.length).toBe(3);
});

test("Unauthenticated users dont't see OldPrompts component", async () => {
  await mockAuth({ isAuthenticated: false });

  render(
    <MemoryRouter>
      <AuthProvider>
        <LandingPage />
      </AuthProvider>
    </MemoryRouter>
  );

  expect(
    screen.getByText(/AI chatbot assistant for customer self-service/i)
  ).toBeInTheDocument();
  expect(screen.getByTestId('input-box')).toBeInTheDocument();

  const oldPromptsElement = screen.queryByTestId('old-prompts');
  expect(oldPromptsElement).not.toBeInTheDocument();
});

test('Authenticated users see OldPrompts component', async () => {
  useAuth.mockReturnValue({ isAuthenticated: true, login: vi.fn() });

  render(
    <MemoryRouter>
      <AuthProvider>
        <LandingPage />
      </AuthProvider>
    </MemoryRouter>
  );

  expect(screen.getByTestId('input-box')).toBeInTheDocument();
  expect(
    screen.getByText(/AI chatbot assistant for customer self-service/i)
  ).toBeInTheDocument();

  const oldPromptsElement = screen.getByTestId('old-prompts');
  expect(oldPromptsElement).toBeInTheDocument();
  expect(oldPromptsElement).toHaveTextContent('Mock Old Prompts');
});
