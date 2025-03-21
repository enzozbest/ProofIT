import { render, screen } from '@testing-library/react';
import { vi, test, expect, beforeEach } from 'vitest';
import { MemoryRouter } from 'react-router-dom';
import userEvent from '@testing-library/user-event';
import { AuthProvider } from '@/contexts/AuthContext';
import { mockAuth } from '../mocks/authContext.mock';

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useNavigate: vi.fn(),
  };
});

beforeEach(() => {
  vi.resetAllMocks();
  vi.resetModules();
});

test('Renders all prompt buttons', async () => {
  await mockAuth({ isAuthenticated: false });

  const { default: GeneratedPrompts } = await import('../../components/GeneratedPrompts');

  const prompts = ['Prompt 1', 'Prompt 2', 'Prompt 3'];

  render(
    <MemoryRouter>
      <AuthProvider>
        <GeneratedPrompts prompts={prompts} />
      </AuthProvider>
    </MemoryRouter>
  );

  prompts.forEach(prompt => {
    const promptButton = screen.getByText(`${prompt} →`);
    expect(promptButton).toBeInTheDocument();
  });
});

test('Handles click when user is not authenticated', async () => {
  const mockLogin = vi.fn();

  vi.doMock('@/contexts/AuthContext', () => ({
    useAuth: () => ({
      isAuthenticated: false,
      login: mockLogin
    }),
    AuthProvider: ({ children }) => children
  }));

  const { default: GeneratedPrompts } = await import('../../components/GeneratedPrompts');

  const prompts = ['Test Prompt'];

  render(
    <MemoryRouter>
      <GeneratedPrompts prompts={prompts} />
    </MemoryRouter>
  );

  const promptButton = screen.getByText('Test Prompt →');
  await userEvent.click(promptButton);

  expect(mockLogin).toHaveBeenCalledWith('Test Prompt');
});

test('Handles click when user is authenticated', async () => {
  await mockAuth({ isAuthenticated: true });

  const { useNavigate } = await import('react-router-dom');
  const navigateMock = vi.fn();
  useNavigate.mockReturnValue(navigateMock);

  const { default: GeneratedPrompts } = await import('../../components/GeneratedPrompts');

  const prompts = ['Test Prompt'];

  render(
    <MemoryRouter>
      <AuthProvider>
        <GeneratedPrompts prompts={prompts} />
      </AuthProvider>
    </MemoryRouter>
  );

  const promptButton = screen.getByText('Test Prompt →');
  await userEvent.click(promptButton);

  expect(navigateMock).toHaveBeenCalledWith('/generate', {
    state: { initialMessage: 'Test Prompt' }
  });
});