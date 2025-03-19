import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import ChatMessage from '../../hooks/Chat';
import { sendChatMessage } from '../../api/FrontEndAPI';

vi.mock('../../api/FrontEndAPI', () => ({
  sendChatMessage: vi.fn(),
}));

const WrapperComponent = ({ mockSetPrototype = () => {}, mockSetPrototypeFiles = () => {} }) => {
  const {
    message,
    setMessage,
    sentMessages,
    handleSend,
    errorMessage,
  } = ChatMessage({
    setPrototype: mockSetPrototype,
    setPrototypeFiles: mockSetPrototypeFiles,
  });

  return (
    <div>
      <input
        type="text"
        value={message}
        onChange={(e) => setMessage(e.target.value)}
        placeholder="Type a message"
      />
      <button onClick={() => handleSend()}>Send</button>
      {sentMessages.map((msg, index) => (
        <div key={index}>
          <p>{msg.content}</p>
          <span>{msg.timestamp}</span>
        </div>
      ))}
      {errorMessage && <p>{errorMessage}</p>}
    </div>
  );
};

describe('ChatMessage Hook', () => {
  it('renders sent messages', () => {
    render(<WrapperComponent />);

    const input = screen.getByPlaceholderText('Type a message');
    fireEvent.change(input, { target: { value: 'Hello, world!' } });

    const button = screen.getByText('Send');
    fireEvent.click(button);

    expect(screen.getByText('Hello, world!')).toBeInTheDocument();
  });

  it('does not send a message if it is empty or contains only whitespace', () => {
    render(<WrapperComponent />);
  
    const input = screen.getByPlaceholderText('Type a message');
    fireEvent.change(input, { target: { value: '   ' } }); // Only whitespace
  
    const button = screen.getByRole('button', { name: 'Send' });
    fireEvent.click(button);
  
    expect(screen.queryByText('   ')).not.toBeInTheDocument();
  });

  it('renders an error message when sending fails', async () => {
    (sendChatMessage as unknown as ReturnType<typeof vi.fn>).mockRejectedValueOnce(new Error('Network error'));

    render(<WrapperComponent />);

    const input = screen.getByPlaceholderText('Type a message');
    fireEvent.change(input, { target: { value: 'Hello, world!' } });

    const button = screen.getByText('Send');
    fireEvent.click(button);

    await waitFor(() => {
      expect(screen.getByText('Error. Please check your connection and try again.')).toBeInTheDocument();
    });
  });

  it('handles LLM response correctly', async () => {
    (sendChatMessage as unknown as ReturnType<typeof vi.fn>).mockImplementationOnce((_, onChatResponse) => {
      onChatResponse({ message: 'LLM response' });
    });

    render(<WrapperComponent />);

    const input = screen.getByPlaceholderText('Type a message');
    fireEvent.change(input, { target: { value: 'Hello, world!' } });

    const button = screen.getByText('Send');
    fireEvent.click(button);

    await waitFor(() => {
      expect(screen.getByText('LLM response')).toBeInTheDocument();
    });
  });

  it('calls setPrototype and setPrototypeFiles correctly', async () => {
    const mockSetPrototype = vi.fn();
    const mockSetPrototypeFiles = vi.fn();

    (sendChatMessage as unknown as ReturnType<typeof vi.fn>).mockImplementationOnce((_, __, onPrototypeResponse) => {
      onPrototypeResponse({ files: ['file1', 'file2'] });
    });

    render(<WrapperComponent mockSetPrototype={mockSetPrototype} mockSetPrototypeFiles={mockSetPrototypeFiles} />);

    const input = screen.getByPlaceholderText('Type a message');
    fireEvent.change(input, { target: { value: 'Hello, world!' } });

    const button = screen.getByText('Send');
    fireEvent.click(button);

    await waitFor(() => {
      expect(mockSetPrototype).toHaveBeenCalledWith(true);
      expect(mockSetPrototypeFiles).toHaveBeenCalledWith(['file1', 'file2']);
    });
  });
});