import { describe, it, vi, expect, afterEach } from 'vitest';
import { sendChatMessage } from '@/api/FrontEndAPI';
import { Message, ChatResponse, PrototypeResponse } from '@/types/Types';

global.fetch = vi.fn();

describe('sendChatMessage', () => {
    it('should call onChatResponse and onPrototypeResponse when the API call is successful', async () => {
        const mockMessage: Message = {
            role: 'User',  
            content: 'Test message',
            timestamp: new Date().toISOString()
        };

        const mockChatResponse: ChatResponse = {
            message: 'Chat response text',
            role: 'LLM',  
            timestamp: new Date().toISOString() 
        };
        const mockPrototypeResponse: PrototypeResponse = {
            files: {} 
        };
        
        const mockServerResponse = { chat: mockChatResponse, prototype: mockPrototypeResponse };

        vi.stubGlobal("fetch", vi.fn().mockResolvedValue({
            ok: true,
            json: async () => mockServerResponse
        }));

        const onChatResponse = vi.fn();
        const onPrototypeResponse = vi.fn();

        await sendChatMessage(mockMessage, onChatResponse, onPrototypeResponse);

        expect(onChatResponse).toHaveBeenCalledWith(mockChatResponse);
        expect(onPrototypeResponse).toHaveBeenCalledWith(mockPrototypeResponse);

        expect(fetch).toHaveBeenCalledWith("http://localhost:8000/api/chat/json", expect.objectContaining({
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                userID: "user123",
                time: mockMessage.timestamp,
                prompt: mockMessage.content
            })
        }));
    });

    afterEach(() => {
        vi.restoreAllMocks();
    });

    
    it('should throw an error when the network response is not ok', async () => {
        vi.stubGlobal("fetch", vi.fn().mockResolvedValue({ ok: false }));
    
        const mockMessage: Message = {
            role: 'User', 
            content: 'Test message',
            timestamp: new Date().toISOString()
        };
    
        await expect(sendChatMessage(mockMessage, vi.fn(), vi.fn()))
            .rejects.toThrow('Network response was not ok');
    
        vi.restoreAllMocks(); 
    });
    
    
    it('should throw an error when fetch fails', async () => {
        vi.stubGlobal("fetch", vi.fn().mockRejectedValue(new Error('Fetch failed')));
    
        const mockMessage: Message = {
            role: 'User',
            content: 'Test message',
            timestamp: new Date().toISOString()
        };
    
        await expect(sendChatMessage(mockMessage, vi.fn(), vi.fn()))
            .rejects.toThrow('Fetch failed');
    
        vi.restoreAllMocks();
    });
    
});
