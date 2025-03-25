import { renderHook, waitFor } from '@testing-library/react';
import { useWebContainer } from '@/hooks/UseWebContainer';
import { WebContainer } from '@webcontainer/api';
import { vi, describe, it, expect, beforeEach, afterEach } from 'vitest';

vi.mock('@webcontainer/api', () => {
  const mockWebContainer = {};

  return {
    WebContainer: {
      boot: vi.fn().mockImplementation(() => Promise.resolve(mockWebContainer)),
    },
  };
});

Object.defineProperty(window, 'crossOriginIsolated', {
  writable: true,
  value: true,
});

describe('useWebContainer', () => {
  let consoleLogSpy: any;
  let consoleErrorSpy: any;

  beforeEach(() => {
    vi.resetModules();

    vi.mocked(WebContainer.boot).mockClear();

    consoleLogSpy = vi.spyOn(console, 'log').mockImplementation(() => {});
    consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {});

    window.crossOriginIsolated = true;
  });

  afterEach(() => {
    consoleLogSpy.mockRestore();
    consoleErrorSpy.mockRestore();
  });

  it('should initialize WebContainer on first call when none exists', async () => {
    const { result } = renderHook(() => useWebContainer());

    expect(result.current.loading).toBe(true);
    expect(result.current.instance).toBeNull();
    expect(result.current.error).toBeNull();
    expect(result.current.isReady).toBe(false);

    expect(WebContainer.boot).toHaveBeenCalledTimes(1);
    expect(consoleLogSpy).toHaveBeenCalledWith(
      'Starting WebContainer boot process'
    );

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
      expect(result.current.instance).not.toBeNull();
      expect(result.current.isReady).toBe(true);
    });

    expect(consoleLogSpy).toHaveBeenCalledWith(
      'WebContainer booted successfully'
    );
  });

  it('should reuse existing WebContainer instance if available', async () => {
    const { result: firstResult } = renderHook(() => useWebContainer());

    await waitFor(() => expect(firstResult.current.loading).toBe(false));

    vi.mocked(WebContainer.boot).mockClear();
    consoleLogSpy.mockClear();

    const { result: secondResult } = renderHook(() => useWebContainer());

    await waitFor(() => expect(secondResult.current.loading).toBe(false));

    expect(WebContainer.boot).not.toHaveBeenCalled();

    expect(secondResult.current.instance).toBe(firstResult.current.instance);
  });

  it('should show an error if cross-origin isolation is not enabled', async () => {
    window.crossOriginIsolated = false;

    const { result } = renderHook(() => useWebContainer());

    await waitFor(() => expect(result.current.loading).toBe(false));

    expect(result.current.error).toBeInstanceOf(Error);
    expect(result.current.error?.message).toContain(
      'Cross-Origin Isolation is not enabled'
    );
    expect(result.current.instance).toBeNull();
    expect(result.current.isReady).toBe(false);

    expect(WebContainer.boot).not.toHaveBeenCalled();
  });
});
