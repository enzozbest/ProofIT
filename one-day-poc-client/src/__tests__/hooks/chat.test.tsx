import { renderHook, act } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import ChatMessage from '../../hooks/Chat';

describe('ChatMessage Hook', () => {
    const mockSetPrototype = vi.fn();
    const mockSetPrototypeId = vi.fn();
    const defaultProps = {
        setPrototype: mockSetPrototype,
        setPrototypeId: mockSetPrototypeId,
        prototypeId: 0
    };

    beforeEach(() => {
        vi.clearAllMocks();
        global.fetch = vi.fn();
    });

    it('should initialize with default values', () => {
        const { result } = renderHook(() => ChatMessage(defaultProps));
        
        expect(result.current.message).toBe('');
        expect(result.current.sentMessages).toEqual([]);
        expect(result.current.errorMessage).toBeNull();
        expect(result.current.llmResponse).toBe('');
    });

    it('should update message when setMessage is called', () => {
        const { result } = renderHook(() => ChatMessage(defaultProps));
        
        act(() => {
            result.current.setMessage('test message');
        });
        
        expect(result.current.message).toBe('test message');
    });

    it('should handle successful message send', async () => {
        const mockResponse = 'LLM response';
        vi.fn().mockResolvedValueOnce({
            ok: true,
            text: () => Promise.resolve(mockResponse),
        });

        const { result } = renderHook(() => ChatMessage(defaultProps));
        
        await act(async () => {
            await result.current.handleSend('test message');
        });

        expect(result.current.sentMessages).toHaveLength(1);
        expect(result.current.sentMessages[0].content).toBe('test message');
        expect(mockSetPrototype).toHaveBeenCalledWith(true);
        expect(mockSetPrototypeId).toHaveBeenCalledWith(1);
    });

    it('should handle network error', async () => {
        vi.fn().mockRejectedValueOnce(new Error("Network error"));

        const { result } = renderHook(() => ChatMessage(defaultProps));
        
        await act(async () => {
            await result.current.handleSend('test message');
        });

        expect(result.current.errorMessage).toBe('Error. Please check your connection and try again.');
    });
});