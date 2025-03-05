import { describe, test, expect, vi } from 'vitest';
import {
  Message,
  ChatHookReturn,
  ChatMessageProps,
  PrototypeFrameProps,
  MessagePayload,
} from '../hooks/Types';

describe('Types', () => {
  test('test for a valid Message interface', () => {
    const message: Message = {
      role: 'User',
      content: 'test',
      timestamp: new Date().toISOString(),
    };

    expect(message.role).toBe('User');
    expect(message.content).toBe('test');
    expect(typeof message.timestamp).toBe('string');
  });

  test('test for a valid ChatHookReturn interface', () => {
    const chatHookReturn: ChatHookReturn = {
      message: 'test',
      setMessage: vi.fn(),
      sentMessages: [],
      handleSend: vi.fn().mockResolvedValue(undefined),
      llmResponse: 'Response',
      errorMessage: null,
      setErrorMessage: vi.fn(),
    };

    expect(chatHookReturn.message).toBe('test');
    expect(chatHookReturn.setMessage).toBeInstanceOf(Function);
    expect(chatHookReturn.sentMessages).toEqual([]);
    expect(chatHookReturn.handleSend).toBeInstanceOf(Function);
    expect(chatHookReturn.llmResponse).toBe('Response');
    expect(chatHookReturn.errorMessage).toBeNull();
    expect(chatHookReturn.setErrorMessage).toBeInstanceOf(Function);
  });

  test('test for a valid ChatMessageProps interface', () => {
    const chatMessageProps: ChatMessageProps = {
      setPrototype: vi.fn(),
      setPrototypeId: vi.fn(),
      prototypeId: 1,
    };

    expect(chatMessageProps.setPrototype).toBeInstanceOf(Function);
    expect(chatMessageProps.setPrototypeId).toBeInstanceOf(Function);
    expect(chatMessageProps.prototypeId).toBe(1);
  });

  test('test for a valid PrototypeFrameProps interface', () => {
    const prototypeFrameProps: PrototypeFrameProps = {
      prototypeId: 1,
      width: '100px',
      height: '100px',
    };

    expect(prototypeFrameProps.prototypeId).toBe(1);
    expect(prototypeFrameProps.width).toBe('100px');
    expect(prototypeFrameProps.height).toBe('100px');
  });

  test('test for a valid MessagePayload interface', () => {
    const messagePayload: MessagePayload = {
      userID: '1',
      time: new Date().toISOString(),
      prompt: 'test',
    };

    expect(messagePayload.userID).toBe('1');
    expect(typeof messagePayload.time).toBe('string');
    expect(messagePayload.prompt).toBe('test');
  });
});
