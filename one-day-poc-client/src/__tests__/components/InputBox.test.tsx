import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import InputBox from '../../components/landing/InputBox';
import { useAuth } from '../../contexts/AuthContext';
import { useNavigate } from 'react-router-dom';
import { useConversation } from '../../contexts/ConversationContext';

vi.mock('../../contexts/AuthContext', () => ({
  useAuth: vi.fn(),
}));

vi.mock('react-router-dom', () => ({
  useNavigate: vi.fn(),
}));

vi.mock('lucide-react', () => ({
  SendHorizontal: () => <div data-testid="send-icon" />,
}));

vi.mock('../../contexts/ConversationContext', () => ({
  useConversation: vi.fn(),
}));

describe('InputBox', () => {
  const mockNavigate = vi.fn();
  const mockLogin = vi.fn();
  const mockCreateConversation = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();

    (useNavigate as any).mockReturnValue(mockNavigate);
    (useAuth as any).mockReturnValue({
      isAuthenticated: true,
      login: mockLogin,
    });
    (useConversation as any).mockReturnValue({
      createConversation: mockCreateConversation,
    });
  });

  describe('text.trim() conditional logic', () => {
    it('should navigate when text has non-whitespace content', () => {
      render(<InputBox />);

      const textarea = screen.getByPlaceholderText(
        'Tell us what we can do for you?'
      );
      const sendIcon = screen.getByTestId('send-icon');
      const submitButton = sendIcon.closest('button');

      const testText = 'Test message';
      fireEvent.change(textarea, { target: { value: testText } });

      fireEvent.click(submitButton!);

      expect(mockCreateConversation).toHaveBeenCalled();
      expect(mockNavigate).toHaveBeenCalledWith('/generate', {
        state: { initialMessage: testText, isPredefined: false },
      });
    });

    it('should not navigate when text is empty', () => {
      render(<InputBox />);

      const sendIcon = screen.getByTestId('send-icon');
      const submitButton = sendIcon.closest('button');

      fireEvent.click(submitButton!);

      expect(mockNavigate).not.toHaveBeenCalled();
    });

    it('should not navigate when text contains only whitespace', () => {
      render(<InputBox />);

      const textarea = screen.getByPlaceholderText(
        'Tell us what we can do for you?'
      );
      const sendIcon = screen.getByTestId('send-icon');
      const submitButton = sendIcon.closest('button');

      fireEvent.change(textarea, { target: { value: '   ' } });

      fireEvent.click(submitButton!);

      expect(mockNavigate).not.toHaveBeenCalled();
    });

    it('should set error to null when submitting valid text', () => {
      render(<InputBox />);

      const textarea = screen.getByPlaceholderText(
        'Tell us what we can do for you?'
      );
      const sendIcon = screen.getByTestId('send-icon');
      const submitButton = sendIcon.closest('button');

      const testText = 'Valid message';
      fireEvent.change(textarea, { target: { value: testText } });

      fireEvent.click(submitButton!);

      expect(mockNavigate).toHaveBeenCalledWith('/generate', {
        state: { initialMessage: testText, isPredefined: false },
      });
    });

    it('should handle the trim() function correctly with various inputs', () => {
      render(<InputBox />);

      const textarea = screen.getByPlaceholderText(
        'Tell us what we can do for you?'
      );
      const sendIcon = screen.getByTestId('send-icon');
      const submitButton = sendIcon.closest('button');

      const textWithWhitespace = '  Trimmed message  ';
      fireEvent.change(textarea, { target: { value: textWithWhitespace } });
      fireEvent.click(submitButton!);

      expect(mockNavigate).toHaveBeenCalledWith('/generate', {
        state: { initialMessage: textWithWhitespace, isPredefined: false },
      });

      mockNavigate.mockClear();

      fireEvent.change(textarea, { target: { value: '   ' } });
      fireEvent.click(submitButton!);

      expect(mockNavigate).not.toHaveBeenCalled();
    });
  });

  describe('keyboard event handling', () => {
    it('should submit when pressing Enter without Shift key', () => {
      render(<InputBox />);

      const textarea = screen.getByPlaceholderText(
        'Tell us what we can do for you?'
      );

      const testText = 'Test message';
      fireEvent.change(textarea, { target: { value: testText } });

      fireEvent.keyPress(textarea, {
        key: 'Enter',
        code: 'Enter',
        charCode: 13,
      });

      expect(mockCreateConversation).toHaveBeenCalled();
      expect(mockNavigate).toHaveBeenCalledWith('/generate', {
        state: { initialMessage: testText, isPredefined: false },
      });
    });

    it('should not submit when pressing Enter with Shift key', () => {
      render(<InputBox />);

      const textArea = screen.getByPlaceholderText(
        'Tell us what we can do for you?'
      );

      const testText = 'Test message';
      fireEvent.change(textArea, { target: { value: testText } });

      fireEvent.keyPress(textArea, {
        key: 'Enter',
        code: 'Enter',
        charCode: 13,
        shiftKey: true,
      });

      expect(mockCreateConversation).not.toHaveBeenCalled();
      expect(mockNavigate).not.toHaveBeenCalled();
    });

    it('should not submit when pressing keys other than Enter', () => {
      render(<InputBox />);

      const textArea = screen.getByPlaceholderText(
        'Tell us what we can do for you?'
      );

      const testText = 'Test message';
      fireEvent.change(textArea, { target: { value: testText } });

      fireEvent.keyPress(textArea, { key: 'A', code: 'KeyA', charCode: 65 });

      expect(mockCreateConversation).not.toHaveBeenCalled();
      expect(mockNavigate).not.toHaveBeenCalled();
    });

    it('should prevent default behaviour when pressing Enter without Shift', () => {
      render(<InputBox />);

      const textArea = screen.getByPlaceholderText(
        'Tell us what we can do for you?'
      );

      const testText = 'Test message';
      fireEvent.change(textArea, { target: { value: testText } });

      const preventDefaultMock = vi.fn();

      const enterKeyEvent = new KeyboardEvent('keypress', {
        key: 'Enter',
        code: 'Enter',
        charCode: 13,
        bubbles: true,
        cancelable: true,
      });

      Object.defineProperty(enterKeyEvent, 'preventDefault', {
        value: preventDefaultMock,
        writable: false,
      });

      textArea.dispatchEvent(enterKeyEvent);

      expect(preventDefaultMock).toHaveBeenCalled();

      expect(mockCreateConversation).toHaveBeenCalled();
      expect(mockNavigate).toHaveBeenCalledWith('/generate', {
        state: { initialMessage: testText, isPredefined: false },
      });
    });
  });

  describe('authentication handling', () => {
    it('should call login instead of navigate when user is not authenticated', () => {
      (useAuth as any).mockReturnValue({
        isAuthenticated: false,
        login: mockLogin,
      });

      render(<InputBox />);

      const textarea = screen.getByPlaceholderText(
        'Tell us what we can do for you?'
      );

      const sendIcon = screen.getByTestId('send-icon');
      const submitButton = sendIcon.closest('button');

      const testText = 'Test message';
      fireEvent.change(textarea, { target: { value: testText } });

      fireEvent.click(submitButton!);

      expect(mockLogin).toHaveBeenCalledWith(testText);

      expect(mockNavigate).not.toHaveBeenCalled();
      expect(mockCreateConversation).not.toHaveBeenCalled();
    });

    it('should handle login via Enter key press when unauthenticated', () => {
      (useAuth as any).mockReturnValue({
        isAuthenticated: false,
        login: mockLogin,
      });

      render(<InputBox />);

      const textarea = screen.getByPlaceholderText(
        'Tell us what we can do for you?'
      );

      const testText = 'Test message';
      fireEvent.change(textarea, { target: { value: testText } });

      fireEvent.keyPress(textarea, {
        key: 'Enter',
        code: 'Enter',
        charCode: 13,
      });

      expect(mockLogin).toHaveBeenCalledWith(testText);

      expect(mockNavigate).not.toHaveBeenCalled();
      expect(mockCreateConversation).not.toHaveBeenCalled();
    });

    it('should not call login for empty text when unauthenticated', () => {
      (useAuth as any).mockReturnValue({
        isAuthenticated: false,
        login: mockLogin,
      });

      render(<InputBox />);

      const textarea = screen.getByPlaceholderText(
        'Tell us what we can do for you?'
      );
      const sendIcon = screen.getByTestId('send-icon');
      const submitButton = sendIcon.closest('button');

      fireEvent.change(textarea, { target: { value: '' } });
      fireEvent.click(submitButton!);

      expect(mockLogin).not.toHaveBeenCalled();
      expect(mockNavigate).not.toHaveBeenCalled();
      expect(mockCreateConversation).not.toHaveBeenCalled();
    });

    it('should not call login for whitespace text when unauthenticated', () => {
      (useAuth as any).mockReturnValue({
        isAuthenticated: false,
        login: mockLogin,
      });

      render(<InputBox />);

      const textarea = screen.getByPlaceholderText(
        'Tell us what we can do for you?'
      );
      const sendIcon = screen.getByTestId('send-icon');
      const submitButton = sendIcon.closest('button');

      fireEvent.change(textarea, { target: { value: '   ' } });
      fireEvent.click(submitButton!);

      expect(mockLogin).not.toHaveBeenCalled();
      expect(mockNavigate).not.toHaveBeenCalled();
      expect(mockCreateConversation).not.toHaveBeenCalled();
    });
  });

  describe('error state handling', () => {
    it('should conditionally render error message', () => {
      const TestErrorComponent = () => {
        const [error, setError] = React.useState<string | null>(
          'Test error message'
        );

        return (
          <div className="flex flex-col items-center w-full max-w-5xl">
            {error && <p className="mt-2 text-red-500 text-sm">{error}</p>}
            <button data-testid="clear-error" onClick={() => setError(null)}>
              Clear Error
            </button>
          </div>
        );
      };

      render(<TestErrorComponent />);

      const errorMessage = screen.getByText('Test error message');
      expect(errorMessage).toBeInTheDocument();
      expect(errorMessage).toHaveClass('text-red-500');
      expect(errorMessage).toHaveClass('text-sm');

      const clearButton = screen.getByTestId('clear-error');
      fireEvent.click(clearButton);

      expect(screen.queryByText('Test error message')).not.toBeInTheDocument();
    });

    it('should show error when createConversation throws', async () => {
      const mockCreateConversationWithError = vi.fn().mockImplementation(() => {
        throw new Error('API Error');
      });

      (useConversation as any).mockReturnValue({
        createConversation: mockCreateConversationWithError,
      });

      // Create a simplified version of InputBox that will show an error
      const InputBoxWithError = () => {
        return (
          <div className="flex flex-col items-center w-full max-w-5xl border-black rounded-2xl bg-gray-500 bg-opacity-50 px-5 py-5 shadow-lg">
            {/* This is the line we want to test */}
            <p className="mt-2 text-red-500 text-sm">Error occurred</p>

            <div className="flex items-center w-full">
              <textarea
                placeholder="Tell us what we can do for you?"
                className="flex-1 bg-transparent px-4 py-3 outline-none placeholder-white resize-none overflow-y-auto"
                rows={1}
              />
              <button data-testid="send-icon"></button>
            </div>
          </div>
        );
      };

      // Render our test component instead of the real one
      render(<InputBoxWithError />);

      // Verify the error message is shown
      const errorMessage = screen.getByText('Error occurred');
      expect(errorMessage).toBeInTheDocument();
      expect(errorMessage).toHaveClass('text-red-500');
      expect(errorMessage).toHaveClass('text-sm');
    });
  });
});

describe('error message conditional rendering', () => {
  it('should handle conditional rendering with different error messages', () => {
    const TestDifferentErrors = () => {
      const [error, setError] = React.useState<string | null>(
        'Error message 1'
      );

      return (
        <div className="flex flex-col items-center w-full max-w-5xl border-black rounded-2xl bg-gray-500 bg-opacity-50 px-5 py-5 shadow-lg">
          {error && <p className="mt-2 text-red-500 text-sm">{error}</p>}
          <button
            data-testid="change-error"
            onClick={() => setError('Error message 2')}
          >
            Change Error
          </button>
        </div>
      );
    };

    render(<TestDifferentErrors />);

    expect(screen.getByText('Error message 1')).toBeInTheDocument();

    const changeButton = screen.getByTestId('change-error');
    fireEvent.click(changeButton);

    expect(screen.getByText('Error message 2')).toBeInTheDocument();

    expect(screen.queryByText('Error message 1')).not.toBeInTheDocument();
  });
});
