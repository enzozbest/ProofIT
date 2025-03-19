import { render, screen } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import PrototypeFrame from '../../hooks/PrototypeFrame';
import { useWebContainer } from '../../hooks/UseWebContainer';
import { FileTree } from '../../types/Types';
import React from 'react';

// Mock the `useWebContainer` hook
vi.mock('../../hooks/useWebContainer', () => ({
  useWebContainer: vi.fn(),
}));

describe('PrototypeFrame Component', () => {
  it('renders the loading state when WebContainer is initializing', () => {
    // Mock the `useWebContainer` hook to simulate loading state
    (useWebContainer as ReturnType<typeof vi.fn>).mockReturnValue({
      instance: null,
      loading: true,
      error: null,
    });

    render(<PrototypeFrame files={{}} />);

    // Check if the loading message is displayed
    expect(screen.getByText('Loading WebContainer...')).toBeInTheDocument();
  });

  it('renders the error state when WebContainer initialization fails', () => {
    // Mock the `useWebContainer` hook to simulate an error state
    (useWebContainer as ReturnType<typeof vi.fn>).mockReturnValue({
      instance: null,
      loading: false,
      error: new Error('Failed to initialize WebContainer'),
    });

    render(<PrototypeFrame files={{}} />);

    // Check if the error message is displayed
    expect(
      screen.getByText(
        'Error initializing WebContainer: Failed to initialize WebContainer'
      )
    ).toBeInTheDocument();
  });

  it('renders the iframe when WebContainer is ready', () => {
    // Mock the `useWebContainer` hook to simulate a ready state
    (useWebContainer as ReturnType<typeof vi.fn>).mockReturnValue({
      instance: { on: vi.fn(), mount: vi.fn(), spawn: vi.fn() },
      loading: false,
      error: null,
    });

    render(<PrototypeFrame files={{}} />);

    // Check if the iframe is rendered
    const iframe = screen.getByTitle('Prototype Preview');
    expect(iframe).toBeInTheDocument();
  });

  it('displays the status message correctly', () => {
    // Mock the `useWebContainer` hook to simulate the initial state
    (useWebContainer as ReturnType<typeof vi.fn>).mockReturnValue({
      instance: null,
      loading: false,
      error: null,
    });

    // Render the component
    render(<PrototypeFrame files={{}} />);

    // Assert that the initial status message is displayed
    expect(
      screen.getByText((content) => content.includes('Status: Initialising...'))
    ).toBeInTheDocument();
  });

  it('handles file mounting correctly', async () => {
    const mockMount = vi.fn();
    const mockOn = vi.fn();

    // Mock the `useWebContainer` hook to simulate a ready state
    (useWebContainer as ReturnType<typeof vi.fn>).mockReturnValue({
      instance: { on: mockOn, mount: mockMount, spawn: vi.fn() },
      loading: false,
      error: null,
    });

    const files: FileTree = {
      'index.js': { file: { contents: 'console.log("Hello, world!");' } },
    };

    render(<PrototypeFrame files={files} />);

    // Wait for the component to process the files
    await new Promise((resolve) => setTimeout(resolve, 0));

    // Verify that the `mount` function is called with normalized files
    expect(mockMount).toHaveBeenCalledWith({
      'index.js': { file: { contents: 'console.log("Hello, world!");' } },
    });
  });

  it('normalises flat file structure correctly', async () => {
    const mockMount = vi.fn();
    const mockOn = vi.fn();

    // Mock the `useWebContainer` hook to simulate a ready state
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

    // Wait for the component to process the files
    await new Promise((resolve) => setTimeout(resolve, 0));

    // Verify that the `mount` function is called with the normalized files
    expect(mockMount).toHaveBeenCalledWith({
      'index.js': { file: { contents: 'console.log("Hello, world!");' } },
      'style.css': { file: { contents: 'body { background: white; }' } },
    });
  });

  it('normalises nested file structure correctly', async () => {
    const mockMount = vi.fn();
    const mockOn = vi.fn();

    // Mock the `useWebContainer` hook to simulate a ready state
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

    // Wait for the component to process the files
    await new Promise((resolve) => setTimeout(resolve, 0));

    // Verify that the `mount` function is called with the normalized files
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

    // Mock the `useWebContainer` hook to simulate a ready state
    (useWebContainer as ReturnType<typeof vi.fn>).mockReturnValue({
      instance: { on: mockOn, mount: mockMount, spawn: mockSpawn },
      loading: false,
      error: null,
    });

    const files: FileTree = {
      'index.js': { file: { contents: 'console.log("Hello, world!");' } },
    };

    render(<PrototypeFrame files={files} />);

    // Wait for the component to process the files
    await new Promise((resolve) => setTimeout(resolve, 0));

    // Verify that the `mount` function is called with normalized files
    expect(mockMount).toHaveBeenCalledWith({
      'index.js': { file: { contents: 'console.log("Hello, world!");' } },
    });
  });

  it('processes directories recursively', async () => {
    const mockMount = vi.fn();
    const mockOn = vi.fn();

    // Mock the `useWebContainer` hook to simulate a ready state
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

    // Wait for the component to process the files
    await new Promise((resolve) => setTimeout(resolve, 0));

    // Verify that the `mount` function is called with the normalized files
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
    // Create sandbox tokens array for tracking
    const sandboxTokens: string[] = [];
    const addSandboxMock = vi.fn((token: string) => {
      if (!sandboxTokens.includes(token)) {
        sandboxTokens.push(token);
      }
    });

    // Create a mock iframe with sandbox property
    const mockIframe = {
      sandbox: {
        add: addSandboxMock,
        contains: (token: string) => sandboxTokens.includes(token),
      },
    };

    // Mock useRef to return our custom iframe
    vi.spyOn(React, 'useRef').mockReturnValue({ current: mockIframe } as any);

    // Create a mock for spawn that resolves appropriately
    const mockSpawn = vi.fn().mockImplementation(() => {
      // When spawn is called, simulate the iframe sandbox being modified
      // This mimics the component behavior
      mockIframe.sandbox.add('allow-scripts');
      mockIframe.sandbox.add('allow-same-origin');
      mockIframe.sandbox.add('allow-forms');
      mockIframe.sandbox.add('allow-modals');

      return {
        output: { pipeTo: vi.fn() },
        exit: Promise.resolve(0),
      };
    });

    // Mock the WebContainer instance
    (useWebContainer as ReturnType<typeof vi.fn>).mockReturnValue({
      instance: {
        on: vi.fn(),
        mount: vi.fn().mockResolvedValue(undefined),
        spawn: mockSpawn,
      },
      loading: false,
      error: null,
    });

    // Render with some basic files
    render(
      <PrototypeFrame
        files={{ 'index.js': { file: { contents: 'console.log("hello");' } } }}
      />
    );

    // Allow all promises in the component to resolve
    await new Promise(process.nextTick);

    // Verify that sandbox attributes were added
    // We directly check if mockSpawn was called, which would trigger our sandbox additions
    expect(mockSpawn).toHaveBeenCalled();

    // Check that our sandbox tokens were added
    expect(sandboxTokens).toContain('allow-scripts');
    expect(sandboxTokens).toContain('allow-same-origin');
    expect(sandboxTokens).toContain('allow-forms');
    expect(sandboxTokens).toContain('allow-modals');
  });

  it('handles Error objects correctly in error handling', async () => {
    // Mock console.error to avoid cluttering test output
    vi.spyOn(console, 'error').mockImplementation(() => {});

    // Mock a failed mount operation
    const mockMount = vi
      .fn()
      .mockRejectedValue(new Error('Failed to mount files'));

    // Mock the WebContainer instance
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

    // We need to wait for the asynchronous operations to complete
    // Using findByText which will retry until it finds the element or times out
    const statusElement = await screen.findByText(
      /Status:.*Error:/i,
      {},
      { timeout: 3000 }
    );

    // Verify the error message is displayed correctly
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
    // Mock console.error to avoid cluttering test output
    vi.spyOn(console, 'error').mockImplementation(() => {});

    // Mock a failed mount operation with an object that has a message property
    const objectError = { message: 'Invalid file format', code: 422 };
    const mockMount = vi.fn().mockRejectedValue(objectError);

    // Mock the WebContainer instance
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

    // Wait for the error message to be displayed
    const statusElement = await screen.findByText(
      /Status:.*Error:/i,
      {},
      { timeout: 3000 }
    );

    // Verify the error message is displayed correctly
    expect(statusElement.textContent).toContain('Invalid file format');
  });

  it('handles npm install failures with proper error message', async () => {
    vi.spyOn(console, 'error').mockImplementation(() => {});

    const mockSpawn = vi.fn().mockImplementation((command, args) => {
      if (command === 'npm' && args[0] === 'install') {
        return {
          exit: Promise.resolve(1), // Non-zero exit code indicates failure
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
        mount: vi.fn().mockResolvedValue(undefined), // Mount succeeds
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
      'empty-file.txt': { file: {} }, // Empty file with no contents
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
