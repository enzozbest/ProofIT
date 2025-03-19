import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { describe, test, expect, vi, beforeEach } from 'vitest';
import { ChatBox } from '../../../components/chat/chat-box.js';

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
});
