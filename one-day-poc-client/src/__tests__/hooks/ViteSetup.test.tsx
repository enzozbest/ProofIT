import { describe, it, expect, vi, beforeEach } from 'vitest';
import React from 'react';
import { 
  ensureViteConfig, 
  ensureViteDependencies, 
  chooseViteStartScript,
  configureViteSandbox,
  ensureIndexHtml,
  ViteSetupContext 
} from '../../hooks/ViteSetup';
import { WebContainer } from '@webcontainer/api';

vi.mock('../../hooks/resources/HtmlTemplates', () => ({
  htmlTemplates: {
    react: '<html>React template {{ENTRY_POINT}}</html>',
    javascript: '<html>JS template {{ENTRY_POINT}}</html>',
    fallback: '<html>Fallback template</html>'
  },
  entryPoints: [
    { path: '/src/main.jsx', type: 'react' },
    { path: '/src/index.js', type: 'javascript' }
  ]
}));

vi.mock('../../hooks/resources/ConfigTemplates', () => ({
  configTemplates: {
    viteReact: 'React Vite config mock',
    viteJs: 'JS Vite config mock'
  }
}));

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

  describe('ensureViteConfig', () => {
    it('does not create config if it already exists', async () => {
      mockWebcontainerInstance.fs.stat.mockResolvedValue({}); // File exists
      
      await ensureViteConfig(context);
      
      expect(mockWebcontainerInstance.fs.writeFile).not.toHaveBeenCalled();
      expect(mockSetStatus).toHaveBeenCalledWith('Ensuring Vite configuration...');
    });

    it('creates React vite.config.js if no package.json exists', async () => {
      mockWebcontainerInstance.fs.stat.mockRejectedValue(new Error('File not found'));
      mockWebcontainerInstance.fs.readFile.mockRejectedValue(new Error('File not found'));
      
      await ensureViteConfig(context);
      
      expect(mockWebcontainerInstance.fs.writeFile).toHaveBeenCalledWith(
        '/vite.config.js',
        'React Vite config mock'
      );
    });

    it('creates JS vite.config.js when no React dependency is found', async () => {
      mockWebcontainerInstance.fs.stat.mockRejectedValue(new Error('File not found'));
      mockWebcontainerInstance.fs.readFile.mockResolvedValue(JSON.stringify({
        dependencies: { 
          'vue': 'latest'
        }
      }));
      
      await ensureViteConfig(context);
      
      expect(mockWebcontainerInstance.fs.writeFile).toHaveBeenCalledWith(
        '/vite.config.js',
        'JS Vite config mock'
      );
    });
  });

  describe('ensureIndexHtml', () => {
    it('does not create index.html if it already exists in root', async () => {
      mockWebcontainerInstance.fs.stat.mockImplementation((path) => {
        if (path === '/index.html') return Promise.resolve({});
        return Promise.reject(new Error('Not found'));
      });
      
      await ensureIndexHtml(context);
      
      expect(mockWebcontainerInstance.fs.writeFile).not.toHaveBeenCalled();
    });

    it('does not create index.html if it exists in public folder', async () => {
      mockWebcontainerInstance.fs.stat.mockImplementation((path) => {
        if (path === '/index.html') return Promise.reject(new Error('Not found'));
        if (path === '/public/index.html') return Promise.resolve({});
        return Promise.reject(new Error('Not found'));
      });
      
      await ensureIndexHtml(context);
      
      expect(mockWebcontainerInstance.fs.writeFile).not.toHaveBeenCalled();
    });

    it('creates React index.html when React entry point is found', async () => {
      mockWebcontainerInstance.fs.stat.mockImplementation((path) => {
        if (path === '/index.html' || path === '/public/index.html') {
          return Promise.reject(new Error('Not found'));
        }
        if (path === '/src/main.jsx') {
          return Promise.resolve({});
        }
        return Promise.reject(new Error('Not found'));
      });
      
      await ensureIndexHtml(context);
      
      expect(mockWebcontainerInstance.fs.writeFile).toHaveBeenCalledWith(
        '/index.html',
        '<html>React template /src/main.jsx</html>'
      );
    });

    it('creates JavaScript index.html when JS entry point is found', async () => {
      mockWebcontainerInstance.fs.stat.mockImplementation((path) => {
        if (path === '/index.html' || path === '/public/index.html') {
          return Promise.reject(new Error('Not found'));
        }
        if (path === '/src/main.jsx') {
          return Promise.reject(new Error('Not found'));
        }
        if (path === '/src/index.js') {
          return Promise.resolve({});
        }
        return Promise.reject(new Error('Not found'));
      });
      
      await ensureIndexHtml(context);
      
      expect(mockWebcontainerInstance.fs.writeFile).toHaveBeenCalledWith(
        '/index.html',
        '<html>JS template /src/index.js</html>'
      );
    });

    it('creates fallback index.html when no entry points are found', async () => {
      mockWebcontainerInstance.fs.stat.mockRejectedValue(new Error('Not found'));
      
      await ensureIndexHtml(context);
      
      expect(mockWebcontainerInstance.fs.writeFile).toHaveBeenCalledWith(
        '/index.html',
        '<html>Fallback template</html>'
      );
    });
  });

  // Keep existing tests for chooseViteStartScript and other functions
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
      expect(updatedJson.dependencies.vite).toBe('^4.3.9');
      expect(updatedJson.dependencies['@vitejs/plugin-react']).toBe('^4.0.0');
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