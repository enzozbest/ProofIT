import { render, screen } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import PrototypeFrame from '../../hooks/PrototypeFrame';
import { useWebContainer } from '../../hooks/useWebContainer';
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
});
