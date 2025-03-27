import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import React from 'react';
import { 
  ensureViteConfig, 
  ensureViteDependencies, 
  chooseViteStartScript,
  configureViteSandbox,
  ensureIndexHtml,
  ViteSetupContext ,
  findIndexHtml
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

vi.mock('../../hooks/ViteSetup', async (importOriginal) => {
  const actual = await importOriginal() as Record<string, any>;
  return {
    ...actual,
    findIndexHtml: vi.fn(actual.findIndexHtml),
  };
});

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
        readdir: vi.fn(),
      }
    };

    mockSetStatus = vi.fn();

    context = {
      webcontainerInstance: mockWebcontainerInstance as unknown as WebContainer,
      setStatus: mockSetStatus
    };
  });

  afterEach(() => {
    vi.mocked(findIndexHtml).mockRestore();
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

    it('creates javascript template when no entry points are found', async () => {
      mockWebcontainerInstance.fs.stat.mockRejectedValue(new Error('Not found'));
      
      await ensureIndexHtml(context);
      
      expect(mockWebcontainerInstance.fs.writeFile).toHaveBeenCalledWith(
        '/index.html',
        '<html>JS template /src/index.js</html>'
      );
    });

    it('copies index.html from public folder to root if it exists there', async () => {
      mockWebcontainerInstance.fs.stat.mockImplementation((path) => {
        if (path === '/index.html') return Promise.reject(new Error('Not found'));
        if (path === '/public/index.html') return Promise.resolve({});
        return Promise.reject(new Error('Not found'));
      });
      
      mockWebcontainerInstance.fs.readFile.mockImplementation((path) => {
        if (path === '/public/index.html') return Promise.resolve('<!DOCTYPE html><html></html>');
        return Promise.reject(new Error('Not found'));
      });
      
      vi.mocked(findIndexHtml).mockResolvedValue('/public/index.html');
      
      await ensureIndexHtml(context);
      
      expect(mockWebcontainerInstance.fs.writeFile).toHaveBeenCalledWith(
        '/index.html',
        '<!DOCTYPE html><html></html>'
      );
    });

    it('copies index.html from public folder to root', async () => {
      mockWebcontainerInstance.fs.stat.mockImplementation((path) => {
        if (path === '/index.html') return Promise.reject(new Error('Not found'));
        if (path === '/public/index.html') return Promise.resolve({});
        return Promise.reject(new Error('Not found'));
      });
      
      mockWebcontainerInstance.fs.readFile.mockImplementation((path) => {
        if (path === '/public/index.html') return Promise.resolve('<!DOCTYPE html><html></html>');
        return Promise.reject(new Error('Not found'));
      });
      
      vi.mocked(findIndexHtml).mockResolvedValue('/public/index.html');
      
      await ensureIndexHtml(context);
      
      expect(mockWebcontainerInstance.fs.writeFile).toHaveBeenCalledWith(
        '/index.html',
        '<!DOCTYPE html><html></html>'
      );
    });

    it('copies file from public/index.html to root when it exists in public folder', async () => {
      mockWebcontainerInstance.fs.stat.mockImplementation((path) => {
        if (path === '/index.html') return Promise.reject(new Error('Not found'));
        if (path === '/public/index.html') return Promise.resolve({});
        return Promise.reject(new Error('Not found'));
      });
      
      mockWebcontainerInstance.fs.readFile.mockImplementation((path) => {
        if (path === '/public/index.html') return Promise.resolve('<!DOCTYPE html><html></html>');
        return Promise.reject(new Error('Not found'));
      });
      
      vi.mocked(findIndexHtml).mockResolvedValue('/public/index.html');
      
      await ensureIndexHtml(context);
      
      expect(mockWebcontainerInstance.fs.writeFile).toHaveBeenCalledWith(
        '/index.html',
        '<!DOCTYPE html><html></html>'
      );
    });
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
  
  describe('findIndexHtml', () => {
    it('returns path when index.html is in the root directory', async () => {
      mockWebcontainerInstance.fs.readdir.mockImplementation((path) => {
        if (path === '/') return Promise.resolve(['index.html', 'src']);
        return Promise.resolve([]);
      });
      
      const result = await findIndexHtml(mockWebcontainerInstance);
      
      expect(result).toBe('/index.html');
    });
    
    it('returns path when index.html is in the src directory', async () => {
      mockWebcontainerInstance.fs.readdir.mockImplementation((path) => {
        if (path === '/') return Promise.resolve(['src', 'package.json']);
        if (path === '/src') return Promise.resolve(['index.html', 'app.js']);
        return Promise.resolve([]);
      });
      
      const result = await findIndexHtml(mockWebcontainerInstance);
      
      expect(result).toBe('/src/index.html');
    });
    
    it('checks common locations when not in root or src', async () => {
      mockWebcontainerInstance.fs.readdir.mockImplementation((path) => {
        if (path === '/') return Promise.resolve(['src', 'package.json']);
        if (path === '/src') return Promise.resolve(['app.js']);
        return Promise.resolve([]);
      });
      
      mockWebcontainerInstance.fs.stat.mockImplementation((path) => {
        if (path === '/public/index.html') return Promise.resolve({});
        return Promise.reject(new Error('Not found'));
      });
      
      const result = await findIndexHtml(mockWebcontainerInstance);
      
      expect(result).toBe('/public/index.html');
    });
    
    it('performs recursive search when not found in common locations', async () => {
      mockWebcontainerInstance.fs.readdir.mockImplementation((path, options) => {
        if (path === '/') {
          if (options?.withFileTypes) {
            return Promise.resolve([
              { name: 'client', isDirectory: true }
            ]);
          }
          return Promise.resolve(['client']);
        }
        if (path === '/client') {
          if (options?.withFileTypes) {
            return Promise.resolve([
              { name: 'assets', isDirectory: true }
            ]);
          }
          return Promise.resolve(['assets']);
        }
        if (path === '/client/assets') {
          if (options?.withFileTypes) {
            return Promise.resolve([
              { name: 'index.html', isDirectory: false }
            ]);
          }
          return Promise.resolve(['index.html']);
        }
        return Promise.resolve([]);
      });
      
      mockWebcontainerInstance.fs.stat.mockRejectedValue(new Error('Not found'));
      
      const result = await findIndexHtml(mockWebcontainerInstance);
      
      expect(result).toBe('/client/assets/index.html');
    });
    
    it('returns null when index.html is not found anywhere', async () => {
      mockWebcontainerInstance.fs.readdir.mockImplementation((path, options) => {
        if (path === '/') {
          if (options?.withFileTypes) {
            return Promise.resolve([
              { name: 'src', isDirectory: true }
            ]);
          }
          return Promise.resolve(['src']);
        }
        if (path === '/src') {
          if (options?.withFileTypes) {
            return Promise.resolve([
              { name: 'app.js', isDirectory: false }
            ]);
          }
          return Promise.resolve(['app.js']);
        }
        return Promise.resolve([]);
      });
      
      mockWebcontainerInstance.fs.stat.mockRejectedValue(new Error('Not found'));
      
      const result = await findIndexHtml(mockWebcontainerInstance);
      
      expect(result).toBe(null);
    });
    
    it('handles errors during search gracefully', async () => {
      mockWebcontainerInstance.fs.readdir.mockRejectedValue(new Error('Permission denied'));
      mockWebcontainerInstance.fs.stat.mockRejectedValue(new Error('Not found'));
      
      const result = await findIndexHtml(mockWebcontainerInstance);
      
      expect(result).toBe(null);
    });
  });
  
  describe('React version compatibility', () => {
    it('selects correct versions for React 17', async () => {
      mockWebcontainerInstance.fs.readFile.mockResolvedValue(JSON.stringify({
        dependencies: { 
          'react': '17.0.2'
        }
      }));
      
      await ensureViteDependencies(context);
      
      const writeCallArg = mockWebcontainerInstance.fs.writeFile.mock.calls[0][1];
      const updatedJson = JSON.parse(writeCallArg);
      expect(updatedJson.dependencies.vite).toBe('^2.9.15');
      expect(updatedJson.dependencies['@vitejs/plugin-react']).toBe('^1.3.2');
    });
    
    it('selects correct versions for React 16', async () => {
      mockWebcontainerInstance.fs.readFile.mockResolvedValue(JSON.stringify({
        dependencies: { 
          'react': '16.13.1'
        }
      }));
      
      await ensureViteDependencies(context);
      
      const writeCallArg = mockWebcontainerInstance.fs.writeFile.mock.calls[0][1];
      const updatedJson = JSON.parse(writeCallArg);
      expect(updatedJson.dependencies.vite).toBe('^2.8.6');
      expect(updatedJson.dependencies['@vitejs/plugin-react']).toBe('^1.2.0');
    });
    
    it('creates React 17 compatible Vite configuration', async () => {
      mockWebcontainerInstance.fs.stat.mockRejectedValue(new Error('File not found'));
      mockWebcontainerInstance.fs.readFile.mockResolvedValue(JSON.stringify({
        dependencies: { 
          'react': '17.0.2'
        }
      }));
      
      await ensureViteConfig(context);
      
      expect(mockWebcontainerInstance.fs.writeFile).toHaveBeenCalled();
    });
  });
  
  describe('Error handling and edge cases', () => {
    it('creates dependencies object if missing in package.json', async () => {
      mockWebcontainerInstance.fs.readFile.mockResolvedValue(JSON.stringify({
        name: 'test-project'
        // No dependencies object
      }));
      
      await ensureViteDependencies(context);
      
      const writeCallArg = mockWebcontainerInstance.fs.writeFile.mock.calls[0][1];
      const updatedJson = JSON.parse(writeCallArg);
      expect(updatedJson.dependencies).toBeDefined();
      expect(updatedJson.dependencies.vite).toBeDefined();
    });
    
    it('creates scripts object if missing in package.json', async () => {
      mockWebcontainerInstance.fs.readFile.mockResolvedValue(JSON.stringify({
        dependencies: {
          'react': 'latest'
        }
        // No scripts object
      }));
      
      await ensureViteDependencies(context);
      
      const writeCallArg = mockWebcontainerInstance.fs.writeFile.mock.calls[0][1];
      const updatedJson = JSON.parse(writeCallArg);
      expect(updatedJson.scripts).toBeDefined();
      expect(updatedJson.scripts.dev).toBe('vite');
    });
    
    it('handles JSON parse errors gracefully', async () => {
      mockWebcontainerInstance.fs.readFile.mockResolvedValue('invalid json');
      
      const result = await ensureViteDependencies(context);
      
      expect(result).toBe(false);
    });
    
    it('copies index.html from non-root location to root', async () => {
      const originalFindIndexHtml = findIndexHtml;
      vi.mocked(findIndexHtml).mockResolvedValue('/src/index.html');
      
      mockWebcontainerInstance.fs.stat.mockImplementation((path) => {
        if (path === '/src/index.html') return Promise.resolve({});
        return Promise.reject(new Error('Not found'));
      });
      
      mockWebcontainerInstance.fs.readFile.mockImplementation((path) => {
        if (path === '/src/index.html') return Promise.resolve('<!DOCTYPE html><html></html>');
        return Promise.reject(new Error('Not found'));
      });
      
      await ensureIndexHtml(context);
      
      expect(mockWebcontainerInstance.fs.writeFile).toHaveBeenCalledWith(
        '/index.html',
        '<!DOCTYPE html><html></html>'
      );
      
      vi.mocked(findIndexHtml).mockRestore();
    });
  });

});