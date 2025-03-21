import { render, screen } from '@testing-library/react';
import { vi, test, expect, beforeEach } from 'vitest';
import { MemoryRouter } from 'react-router-dom';
import userEvent from '@testing-library/user-event';
import { AuthProvider } from '@/contexts/AuthContext';
import { mockAuth } from '../mocks/authContext.mock';

// Mock useNavigate
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

  // Check if all prompts are rendered as buttons
  prompts.forEach(prompt => {
    const promptButton = screen.getByText(`${prompt} →`);
    expect(promptButton).toBeInTheDocument();
  });
});

test('Handles click when user is not authenticated', async () => {
  // Create a mock login function
  const mockLogin = vi.fn();

  // Custom mock implementation for useAuth
  vi.doMock('@/contexts/AuthContext', () => ({
    useAuth: () => ({
      isAuthenticated: false,
      login: mockLogin
    }),
    AuthProvider: ({ children }) => children
  }));

  // Import the component after mocking
  const { default: GeneratedPrompts } = await import('../../components/GeneratedPrompts');

  const prompts = ['Test Prompt'];

  // Render without AuthProvider since we're mocking useAuth directly
  render(
    <MemoryRouter>
      <GeneratedPrompts prompts={prompts} />
    </MemoryRouter>
  );

  // Click on the prompt button
  const promptButton = screen.getByText('Test Prompt →');
  await userEvent.click(promptButton);

  // Verify login was called with the prompt text
  expect(mockLogin).toHaveBeenCalledWith('Test Prompt');
});

test('Handles click when user is authenticated', async () => {
  // Mock authenticated state
  await mockAuth({ isAuthenticated: true });

  // Setup navigate mock
  const { useNavigate } = await import('react-router-dom');
  const navigateMock = vi.fn();
  useNavigate.mockReturnValue(navigateMock);

  // Import the component after mocking
  const { default: GeneratedPrompts } = await import('../../components/GeneratedPrompts');

  const prompts = ['Test Prompt'];

  render(
    <MemoryRouter>
      <AuthProvider>
        <GeneratedPrompts prompts={prompts} />
      </AuthProvider>
    </MemoryRouter>
  );

  // Click on the prompt button
  const promptButton = screen.getByText('Test Prompt →');
  await userEvent.click(promptButton);

  // Verify navigation was called with the correct route and state
  expect(navigateMock).toHaveBeenCalledWith('/generate', {
    state: { initialMessage: 'Test Prompt' }
  });
});