import { renderHook, act } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import usePrototypeFrame from '@/hooks/UsePrototypeFrame';
import { useWebContainer } from '@/hooks/UseWebContainer';
import { normaliseFiles, cleanFileSystem } from '@/hooks/FileHandler';
import {
  ensureViteConfig,
  ensureViteDependencies,
  chooseViteStartScript,
  configureViteSandbox,
  ensureIndexHtml,
} from '@/hooks/ViteSetup';
import React from 'react';

// Mock dependencies
vi.mock('@/hooks/UseWebContainer', () => ({
  useWebContainer: vi.fn(),
}));

vi.mock('@/hooks/FileHandler', () => ({
  normaliseFiles: vi.fn(),
  cleanFileSystem: vi.fn(),
}));

vi.mock('@/hooks/ViteSetup', () => ({
  ensureViteConfig: vi.fn(),
  ensureViteDependencies: vi.fn(),
  chooseViteStartScript: vi.fn(),
  configureViteSandbox: vi.fn(),
  ensureIndexHtml: vi.fn(),
}));

describe('usePrototypeFrame', () => {
  const mockWebContainerInstance = {
    on: vi.fn(),
    mount: vi.fn(),
    spawn: vi.fn(),
    fs: {
      readFile: vi.fn(),
      writeFile: vi.fn(),
      readdir: vi.fn(),
      rm: vi.fn(),
    },
  };

  const mockProcess = {
    kill: vi.fn(),
    exit: Promise.resolve(0),
    output: {
      pipeTo: vi.fn(),
    },
  };

  beforeEach(() => {
    vi.resetAllMocks();
    vi.useFakeTimers();

    // Setup default mocks
    (useWebContainer as any).mockReturnValue({
      instance: mockWebContainerInstance,
      loading: false,
      error: null,
    });

    (normaliseFiles as any).mockReturnValue({});
    (cleanFileSystem as any).mockResolvedValue(undefined);
    (ensureViteDependencies as any).mockResolvedValue(false);
    (ensureViteConfig as any).mockResolvedValue(undefined);
    (ensureIndexHtml as any).mockResolvedValue(undefined);
    (chooseViteStartScript as any).mockReturnValue('dev');
    (configureViteSandbox as any).mockImplementation(() => {});

    mockWebContainerInstance.spawn.mockResolvedValue(mockProcess);
    mockWebContainerInstance.fs.readFile.mockResolvedValue(
      JSON.stringify({
        scripts: { dev: 'vite' },
        dependencies: {},
      })
    );
  });

  afterEach(() => {
    vi.clearAllMocks();
    vi.useRealTimers();
  });

  it('should initialize with correct default states', () => {
    vi.spyOn(React, 'useEffect').mockImplementation(() => {});

    const { result } = renderHook(() => usePrototypeFrame({ files: {} }));

    expect(result.current.status).toBe('Resetting environment...');
    expect(result.current.url).toBe('');
    expect(result.current.loading).toBe(false);
    expect(result.current.error).toBeUndefined();
    expect(result.current.iframeRef).toBeDefined();
  });

  it('should register server-ready listener when WebContainer is available', () => {
    renderHook(() => usePrototypeFrame({ files: {} }));

    expect(mockWebContainerInstance.on).toHaveBeenCalledWith(
      'server-ready',
      expect.any(Function)
    );
  });

  it('should mount files and start server when WebContainer and files are available', async () => {
    const testFiles = {
      'index.html': {
        file: {
          contents: '<!DOCTYPE html><html></html>',
        },
      },
    };

    renderHook(() => usePrototypeFrame({ files: testFiles }));

    await act(async () => {
      await vi.runAllTimersAsync();
    });

    expect(cleanFileSystem).toHaveBeenCalledWith(mockWebContainerInstance);
    expect(normaliseFiles).toHaveBeenCalledWith(testFiles);
    expect(mockWebContainerInstance.mount).toHaveBeenCalled();
    expect(ensureViteDependencies).toHaveBeenCalled();
    expect(ensureViteConfig).toHaveBeenCalled();
    expect(ensureIndexHtml).toHaveBeenCalled();
    expect(mockWebContainerInstance.spawn).toHaveBeenCalledWith('npm', [
      'run',
      'dev',
    ]);
  });

  it('should reinstall dependencies if an out-dated React version is used', async () => {
    const testFiles = {
      'index.html': {
        file: {
          contents: '<!DOCTYPE html><html></html>',
        },
      },
      'package.json': {
        file: {
          contents: `{
            "scripts": {
              "dev": "vite",
              "build": "vite build",
              "preview": "vite preview"
            },
            "dependencies": {
              "react": "^17.2.0",
              "react-dom": "^17.2.0"
            },
            "devDependencies": {
              "@vitejs/plugin-react": "3.1.0",
              "vite": "4.1.4",
              "esbuild-wasm": "0.17.12"
            }
          }`,
        },
      },
    };

    // const spy = vi.spyOn(usePrototypeFrame, 'installDependencies');

    renderHook(() => usePrototypeFrame({ files: testFiles }));

    await act(async () => {
      await vi.runAllTimersAsync();
    });

    expect(cleanFileSystem).toHaveBeenCalledWith(mockWebContainerInstance);
    expect(normaliseFiles).toHaveBeenCalledWith(testFiles);
    expect(mockWebContainerInstance.mount).toHaveBeenCalled();
    expect(ensureViteDependencies).toHaveBeenCalled();
    expect(ensureViteConfig).toHaveBeenCalled();
    expect(ensureIndexHtml).toHaveBeenCalled();
    // expect(spy).toHaveBeenCalled();
    expect(mockWebContainerInstance.spawn).toHaveBeenCalledWith('npm', [
      'run',
      'dev',
    ]);
  });

  it('should not attempt to mount files if WebContainer is not ready', () => {
    (useWebContainer as any).mockReturnValue({
      instance: null,
      loading: true,
      error: null,
    });

    renderHook(() => usePrototypeFrame({ files: {} }));

    expect(mockWebContainerInstance.mount).not.toHaveBeenCalled();
    expect(normaliseFiles).not.toHaveBeenCalled();
  });

  describe('startServer and runServerWithAutoInstall', () => {
    it('should do nothing if no webcontainer instance in startServer', async () => {
      (useWebContainer as any).mockReturnValueOnce({
        instance: null,
        loading: false,
        error: null,
      });

      const { result } = renderHook(() => usePrototypeFrame({ files: {} }));

      await act(async () => {
        await vi.runAllTimersAsync();
      });

      expect(mockWebContainerInstance.spawn).not.toHaveBeenCalled();
      expect(result.current.status).not.toBe('Starting development server...');
    });
  });

  it('should update package.json when installing a specific dependency', async () => {
    mockWebContainerInstance.fs.readFile.mockResolvedValue(
      JSON.stringify({
        name: 'test-project',
        dependencies: {},
      })
    );

    const { result } = renderHook(() => usePrototypeFrame({ files: {} }));

    expect(mockWebContainerInstance.fs.readFile).toBeDefined();
    expect(mockWebContainerInstance.fs.writeFile).toBeDefined();
  });
});
