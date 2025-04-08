import {
  describe,
  it,
  vi,
  expect,
  afterEach,
  beforeEach,
  afterAll,
} from 'vitest';
import { getPrototypeForMessage } from '@/api/PrototypeAPI';
import { FileTree } from '@/types/Types';

const originalConsoleError = console.error;
const originalConsoleLog = console.log;
console.error = vi.fn();
console.log = vi.fn();

global.fetch = vi.fn();

describe('getPrototypeForMessage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    (console.error as any).mockClear();
    (console.log as any).mockClear();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('should return null and log error if either conversationId or messageId is missing', async () => {
    const result1 = await getPrototypeForMessage('', 'some-message-id');
    expect(result1).toBeNull();
    expect(console.error).toHaveBeenCalledWith(
      'Error fetching prototype:',
      expect.any(Error),
      'For IDs:',
      { conversationId: '', messageId: 'some-message-id' }
    );

    (console.error as any).mockClear();

    const result2 = await getPrototypeForMessage('some-conversation-id', '');
    expect(result2).toBeNull();
    expect(console.error).toHaveBeenCalledWith(
      'Error fetching prototype:',
      expect.any(Error),
      'For IDs:',
      { conversationId: 'some-conversation-id', messageId: '' }
    );
  });

  it('should fetch and return the file tree on successful API call', async () => {
    const mockFileTree = { 'file1.ts': '// content', 'file2.jsx': '<Component />' };
    const mockApiResponse = {
      files: JSON.stringify(mockFileTree)
    };

    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({
        ok: true,
        json: async () => mockApiResponse,
      })
    );

    const result = await getPrototypeForMessage('valid-conversation-id', 'valid-message-id');

    expect(result).toEqual(mockFileTree);
    expect(fetch).toHaveBeenCalledWith(
      'http://localhost:8000/api/chat/history/valid-conversation-id/valid-message-id',
      {
        method: 'GET',
        credentials: 'include',
        headers: { 'Content-Type': 'application/json' },
      }
    );

    expect(console.log).toHaveBeenCalledWith('Fetching prototype with:', {
      conversationId: 'valid-conversation-id',
      messageId: 'valid-message-id'
    });

    expect(console.log).toHaveBeenCalledWith('Successfully fetched prototype for:', {
      conversationId: 'valid-conversation-id',
      messageId: 'valid-message-id'
    });
  });

  it('should return null when API response is not ok', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({
        ok: false,
        status: 404,
      })
    );

    const result = await getPrototypeForMessage('invalid-conversation-id', 'invalid-message-id');

    expect(result).toBeNull();
    expect(console.error).toHaveBeenCalledWith(
      'Request failed for:',
      { conversationId: 'invalid-conversation-id', messageId: 'invalid-message-id' },
      'Status:',
      404
    );
    expect(console.error).toHaveBeenCalledWith(
      'Error fetching prototype:',
      expect.any(Error),
      'For IDs:',
      { conversationId: 'invalid-conversation-id', messageId: 'invalid-message-id' }
    );
  });

  it('should return null when network error occurs', async () => {
    const networkError = new Error('Network failure');
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(networkError));

    const result = await getPrototypeForMessage('test-conversation-id', 'test-message-id');

    expect(result).toBeNull();
    expect(console.error).toHaveBeenCalledWith(
      'Error fetching prototype:',
      networkError,
      'For IDs:',
      { conversationId: 'test-conversation-id', messageId: 'test-message-id' }
    );
  });

  it('should handle parsing errors in the response', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({
        ok: true,
        json: async () => ({ files: 'not-valid-json' }), 
      })
    );

    const result = await getPrototypeForMessage('valid-id', 'valid-msg-id');

    expect(result).toBeNull();
    expect(console.error).toHaveBeenCalledWith(
      'Error fetching prototype:',
      expect.any(Error),
      'For IDs:',
      { conversationId: 'valid-id', messageId: 'valid-msg-id' }
    );
  });
});

afterAll(() => {
  console.error = originalConsoleError;
  console.log = originalConsoleLog;
});