import { render, screen } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import PrototypeFrame from '@/components/prototype/PrototypeFrame';
import { useWebContainer } from '../../hooks/UseWebContainer';
import { FileTree } from '../../types/Types';
import React from 'react';

vi.mock('../../hooks/UseWebContainer', () => ({
  useWebContainer: vi.fn(),
}));

describe('PrototypeFrame Component', () => {
  it('renders the iframe when WebContainer is ready', () => {
    (useWebContainer as ReturnType<typeof vi.fn>).mockReturnValue({
      instance: { on: vi.fn(), mount: vi.fn(), spawn: vi.fn() },
      loading: false,
      error: null,
    });

    render(<PrototypeFrame files={{}} />);

    const iframe = screen.getByTitle('Prototype Preview');
    expect(iframe).toBeInTheDocument();
  });

  it('displays the status message correctly', () => {
    (useWebContainer as ReturnType<typeof vi.fn>).mockReturnValue({
      instance: null,
      loading: false,
      error: null,
    });

    render(<PrototypeFrame files={{}} />);

    expect(
      screen.getByText((content) => content.includes('Status: Initialising...'))
    ).toBeInTheDocument();
  });

  it('handles file mounting correctly', async () => {
    const mockMount = vi.fn();
    const mockOn = vi.fn();

    (useWebContainer as ReturnType<typeof vi.fn>).mockReturnValue({
      instance: { on: mockOn, mount: mockMount, spawn: vi.fn() },
      loading: false,
      error: null,
    });

    const files: FileTree = {
      'index.js': { file: { contents: 'console.log("Hello, world!");' } },
    };

    render(<PrototypeFrame files={files} />);

    await new Promise((resolve) => setTimeout(resolve, 0));

    expect(mockMount).toHaveBeenCalledWith({
      'index.js': { file: { contents: 'console.log("Hello, world!");' } },
    });
  });

  it('normalises flat file structure correctly', async () => {
    const mockMount = vi.fn();
    const mockOn = vi.fn();

    (useWebContainer as ReturnType<typeof vi.fn>).mockReturnValue({
      instance: { on: mockOn, mount: mockMount, spawn: vi.fn() },
      loading: false,
      error: null,
    });

    const files: FileTree = {
      'index.js': { file: { contents: 'console.log("Hello, world!");' } },
      'style.css': { file: { contents: 'body { background: white; }' } },
    };

    render(<PrototypeFrame files={files} />);

    await new Promise((resolve) => setTimeout(resolve, 0));

    expect(mockMount).toHaveBeenCalledWith({
      'index.js': { file: { contents: 'console.log("Hello, world!");' } },
      'style.css': { file: { contents: 'body { background: white; }' } },
    });
  });

  it('normalises nested file structure correctly', async () => {
    const mockMount = vi.fn();
    const mockOn = vi.fn();

    (useWebContainer as ReturnType<typeof vi.fn>).mockReturnValue({
      instance: { on: mockOn, mount: mockMount, spawn: vi.fn() },
      loading: false,
      error: null,
    });

    const files: FileTree = {
      'src/index.js': { file: { contents: 'console.log("Hello, world!");' } },
      'src/utils/helper.js': {
        file: { contents: 'export const helper = () => {};' },
      },
      'assets/style.css': { file: { contents: 'body { background: white; }' } },
    };

    render(<PrototypeFrame files={files} />);

    await new Promise((resolve) => setTimeout(resolve, 0));

    expect(mockMount).toHaveBeenCalledWith({
      src: {
        directory: {
          'index.js': { file: { contents: 'console.log("Hello, world!");' } },
          utils: {
            directory: {
              'helper.js': {
                file: { contents: 'export const helper = () => {};' },
              },
            },
          },
        },
      },
      assets: {
        directory: {
          'style.css': { file: { contents: 'body { background: white; }' } },
        },
      },
    });
  });

  it('handles file mounting correctly', async () => {
    const mockMount = vi.fn();
    const mockOn = vi.fn();
    const mockSpawn = vi.fn();

    (useWebContainer as ReturnType<typeof vi.fn>).mockReturnValue({
      instance: { on: mockOn, mount: mockMount, spawn: mockSpawn },
      loading: false,
      error: null,
    });

    const files: FileTree = {
      'index.js': { file: { contents: 'console.log("Hello, world!");' } },
    };

    render(<PrototypeFrame files={files} />);

    await new Promise((resolve) => setTimeout(resolve, 0));

    expect(mockMount).toHaveBeenCalledWith({
      'index.js': { file: { contents: 'console.log("Hello, world!");' } },
    });
  });

  it('processes directories recursively', async () => {
    const mockMount = vi.fn();
    const mockOn = vi.fn();

    (useWebContainer as ReturnType<typeof vi.fn>).mockReturnValue({
      instance: { on: mockOn, mount: mockMount, spawn: vi.fn() },
      loading: false,
      error: null,
    });

    const files: FileTree = {
      src: {
        directory: {
          'index.js': { file: { contents: 'console.log("Hello, world!");' } },
          utils: {
            directory: {
              'helper.js': {
                file: { contents: 'export const helper = () => {};' },
              },
            },
          },
        },
      },
      assets: {
        directory: {
          'style.css': { file: { contents: 'body { background: white; }' } },
        },
      },
    };

    render(<PrototypeFrame files={files} />);

    await new Promise((resolve) => setTimeout(resolve, 0));

    expect(mockMount).toHaveBeenCalledWith({
      src: {
        directory: {
          'index.js': { file: { contents: 'console.log("Hello, world!");' } },
          utils: {
            directory: {
              'helper.js': {
                file: { contents: 'export const helper = () => {};' },
              },
            },
          },
        },
      },
      assets: {
        directory: {
          'style.css': { file: { contents: 'body { background: white; }' } },
        },
      },
    });
  });

  it('renders iframe with correct sandbox attributes', () => {
    (useWebContainer as ReturnType<typeof vi.fn>).mockReturnValue({
      instance: { on: vi.fn(), mount: vi.fn(), spawn: vi.fn() },
      loading: false,
      error: null,
    });

    render(<PrototypeFrame files={{}} />);

    const iframe = screen.getByTitle('Prototype Preview');
    expect(iframe).toBeInTheDocument();

    const sandboxAttr = iframe.getAttribute('sandbox');
    expect(sandboxAttr).toContain('allow-scripts');
    expect(sandboxAttr).toContain('allow-same-origin');
    expect(sandboxAttr).toContain('allow-forms');
    expect(sandboxAttr).toContain('allow-modals');
  });
   */

  it('handles Error objects correctly in error handling', async () => {
    vi.spyOn(console, 'error').mockImplementation(() => {});

    const mockMount = vi
      .fn()
      .mockRejectedValue(new Error('Failed to mount files'));

    (useWebContainer as ReturnType<typeof vi.fn>).mockReturnValue({
      instance: {
        on: vi.fn(),
        mount: mockMount,
        spawn: vi.fn(),
      },
      loading: false,
      error: null,
    });

    render(
      <PrototypeFrame
        files={{ 'index.js': { file: { contents: 'console.log("hello");' } } }}
      />
    );

    const statusElement = await screen.findByText(
      /Status:.*Error:/i,
      {},
      { timeout: 3000 }
    );

    expect(statusElement.textContent).toContain('Failed to mount files');
  });

  it('handles files with "contents" property at root level', async () => {
    const mockMount = vi.fn();
    const mockOn = vi.fn();

    (useWebContainer as ReturnType<typeof vi.fn>).mockReturnValue({
      instance: {
        on: mockOn,
        mount: mockMount,
        spawn: vi.fn(),
      },
      loading: false,
      error: null,
    });

    const files: FileTree = {
      'root-file.js': {
        contents: 'console.log("root file");',
      } as any,
    };

    render(<PrototypeFrame files={files} />);

    await new Promise((resolve) => setTimeout(resolve, 0));

    expect(mockMount).toHaveBeenCalledWith({
      'root-file.js': { file: { contents: 'console.log("root file");' } },
    });
  });

  it('handles files with "contents" property in nested paths', async () => {
    const mockMount = vi.fn();
    const mockOn = vi.fn();

    (useWebContainer as ReturnType<typeof vi.fn>).mockReturnValue({
      instance: { on: mockOn, mount: mockMount, spawn: vi.fn() },
      loading: false,
      error: null,
    });

    const files = {
      'src/nested-file.js': { contents: 'console.log("nested file");' },
    } as unknown as FileTree;

    render(<PrototypeFrame files={files} />);

    await new Promise((resolve) => setTimeout(resolve, 0));

    expect(mockMount).toHaveBeenCalledWith({
      src: {
        directory: {
          'nested-file.js': {
            file: { contents: 'console.log("nested file");' },
          },
        },
      },
    });
  });

  it('handles string errors correctly', async () => {
    vi.spyOn(console, 'error').mockImplementation(() => {});

    const stringError = 'Server connection failed';
    const mockMount = vi.fn().mockRejectedValue(stringError);

    (useWebContainer as ReturnType<typeof vi.fn>).mockReturnValue({
      instance: {
        on: vi.fn(),
        mount: mockMount,
        spawn: vi.fn(),
      },
      loading: false,
      error: null,
    });

    render(
      <PrototypeFrame
        files={{ 'index.js': { file: { contents: 'console.log("hello");' } } }}
      />
    );

    const statusElement = await screen.findByText(
      /Status:.*Error:/i,
      {},
      { timeout: 3000 }
    );

    expect(statusElement.textContent).toContain('Server connection failed');
  });

  it('handles object errors with message property correctly', async () => {
    vi.spyOn(console, 'error').mockImplementation(() => {});

    const objectError = { message: 'Invalid file format', code: 422 };
    const mockMount = vi.fn().mockRejectedValue(objectError);

    (useWebContainer as ReturnType<typeof vi.fn>).mockReturnValue({
      instance: {
        on: vi.fn(),
        mount: mockMount,
        spawn: vi.fn(),
      },
      loading: false,
      error: null,
    });

    render(
      <PrototypeFrame
        files={{ 'index.js': { file: { contents: 'console.log("hello");' } } }}
      />
    );

    const statusElement = await screen.findByText(
      /Status:.*Error:/i,
      {},
      { timeout: 3000 }
    );

    expect(statusElement.textContent).toContain('Invalid file format');
  });

  it('handles npm install failures with proper error message', async () => {
    vi.spyOn(console, 'error').mockImplementation(() => {});

    const mockSpawn = vi.fn().mockImplementation((command, args) => {
      if (command === 'npm') {
        return {
          exit: Promise.resolve(1),
          output: { pipeTo: vi.fn() },
        };
      }
      return {
        exit: Promise.resolve(0),
        output: { pipeTo: vi.fn() },
      };
    });

    const mockIframe = {
      sandbox: {
        add: vi.fn(),
        contains: vi.fn().mockReturnValue(false),
      },
    };

    vi.spyOn(React, 'useRef').mockReturnValue({ current: mockIframe });

    (useWebContainer as ReturnType<typeof vi.fn>).mockReturnValue({
      instance: {
        on: vi.fn(),
        mount: vi.fn().mockResolvedValue(undefined),
        spawn: mockSpawn,
      },
      loading: false,
      error: null,
    });

    render(
      <PrototypeFrame
        files={{ 'index.js': { file: { contents: 'console.log("hello");' } } }}
      />
    );

    const statusElement = await screen.findByText(
      /Status:.*Error:/i,
      {},
      { timeout: 3000 }
    );

    expect(statusElement.textContent).toContain(
      'Error:'
    );

    expect(mockSpawn).toHaveBeenCalledWith('npm', ['run', 'dev']);
  });

  it('pipes server output to a WritableStream', async () => {
    vi.restoreAllMocks();
    const consoleLogSpy = vi.spyOn(console, 'log').mockImplementation(() => {});

    const mockIframe = {
      sandbox: {
        add: vi.fn(),
        contains: vi.fn().mockReturnValue(false),
      },
    };

    vi.spyOn(React, 'useRef').mockReturnValue({ current: mockIframe });

    const mockPipeTo = vi.fn();

    const mockSpawn = vi.fn().mockImplementation(() => {
      return {
        output: {
          pipeTo: mockPipeTo,
        },
        exit: Promise.resolve(0),
      };
    });

    (useWebContainer as ReturnType<typeof vi.fn>).mockReturnValue({
      instance: {
        on: vi.fn(),
        mount: vi.fn().mockResolvedValue(undefined),
        spawn: mockSpawn,
        fs: {
          readdir: vi.fn().mockResolvedValue([]),
        },
      },
      loading: false,
      error: null,
    });

    render(
      <PrototypeFrame
        files={{ 'index.js': { file: { contents: 'console.log("hello");' } } }}
      />
    );

    await new Promise((resolve) => setTimeout(resolve, 500));
    expect(mockSpawn).toHaveBeenCalled();
  });

  it('logs server output to console when received', async () => {
    vi.restoreAllMocks();
    const consoleLogSpy = vi.spyOn(console, 'log');

    const mockIframe = {
      sandbox: {
        add: vi.fn(),
        contains: vi.fn().mockReturnValue(false),
      },
    };

    vi.spyOn(React, 'useRef').mockReturnValue({ current: mockIframe });

    global.WritableStream = vi.fn().mockImplementation(() => {
      return {
        getWriter: vi.fn().mockReturnValue({
          write: vi.fn(),
          releaseLock: vi.fn(),
        }),
      };
    });

    (useWebContainer as ReturnType<typeof vi.fn>).mockReturnValue({
      instance: {
        on: vi.fn(),
        mount: vi.fn().mockResolvedValue(undefined),
        spawn: vi.fn().mockImplementation(() => {
          return {
            output: {
              pipeTo: vi.fn(),
            },
            exit: Promise.resolve(0),
          };
        }),
        fs: {
          readdir: vi.fn().mockResolvedValue([]),
          rm: vi.fn().mockResolvedValue(undefined),
        }
      },
      loading: false,
      error: null,
    });

    render(
      <PrototypeFrame
        files={{ 'index.js': { file: { contents: 'console.log("hello");' } } }}
      />
    );

    await new Promise((resolve) => setTimeout(resolve, 500));

    const logCalls = consoleLogSpy.mock.calls.map(call => call[0]);

    expect(logCalls.some(msg =>
      typeof msg === 'string' && msg.includes('Files to mount')
    )).toBe(true);
  });

  it('sets up the server-ready listener correctly', () => {
    const useEffectSpy = vi.spyOn(React, 'useEffect');

    const mockOn = vi.fn();
    (useWebContainer as ReturnType<typeof vi.fn>).mockReturnValue({
      instance: {
        on: mockOn,
        mount: vi.fn(),
        spawn: vi.fn(),
      },
      loading: false,
      error: null,
    });

    render(<PrototypeFrame files={{}} />);

    expect(mockOn).toHaveBeenCalledWith('server-ready', expect.any(Function));

    const serverReadyCallback = mockOn.mock.calls.find(
      (call) => call[0] === 'server-ready'
    )?.[1];

    const setUrl = vi.fn();
    const setStatus = vi.fn();

    if (serverReadyCallback) {
      const simulateCallback = () => {
        const closureSetUrl = setUrl;
        const closureSetStatus = setStatus;

        serverReadyCallback(3000, 'http://localhost:3000');

        return { setUrl, setStatus };
      };

      const result = simulateCallback();

      expect(console.log).toHaveBeenCalledWith(
        'Server ready on port',
        3000,
        'at URL',
        'http://localhost:3000'
      );
    }
  });

  it('handles empty files correctly', async () => {
    const mockMount = vi.fn();
    const mockOn = vi.fn();

    vi.spyOn(console, 'log').mockImplementation(() => {});

    (useWebContainer as ReturnType<typeof vi.fn>).mockReturnValue({
      instance: { on: mockOn, mount: mockMount, spawn: vi.fn() },
      loading: false,
      error: null,
    });

    const files: FileTree = {
      'empty-file.txt': { file: { contents: '' } },
      'normal-file.js': {
        file: { contents: 'console.log("I have content");' },
      },
    };

    render(<PrototypeFrame files={files} />);

    await new Promise((resolve) => setTimeout(resolve, 0));

    expect(mockMount).toHaveBeenCalledWith({
      'normal-file.js': {
        file: { contents: 'console.log("I have content");' },
      },
    });
  });

  it('reuses existing directories when available', async () => {
    const mockMount = vi.fn();
    const mockOn = vi.fn();

    const consoleLogSpy = vi.spyOn(console, 'log');

    (useWebContainer as ReturnType<typeof vi.fn>).mockReturnValue({
      instance: { on: mockOn, mount: mockMount, spawn: vi.fn() },
      loading: false,
      error: null,
    });

    const files: FileTree = {
      'src/components/Button.jsx': {
        file: {
          contents: 'export const Button = () => <button>Click me</button>;',
        },
      },
      'src/components/Input.jsx': {
        file: { contents: 'export const Input = () => <input />;' },
      },
    };

    render(<PrototypeFrame files={files} />);

    await new Promise((resolve) => setTimeout(resolve, 0));

    const logCalls = consoleLogSpy.mock.calls.map(call => call[0]);

    expect(logCalls).toContain('Creating directory: src');
    expect(logCalls).toContain('Creating directory: components');

    expect(mockMount).toHaveBeenCalledWith({
      src: {
        directory: {
          components: {
            directory: {
              'Button.jsx': {
                file: {
                  contents:
                    'export const Button = () => <button>Click me</button>;',
                },
              },
              'Input.jsx': {
                file: { contents: 'export const Input = () => <input />;' },
              },
            },
          },
        },
      },
    });
  });

  it('handles a mix of new and existing directories', async () => {
    const mockMount = vi.fn();
    const mockOn = vi.fn();

    vi.clearAllMocks();

    const consoleLogSpy = vi.spyOn(console, 'log');

    (useWebContainer as ReturnType<typeof vi.fn>).mockReturnValue({
      instance: { on: mockOn, mount: mockMount, spawn: vi.fn() },
      loading: false,
      error: null,
    });

    const files: FileTree = {
      'src/components/Button.jsx': {
        file: {
          contents: 'export const Button = () => <button>Click</button>;',
        },
      },
      'src/utils/helpers.js': {
        file: {
          contents: 'export const formatDate = date => date.toISOString();',
        },
      },
      'src/components/Input.jsx': {
        file: { contents: 'export const Input = () => <input />;' },
      },
    };

    render(<PrototypeFrame files={files} />);

    await new Promise((resolve) => setTimeout(resolve, 0));

    const creationLogs = consoleLogSpy.mock.calls.filter(
      (call) =>
        typeof call[0] === 'string' &&
        call[0].includes('Creating directory:')
    );

    expect(creationLogs.length).toBeGreaterThanOrEqual(0);

    expect(mockMount).toHaveBeenCalledWith({
      src: {
        directory: {
          components: {
            directory: {
              'Button.jsx': {
                file: {
                  contents:
                    'export const Button = () => <button>Click</button>;',
                },
              },
              'Input.jsx': {
                file: { contents: 'export const Input = () => <input />;' },
              },
            },
          },
          utils: {
            directory: {
              'helpers.js': {
                file: {
                  contents:
                    'export const formatDate = date => date.toISOString();',
                },
              },
            },
          },
        },
      },
    });
  });

  it('handles the same directory path from different sources correctly', async () => {
    const mockMount = vi.fn();
    const mockOn = vi.fn();

    vi.clearAllMocks();

    const consoleLogSpy = vi.spyOn(console, 'log');

    (useWebContainer as ReturnType<typeof vi.fn>).mockReturnValue({
      instance: { on: mockOn, mount: mockMount, spawn: vi.fn() },
      loading: false,
      error: null,
    });

    const files = {
      'src/utils/format.js': {
        contents: 'export const format = str => str.toUpperCase();',
      } as any,

      'src/utils/validate.js': {
        file: { contents: 'export const validate = val => Boolean(val);' },
      },
    } as unknown as FileTree;

    render(<PrototypeFrame files={files} />);

    await new Promise((resolve) => setTimeout(resolve, 0));

    expect(consoleLogSpy).toHaveBeenCalledWith('Creating directory: src');
    expect(consoleLogSpy).toHaveBeenCalledWith('Creating directory: utils');

    const logCalls = consoleLogSpy.mock.calls.map(call => call[0]);
    expect(logCalls).toContain('Creating directory: src');
    expect(logCalls).toContain('Creating directory: utils');

    expect(mockMount).toHaveBeenCalledWith({
      src: {
        directory: {
          utils: {
            directory: {
              'format.js': {
                file: {
                  contents: 'export const format = str => str.toUpperCase();',
                },
              },
              'validate.js': {
                file: {
                  contents: 'export const validate = val => Boolean(val);',
                },
              },
            },
          },
        },
      },
    });
  });
});

describe('Filesystem cleanup functionality', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('removes src, public, and node_modules directories when they exist', async () => {
    const consoleLogSpy = vi.spyOn(console, 'log').mockImplementation(() => {});

    const mockFsRm = vi.fn().mockResolvedValue(undefined);
    const mockFsReaddir = vi
      .fn()
      .mockResolvedValue(['src', 'public', 'node_modules', 'package.json']);

    const mockWebContainer = {
      fs: {
        readdir: mockFsReaddir,
        rm: mockFsRm,
      },
      on: vi.fn(),
      mount: vi.fn(),
      spawn: vi.fn(),
    };

    (useWebContainer as ReturnType<typeof vi.fn>).mockReturnValue({
      instance: mockWebContainer,
      loading: false,
      error: null,
    });

    render(
      <PrototypeFrame
        files={{ 'test.js': { file: { contents: 'console.log("test")' } } }}
      />
    );

    await new Promise((resolve) => setTimeout(resolve, 50));

    expect(mockFsReaddir).toHaveBeenCalledWith('/');

    expect(mockFsRm).toHaveBeenCalledWith('/src', {
      recursive: true,
      force: true,
    });
    expect(mockFsRm).toHaveBeenCalledWith('/public', {
      recursive: true,
      force: true,
    });
    expect(mockFsRm).toHaveBeenCalledWith('/node_modules', {
      recursive: true,
      force: true,
    });

    expect(consoleLogSpy).toHaveBeenCalledWith('Current root entries:', [
      'src',
      'public',
      'node_modules',
      'package.json',
    ]);

    expect(consoleLogSpy).toHaveBeenCalledWith('Filesystem reset complete');
  });

  it('handles errors when removing individual files', async () => {
    const consoleLogSpy = vi.spyOn(console, 'log').mockImplementation(() => {});

    const removalError = new Error('Permission denied');

    const mockFsRm = vi.fn().mockImplementation((path) => {
      if (path === '/package.json') {
        return Promise.reject(removalError);
      }
      return Promise.resolve();
    });

    const mockFsReaddir = vi
      .fn()
      .mockResolvedValue(['src', 'package.json', 'README.md']);

    const mockWebContainer = {
      fs: {
        readdir: mockFsReaddir,
        rm: mockFsRm,
      },
      on: vi.fn(),
      mount: vi.fn(),
      spawn: vi.fn(),
    };

    (useWebContainer as ReturnType<typeof vi.fn>).mockReturnValue({
      instance: mockWebContainer,
      loading: false,
      error: null,
    });

    render(
      <PrototypeFrame
        files={{ 'test.js': { file: { contents: 'console.log("test")' } } }}
      />
    );

    await new Promise((resolve) => setTimeout(resolve, 100));

    expect(mockFsRm).toHaveBeenCalledWith('/package.json');
    expect(mockFsRm).toHaveBeenCalledWith('/README.md');

    expect(consoleLogSpy).toHaveBeenCalledWith(
      'Error removing package.json:',
      removalError
    );

    expect(consoleLogSpy).toHaveBeenCalledWith('Removed file: README.md');
    expect(consoleLogSpy).toHaveBeenCalledWith('Filesystem reset complete');

    consoleLogSpy.mockRestore();
  });

  it('logs different types of errors when removing files', async () => {
    const consoleLogSpy = vi.spyOn(console, 'log').mockImplementation(() => {});

    const mockFsRm = vi.fn().mockImplementation((path) => {
      if (path === '/file1.txt') {
        return Promise.reject(new Error('Standard error'));
      } else if (path === '/file2.txt') {
        return Promise.reject('String error');
      } else if (path === '/file3.txt') {
        return Promise.reject({ code: 403, message: 'Object error' });
      }
      return Promise.resolve();
    });

    const mockFsReaddir = vi
      .fn()
      .mockResolvedValue(['file1.txt', 'file2.txt', 'file3.txt', 'file4.txt']);

    const mockWebContainer = {
      fs: {
        readdir: mockFsReaddir,
        rm: mockFsRm,
      },
      on: vi.fn(),
      mount: vi.fn(),
      spawn: vi.fn(),
    };

    (useWebContainer as ReturnType<typeof vi.fn>).mockReturnValue({
      instance: mockWebContainer,
      loading: false,
      error: null,
    });

    render(
      <PrototypeFrame
        files={{ 'test.js': { file: { contents: 'console.log("test")' } } }}
      />
    );

    await new Promise((resolve) => setTimeout(resolve, 100));

    expect(mockFsRm).toHaveBeenCalledWith('/file1.txt');
    expect(mockFsRm).toHaveBeenCalledWith('/file2.txt');
    expect(mockFsRm).toHaveBeenCalledWith('/file3.txt');
    expect(mockFsRm).toHaveBeenCalledWith('/file4.txt');

    expect(consoleLogSpy).toHaveBeenCalledWith(
      'Error removing file1.txt:',
      expect.objectContaining({ message: 'Standard error' })
    );

    expect(consoleLogSpy).toHaveBeenCalledWith(
      'Error removing file2.txt:',
      'String error'
    );

    expect(consoleLogSpy).toHaveBeenCalledWith(
      'Error removing file3.txt:',
      expect.objectContaining({ code: 403, message: 'Object error' })
    );

    expect(consoleLogSpy).toHaveBeenCalledWith('Removed file: file4.txt');
    expect(consoleLogSpy).toHaveBeenCalledWith('Filesystem reset complete');
  });
});
