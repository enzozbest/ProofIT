import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import ChatMessage from '../../hooks/Chat';
import { sendChatMessage } from '../../api/FrontEndAPI';

// Mock the `sendChatMessage` function using Vitest
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

    // Simulate typing a message
    const input = screen.getByPlaceholderText('Type a message');
    fireEvent.change(input, { target: { value: 'Hello, world!' } });

    // Simulate clicking the send button
    const button = screen.getByText('Send');
    fireEvent.click(button);

    // Check if the message is rendered
    expect(screen.getByText('Hello, world!')).toBeInTheDocument();
  });

  it('does not send a message if it is empty or contains only whitespace', () => {
    render(<WrapperComponent />);
  
    // Simulate typing an empty message
    const input = screen.getByPlaceholderText('Type a message');
    fireEvent.change(input, { target: { value: '   ' } }); // Only whitespace
  
    // Simulate clicking the send button
    const button = screen.getByRole('button', { name: 'Send' });
    fireEvent.click(button);
  
    // Check that no message is added to the sentMessages list
    expect(screen.queryByText('   ')).not.toBeInTheDocument();
  });

  it('renders an error message when sending fails', async () => {
    // Mock `sendChatMessage` to throw an error
    (sendChatMessage as unknown as ReturnType<typeof vi.fn>).mockRejectedValueOnce(new Error('Network error'));

    render(<WrapperComponent />);

    // Simulate typing a message
    const input = screen.getByPlaceholderText('Type a message');
    fireEvent.change(input, { target: { value: 'Hello, world!' } });

    // Simulate clicking the send button
    const button = screen.getByText('Send');
    fireEvent.click(button);

    // Wait for the error message to appear
    await waitFor(() => {
      expect(screen.getByText('Error. Please check your connection and try again.')).toBeInTheDocument();
    });
  });

  it('handles LLM response correctly', async () => {
    // Mock `sendChatMessage` to call the LLM response callback
    (sendChatMessage as unknown as ReturnType<typeof vi.fn>).mockImplementationOnce((_, onChatResponse) => {
      onChatResponse({ message: 'LLM response' });
    });

    render(<WrapperComponent />);

    // Simulate typing a message
    const input = screen.getByPlaceholderText('Type a message');
    fireEvent.change(input, { target: { value: 'Hello, world!' } });

    // Simulate clicking the send button
    const button = screen.getByText('Send');
    fireEvent.click(button);

    // Wait for the LLM response to be rendered
    await waitFor(() => {
      expect(screen.getByText('LLM response')).toBeInTheDocument();
    });
  });

  it('calls setPrototype and setPrototypeFiles correctly', async () => {
    const mockSetPrototype = vi.fn();
    const mockSetPrototypeFiles = vi.fn();

    // Mock `sendChatMessage` to call the prototype response callback
    (sendChatMessage as unknown as ReturnType<typeof vi.fn>).mockImplementationOnce((_, __, onPrototypeResponse) => {
      onPrototypeResponse({ files: ['file1', 'file2'] });
    });

    render(<WrapperComponent mockSetPrototype={mockSetPrototype} mockSetPrototypeFiles={mockSetPrototypeFiles} />);

    // Simulate typing a message
    const input = screen.getByPlaceholderText('Type a message');
    fireEvent.change(input, { target: { value: 'Hello, world!' } });

    // Simulate clicking the send button
    const button = screen.getByText('Send');
    fireEvent.click(button);

    // Wait for the callbacks to be called
    await waitFor(() => {
      expect(mockSetPrototype).toHaveBeenCalledWith(true);
      expect(mockSetPrototypeFiles).toHaveBeenCalledWith(['file1', 'file2']);
    });
  });
});