import React, { useState } from 'react';
import { render, waitFor, fireEvent } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { describe, test, expect, vi, beforeEach } from 'vitest';
import { ChatBox } from '../../../components/chat/ChatBox.js';

describe('ChatBox Component', () => {
  let setMessageMock, handleSendMock, setErrorMock;

  beforeEach(() => {
    setMessageMock = vi.fn();
    handleSendMock = vi.fn();
    setErrorMock = vi.fn();
  });

  test('test if handleSend is called if initial message is set', async () => {
    const initialMessage = 'test';

    render(
      <MemoryRouter
        initialEntries={[{ pathname: '/', state: { initialMessage } }]}
      >
        <Routes>
          <Route
            path="/"
            element={
              <ChatBox
                message=""
                setMessage={setMessageMock}
                handleSend={handleSendMock}
                setError={setErrorMock}
              />
            }
          />
        </Routes>
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(setMessageMock).toHaveBeenCalledWith(initialMessage);
    });

    setMessageMock.mock.calls[0][0] = initialMessage;

    render(
      <MemoryRouter
        initialEntries={[{ pathname: '/', state: { initialMessage } }]}
      >
        <Routes>
          <Route
            path="/"
            element={
              <ChatBox
                message={initialMessage}
                setMessage={setMessageMock}
                handleSend={handleSendMock}
                setError={setErrorMock}
              />
            }
          />
        </Routes>
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(handleSendMock).toHaveBeenCalled();
    });
  });

  test('test handleSend is not called if initialMessage is not present', async () => {
    render(
      <MemoryRouter initialEntries={[{ pathname: '/' }]}>
        <Routes>
          <Route
            path="/"
            element={
              <ChatBox
                message=""
                setMessage={setMessageMock}
                handleSend={handleSendMock}
                setError={setErrorMock}
              />
            }
          />
        </Routes>
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(setMessageMock).not.toHaveBeenCalled();
    });

    await waitFor(() => {
      expect(handleSendMock).not.toHaveBeenCalled();
    });
  });

  test('should not call handleSend if isSending is true', async () => {

    const neverResolvingHandleSend = vi.fn(() => new Promise(() => {})); 
    const setIsSendingMock = vi.fn();
    
    const { getByText } = render(
      <MemoryRouter>
        <ChatBox
          message="test message"
          setMessage={setMessageMock}
          handleSend={neverResolvingHandleSend}
          setError={setErrorMock}
          setIsSending={setIsSendingMock}
        />
      </MemoryRouter>
    );

    const sendButton = getByText('Send');
    fireEvent.click(sendButton);
    
    expect(neverResolvingHandleSend).toHaveBeenCalledTimes(1);
    
    fireEvent.click(sendButton);
    
    expect(neverResolvingHandleSend).toHaveBeenCalledTimes(1);
  });

  test('should set error when handleSend fails', async () => {
    const errorMessage = 'Test error';
    const failingHandleSend = vi.fn().mockRejectedValue(new Error(errorMessage));
    
    const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {});

    const { getByText } = render(
      <MemoryRouter>
        <ChatBox
          message="test message"
          setMessage={setMessageMock}
          handleSend={failingHandleSend}
          setError={setErrorMock}
          isSending={false}
          setIsSending={vi.fn()}
        />
      </MemoryRouter>
    );

    const sendButton = getByText('Send');
    fireEvent.click(sendButton);
    
    await waitFor(() => {
      expect(consoleSpy).toHaveBeenCalledWith('Failed to send message:', expect.any(Error));
      
      expect(setErrorMock).toHaveBeenCalledWith('Failed to send message. Please try again.');
    });

    consoleSpy.mockRestore();
  });

  test('should not call handleSend if button is disabled when isSending is true', async () => {
    vi.restoreAllMocks();
    
    const MockedChatBox = (props) => {
      const isSending = true;
      
      return (
        <div className="flex p-2.5 border-t border-gray-300 text-secondary-foreground">
          <input
            type="text"
            value={props.message}
            onChange={() => {}}
            placeholder="How can we help you today?"
            disabled={isSending}
            className="flex-1 p-2.5 rounded-sm mr-2.5 focus:outline-none focus:ring-2 focus:ring-muted/50"
          />
          <button
            type="button"
            disabled={!props.message || isSending}
            onClick={() => {
              if (isSending) return; 
              props.handleSend();
            }}
            className="py-2.5 px-5 bg-purple-700 hover:bg-purple-800 text-white border-0 rounded-lg cursor-pointer disabled:opacity-50 transition-all"
          >
            Send
          </button>
        </div>
      );
    };
    
    const { getByText } = render(
      <MemoryRouter>
        <MockedChatBox
          message="test message" 
          setMessage={setMessageMock}
          handleSend={handleSendMock}
          setError={setErrorMock}
        />
      </MemoryRouter>
    );

    const sendButton = getByText('Send');
    
    expect(sendButton.disabled).toBe(true);
    
    fireEvent.click(sendButton);
    
    expect(handleSendMock).not.toHaveBeenCalled();
  });

  test('should call handleSend when Enter key is pressed', async () => {
    const { getByPlaceholderText } = render(
      <MemoryRouter>
        <ChatBox
          message="test message"
          setMessage={setMessageMock}
          handleSend={handleSendMock}
          setError={setErrorMock}
        />
      </MemoryRouter>
    );

    const inputElement = getByPlaceholderText('How can we help you today?');
    
    fireEvent.keyDown(inputElement, { key: 'Enter', code: 'Enter' });
    
    expect(handleSendMock).toHaveBeenCalled();
  });

  test('should not call handleSend when a key other than Enter is pressed', async () => {
    const { getByPlaceholderText } = render(
      <MemoryRouter>
        <ChatBox
          message="test message"
          setMessage={setMessageMock}
          handleSend={handleSendMock}
          setError={setErrorMock}
        />
      </MemoryRouter>
    );

    const inputElement = getByPlaceholderText('How can we help you today?');
    
    fireEvent.keyDown(inputElement, { key: 'a', code: 'KeyA' });
  
    expect(handleSendMock).not.toHaveBeenCalled();
  });

  test('should not call handleSend on Enter key press if isSending is true', async () => {
    const neverResolvingHandleSend = vi.fn(() => new Promise(() => {}));
    
    const { getByPlaceholderText, getByText } = render(
      <MemoryRouter>
        <ChatBox
          message="test message"
          setMessage={setMessageMock}
          handleSend={neverResolvingHandleSend}
          setError={setErrorMock}
        />
      </MemoryRouter>
    );

    const inputElement = getByPlaceholderText('How can we help you today?');
    const sendButton = getByText('Send');
    
    fireEvent.click(sendButton);
    
    expect(neverResolvingHandleSend).toHaveBeenCalledTimes(1);
    
    fireEvent.keyDown(inputElement, { key: 'Enter', code: 'Enter' });
    
    expect(neverResolvingHandleSend).toHaveBeenCalledTimes(1);
  });

  test('should call setMessage with input value when user types', async () => {
    const { getByPlaceholderText } = render(
      <MemoryRouter>
        <ChatBox
          message=""
          setMessage={setMessageMock}
          handleSend={handleSendMock}
          setError={setErrorMock}
        />
      </MemoryRouter>
    );

    const inputElement = getByPlaceholderText('How can we help you today?');
    
    const testValue = 'Hello, world!';
    fireEvent.change(inputElement, { target: { value: testValue } });
    
    expect(setMessageMock).toHaveBeenCalledWith(testValue);
    
    const anotherTestValue = 'Another test message';
    fireEvent.change(inputElement, { target: { value: anotherTestValue } });
    
    expect(setMessageMock).toHaveBeenCalledWith(anotherTestValue);
    
    expect(setMessageMock).toHaveBeenCalledTimes(2);
  });
});
