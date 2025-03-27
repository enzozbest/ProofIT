import { render, screen } from '@testing-library/react';
import { vi, test, expect, beforeEach, describe } from 'vitest';
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

vi.mock('@/contexts/ConversationContext', () => ({
  useConversation: vi.fn(),
  ConversationProvider: ({ children }) => children,
}));

beforeEach(() => {
  vi.resetAllMocks();
  vi.resetModules();
});

describe('GeneratedPrompts Component', () => {
  test('Renders all prompt buttons', async () => {
    await mockAuth({ isAuthenticated: false });

    const { useConversation } = await import('@/contexts/ConversationContext');
    useConversation.mockReturnValue({
      createConversation: vi.fn(),
    });

    const { default: GeneratedPrompts } = await import(
      '../../components/landing/GeneratedPrompts'
    );

    const prompts = ['Prompt 1', 'Prompt 2', 'Prompt 3'];

    render(
      <MemoryRouter>
        <AuthProvider>
          <GeneratedPrompts prompts={prompts} />
        </AuthProvider>
      </MemoryRouter>
    );

    prompts.forEach((prompt) => {
      const promptButton = screen.getByText(`${prompt} →`);
      expect(promptButton).toBeInTheDocument();
    });
  });

  test('Handles click when user is not authenticated', async () => {
    const mockLogin = vi.fn();

    vi.doMock('@/contexts/AuthContext', () => ({
      useAuth: () => ({
        isAuthenticated: false,
        login: mockLogin,
      }),
      AuthProvider: ({ children }) => children,
    }));

    const { useConversation } = await import('@/contexts/ConversationContext');
    useConversation.mockReturnValue({
      createConversation: vi.fn(),
    });

    const { default: GeneratedPrompts } = await import(
      '../../components/landing/GeneratedPrompts'
    );

    const prompts = ['Test Prompt'];

    render(
      <MemoryRouter>
        <GeneratedPrompts prompts={prompts} />
      </MemoryRouter>
    );

    const promptButton = screen.getByText('Test Prompt →');
    await userEvent.click(promptButton);

    expect(mockLogin).toHaveBeenCalledWith('Test Prompt', true);
  });

  test('Renders empty component when no prompts are provided', async () => {
    await mockAuth({ isAuthenticated: false });

    const { useConversation } = await import('@/contexts/ConversationContext');
    useConversation.mockReturnValue({
      createConversation: vi.fn(),
    });

    const { default: GeneratedPrompts } = await import(
      '../../components/landing/GeneratedPrompts'
    );

    const { container } = render(
      <MemoryRouter>
        <AuthProvider>
          <GeneratedPrompts prompts={[]} />
        </AuthProvider>
      </MemoryRouter>
    );

    // The container should be empty except for the outer div
    expect(container.querySelector('button')).not.toBeInTheDocument();
  });

  test('Creates conversation and navigates when user is authenticated', async () => {
    // Mock the auth context to simulate an authenticated user
    await mockAuth({ isAuthenticated: true });

    // Mock the navigate function
    const { useNavigate } = await import('react-router-dom');
    const navigateMock = vi.fn();
    useNavigate.mockReturnValue(navigateMock);

    // Mock the conversation context with a spy on createConversation
    const createConversationMock = vi
      .fn()
      .mockReturnValue('new-conversation-id');
    const { useConversation } = await import('@/contexts/ConversationContext');
    useConversation.mockReturnValue({
      createConversation: createConversationMock,
    });

    // Import and render the component
    const { default: GeneratedPrompts } = await import(
      '../../components/landing/GeneratedPrompts'
    );

    const prompts = ['Create a portfolio website'];

    render(
      <MemoryRouter>
        <AuthProvider>
          <GeneratedPrompts prompts={prompts} />
        </AuthProvider>
      </MemoryRouter>
    );

    // Find and click the prompt button
    const promptButton = screen.getByText('Create a portfolio website →');
    await userEvent.click(promptButton);

    // Verify that createConversation was called
    expect(createConversationMock).toHaveBeenCalledTimes(1);

    // Verify that navigate was called with the correct parameters
    expect(navigateMock).toHaveBeenCalledWith('/generate', {
      state: {
        initialMessage: 'Create a portfolio website',
        isPredefined: true,
      },
    });
  });
});
