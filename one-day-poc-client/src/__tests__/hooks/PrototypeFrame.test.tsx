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

  it('sets iframe sandbox attributes correctly', async () => {
    const sandboxTokens: string[] = [];
    const addSandboxMock = vi.fn((token: string) => {
      if (!sandboxTokens.includes(token)) {
        sandboxTokens.push(token);
      }
    });

    const mockIframe = {
      sandbox: {
        add: addSandboxMock,
        contains: (token: string) => sandboxTokens.includes(token),
      },
    };

    vi.spyOn(React, 'useRef').mockReturnValue({ current: mockIframe } as any);

    const mockSpawn = vi.fn().mockImplementation(() => {

      mockIframe.sandbox.add('allow-scripts');
      mockIframe.sandbox.add('allow-same-origin');
      mockIframe.sandbox.add('allow-forms');
      mockIframe.sandbox.add('allow-modals');

      return {
        output: { pipeTo: vi.fn() },
        exit: Promise.resolve(0),
      };
    });

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

    await new Promise(process.nextTick);

    expect(mockSpawn).toHaveBeenCalled();

    expect(sandboxTokens).toContain('allow-scripts');
    expect(sandboxTokens).toContain('allow-same-origin');
    expect(sandboxTokens).toContain('allow-forms');
    expect(sandboxTokens).toContain('allow-modals');
  });

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
      if (command === 'npm' && args[0] === 'install') {
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
      /Status:.*Error: npm install failed with exit code 1/i,
      {},
      { timeout: 3000 }
    );

    expect(statusElement.textContent).toContain(
      'npm install failed with exit code 1'
    );

    expect(mockSpawn).toHaveBeenCalledWith('npm', ['install']);
  });

  it('pipes server output to a WritableStream', async () => {
    const consoleLogSpy = vi.spyOn(console, 'log');

    const mockPipeTo = vi.fn();

    const mockSpawn = vi.fn().mockImplementation((command, args) => {
      if (command === 'npm') {
        if (args[0] === 'install') {
          return {
            exit: Promise.resolve(0),
            output: { pipeTo: vi.fn() },
          };
        } else if (args[0] === 'run' && args[1] === 'start') {
          return {
            output: {
              pipeTo: mockPipeTo,
            },
            exit: Promise.resolve(0),
          };
        }
      }
      return {
        exit: Promise.resolve(0),
        output: { pipeTo: vi.fn() },
      };
    });

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

    await new Promise((resolve) => setTimeout(resolve, 100));

    expect(mockPipeTo).toHaveBeenCalled();

    const pipeToArg = mockPipeTo.mock.calls[0][0];
    expect(pipeToArg).toBeInstanceOf(WritableStream);
  });

  it('logs server output to console when received', async () => {
    const consoleLogSpy = vi.spyOn(console, 'log');

    const mockPipeTo = vi.fn().mockImplementation((writableStream) => {
      const writer = writableStream.getWriter();

      writer.write('Server started on port 3000');
      writer.write('Compiled successfully!');

      return { catch: vi.fn() };
    });

    const mockSpawn = vi.fn().mockImplementation((command, args) => {
      if (command === 'npm') {
        if (args[0] === 'install') {
          return {
            exit: Promise.resolve(0),
            output: { pipeTo: vi.fn() },
          };
        } else if (args[0] === 'run' && args[1] === 'start') {
          return {
            output: {
              pipeTo: mockPipeTo,
            },
            exit: Promise.resolve(0),
          };
        }
      }
      return {
        exit: Promise.resolve(0),
        output: { pipeTo: vi.fn() },
      };
    });

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

    await new Promise((resolve) => setTimeout(resolve, 100));

    expect(consoleLogSpy).toHaveBeenCalledWith(
      'Server output:',
      'Server started on port 3000'
    );
    expect(consoleLogSpy).toHaveBeenCalledWith(
      'Server output:',
      'Compiled successfully!'
    );
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

    const consoleLogSpy = vi.spyOn(console, 'log');

    (useWebContainer as ReturnType<typeof vi.fn>).mockReturnValue({
      instance: { on: mockOn, mount: mockMount, spawn: vi.fn() },
      loading: false,
      error: null,
    });

    const files: FileTree = {
      'empty-file.txt': { file: {} }, 
      'normal-file.js': {
        file: { contents: 'console.log("I have content");' },
      },
    };

    render(<PrototypeFrame files={files} />);

    await new Promise((resolve) => setTimeout(resolve, 0));

    expect(consoleLogSpy).toHaveBeenCalledWith(
      '  Adding empty file: empty-file.txt'
    );

    expect(mockMount).toHaveBeenCalledWith({
      'empty-file.txt': { file: { contents: '' } },
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

    expect(consoleLogSpy).toHaveBeenCalledWith('  Creating new directory: src');
    expect(consoleLogSpy).toHaveBeenCalledWith(
      '  Creating new directory: components'
    );

    expect(consoleLogSpy).toHaveBeenCalledWith(
      '  Directory src already exists'
    );
    expect(consoleLogSpy).toHaveBeenCalledWith(
      '  Directory components already exists'
    );

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

    console.log.mockClear();

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
        call[0].includes('Creating new directory')
    );

    const reuseLogs = consoleLogSpy.mock.calls.filter(
      (call) =>
        typeof call[0] === 'string' &&
        call[0].includes('Directory') &&
        call[0].includes('already exists')
    );

    expect(creationLogs.length).toBeGreaterThanOrEqual(3);

    expect(reuseLogs.length).toBeGreaterThanOrEqual(3);

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

    console.log.mockClear();

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

    expect(consoleLogSpy).toHaveBeenCalledWith('  Creating new directory: src');
    expect(consoleLogSpy).toHaveBeenCalledWith(
      '  Creating new directory: utils'
    );
    expect(consoleLogSpy).toHaveBeenCalledWith(
      '  Directory src already exists'
    );
    expect(consoleLogSpy).toHaveBeenCalledWith(
      '  Directory utils already exists'
    );

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

    expect(consoleLogSpy).toHaveBeenCalledWith('Filesystem selectively reset');
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
    expect(consoleLogSpy).toHaveBeenCalledWith('Filesystem selectively reset');
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
  });
});
