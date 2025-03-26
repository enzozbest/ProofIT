import { describe, it, expect, vi, beforeEach } from 'vitest';
import React from 'react';
import { 
  ensureViteConfig, 
  ensureViteDependencies, 
  chooseViteStartScript,
  configureViteSandbox,
  ViteSetupContext 
} from '../../hooks/ViteSetup';
import { WebContainer } from '@webcontainer/api';

vi.mock('@webcontainer/api', () => ({
  WebContainer: {
    boot: vi.fn(),
  }
}));

describe('ViteSetup', () => {
  let mockWebcontainerInstance: any;
  let context: ViteSetupContext;
  let mockSetStatus: any;

  beforeEach(() => {
    vi.clearAllMocks();

    mockWebcontainerInstance = {
      fs: {
        stat: vi.fn(),
        readFile: vi.fn(),
        writeFile: vi.fn(),
      }
    };

    mockSetStatus = vi.fn();

    context = {
      webcontainerInstance: mockWebcontainerInstance as unknown as WebContainer,
      setStatus: mockSetStatus
    };
  });

  describe('chooseViteStartScript', () => {
    it('returns "dev" when available', () => {
      const result = chooseViteStartScript(['build', 'dev', 'test']);
      expect(result).toBe('dev');
    });

    it('follows priority order for scripts', () => {
      const result = chooseViteStartScript(['build', 'start', 'test']);
      expect(result).toBe('start');
    });

    it('returns first available script if no priority matches', () => {
      const result = chooseViteStartScript(['build', 'test']);
      expect(result).toBe('build');
    });

    it('returns "dev" as default when no scripts available', () => {
      const result = chooseViteStartScript([]);
      expect(result).toBe('dev');
    });
  });

  describe('ensureViteConfig', () => {
    it('does not create config if it already exists', async () => {
      mockWebcontainerInstance.fs.stat.mockResolvedValue({}); // File exists
      
      await ensureViteConfig(context);
      
      expect(mockWebcontainerInstance.fs.writeFile).not.toHaveBeenCalled();
      expect(mockSetStatus).toHaveBeenCalledWith('Ensuring Vite configuration...');
    });

    it('creates vite.config.js if it does not exist', async () => {
      mockWebcontainerInstance.fs.stat.mockRejectedValue(new Error('File not found'));
      
      await ensureViteConfig(context);
      
      expect(mockWebcontainerInstance.fs.writeFile).toHaveBeenCalledWith(
        '/vite.config.js',
        expect.stringContaining('defineConfig')
      );
    });

    it('handles errors gracefully', async () => {
      const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
      mockWebcontainerInstance.fs.stat.mockRejectedValue(new Error('Error checking file'));
      mockWebcontainerInstance.fs.writeFile.mockRejectedValue(new Error('Write error'));
      
      await ensureViteConfig(context);
      
      expect(consoleErrorSpy).toHaveBeenCalled();
      consoleErrorSpy.mockRestore();
    });

    it('does nothing when webcontainerInstance is null', async () => {
      const nullContext = { ...context, webcontainerInstance: null };
      
      await ensureViteConfig(nullContext);
      
      expect(mockSetStatus).not.toHaveBeenCalled();
    });
  });

  describe('ensureViteDependencies', () => {
    it('adds missing dependencies and returns true when changes needed', async () => {
      const mockPackageJson = JSON.stringify({
        dependencies: {
          'react': 'latest'
        }
      });
      
      mockWebcontainerInstance.fs.readFile.mockResolvedValue(mockPackageJson);
      
      const result = await ensureViteDependencies(context);
      
      expect(result).toBe(true);
      expect(mockWebcontainerInstance.fs.writeFile).toHaveBeenCalled();
      expect(mockSetStatus).toHaveBeenCalledWith('Installing Vite dependencies...');
      
      // Should have added vite and other missing deps
      const writeCallArg = mockWebcontainerInstance.fs.writeFile.mock.calls[0][1];
      const updatedJson = JSON.parse(writeCallArg);
      expect(updatedJson.dependencies.vite).toBe('latest');
      expect(updatedJson.dependencies['@vitejs/plugin-react']).toBe('latest');
    });

    it('adds dev script if missing', async () => {
      const mockPackageJson = JSON.stringify({
        dependencies: {
          'vite': 'latest',
          '@vitejs/plugin-react': 'latest',
          'react': 'latest',
          'react-dom': 'latest'
        },
        scripts: {
          'build': 'vite build'
        }
      });
      
      mockWebcontainerInstance.fs.readFile.mockResolvedValue(mockPackageJson);
      
      const result = await ensureViteDependencies(context);
      
      expect(result).toBe(true);
      
      const writeCallArg = mockWebcontainerInstance.fs.writeFile.mock.calls[0][1];
      const updatedJson = JSON.parse(writeCallArg);
      expect(updatedJson.scripts.dev).toBe('vite');
    });
    
    it('returns false when no changes needed', async () => {
      const mockPackageJson = JSON.stringify({
        dependencies: {
          'vite': 'latest',
          '@vitejs/plugin-react': 'latest',
          'react': 'latest',
          'react-dom': 'latest'
        },
        scripts: {
          'dev': 'vite'
        }
      });
      
      mockWebcontainerInstance.fs.readFile.mockResolvedValue(mockPackageJson);
      
      const result = await ensureViteDependencies(context);
      
      expect(result).toBe(false);
      expect(mockWebcontainerInstance.fs.writeFile).not.toHaveBeenCalled();
    });
  });

  describe('configureViteSandbox', () => {
    it('configures iframe sandbox correctly', () => {
      const mockAdd = vi.fn();
      const mockRef = {
        current: {
          sandbox: {
            add: mockAdd
          }
        }
      };
      
      configureViteSandbox(mockRef as unknown as React.RefObject<HTMLIFrameElement>);
      
      expect(mockAdd).toHaveBeenCalledWith('allow-forms');
      expect(mockAdd).toHaveBeenCalledWith('allow-modals');
    });

    it('does nothing if iframeRef.current is null', () => {
      const mockRef = { current: null };
      
      expect(() => {
        configureViteSandbox(mockRef as React.RefObject<HTMLIFrameElement>);
      }).not.toThrow();
    });
  });
});