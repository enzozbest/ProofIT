import { WebContainer } from '@webcontainer/api';
import { htmlTemplates, entryPoints } from './resources/HtmlTemplates';
import { configTemplates } from './resources/ConfigTemplates';

// Define types for context objects
export interface ViteSetupContext {
  webcontainerInstance: WebContainer | null;
  setStatus?: (status: string) => void;
}

/**
 * Ensure Vite configuration exists
 */
export const ensureViteConfig = async (context: ViteSetupContext): Promise<void> => {
  const { webcontainerInstance, setStatus } = context;
  if (!webcontainerInstance) return;
  
  setStatus?.('Ensuring Vite configuration...');
  
  try {
    try {
      await webcontainerInstance.fs.stat('/vite.config.js');
      console.log('vite.config.js exists');
      return;
    } catch (e) {
      let configType = 'viteReact';
      let isReact17 = false;
      
      try {
        const packageJsonText = await webcontainerInstance.fs.readFile('/package.json', 'utf-8');
        const packageJson = JSON.parse(packageJsonText);
        
        if (!packageJson.dependencies?.react && !packageJson.devDependencies?.react) {
          configType = 'viteJs';
        } else {
          const reactVersion = packageJson.dependencies?.react || packageJson.devDependencies?.react;
          if (reactVersion && reactVersion.includes('17.')) {
            isReact17 = true;
            console.log('Detected React 17, using classic JSX runtime');
          }
        }
      } catch (e) {
        console.log('Error reading package.json, using default React config:', e);
      }
      
      let configTemplate = configTemplates[configType];
      
      if (isReact17 && configType === 'viteReact') {
        configTemplate = configTemplate.replace(
          "plugins: [react()]", 
          "plugins: [react({ jsxRuntime: 'classic' })]"
        );
      }
      
      await webcontainerInstance.fs.writeFile('/vite.config.js', configTemplate);
      console.log(`Created vite.config.js with ${configType} template${isReact17 ? ' (React 17 mode)' : ''}`);
    }
  } catch (e) {
    console.error('Error ensuring Vite config:', e);
  }
};

/**
 * Ensure Vite dependencies are installed
 */
export const ensureViteDependencies = async (context: ViteSetupContext): Promise<boolean> => {
  const { webcontainerInstance, setStatus } = context;
  if (!webcontainerInstance) return false;
  
  try {
    const packageJsonText = await webcontainerInstance.fs.readFile('/package.json', 'utf-8');
    const packageJson = JSON.parse(packageJsonText);
    
    const reactVersion = packageJson.dependencies?.react || 'latest';
    const { viteVersion, pluginReactVersion } = getCompatibleVersions(reactVersion);
    console.log(`Using Vite ${viteVersion} with React ${reactVersion}`);
    
    const requiredDeps = {
      'vite': viteVersion,
      '@vitejs/plugin-react': pluginReactVersion,
      'react': reactVersion,
      'react-dom': packageJson.dependencies?.['react-dom'] || reactVersion
    };
    
    let needsInstall = false;
    
    if (!packageJson.dependencies) {
      packageJson.dependencies = {};
    }
    
    for (const [dep, version] of Object.entries(requiredDeps)) {
      if (!packageJson.dependencies[dep]) {
        packageJson.dependencies[dep] = version;
        needsInstall = true;
      }
    }
    
    if (!packageJson.scripts) {
      packageJson.scripts = {};
    }
    
    if (!packageJson.scripts.dev) {
      packageJson.scripts.dev = 'vite';
      needsInstall = true;
    }
    
    if (needsInstall) {
      await webcontainerInstance.fs.writeFile(
        '/package.json',
        JSON.stringify(packageJson, null, 2)
      );
      
      setStatus?.('Installing Vite dependencies...');
      return true;
    }
    
    return false;
  } catch (e) {
    console.error('Error ensuring Vite dependencies:', e);
    return false;
  }
};

/**
 * Determines compatible Vite and plugin-react versions based on React version
 */
function getCompatibleVersions(reactVersion: string): { viteVersion: string, pluginReactVersion: string } {
  const version = reactVersion.replace(/^\^|~/, '');
  const majorVersion = parseInt(version.split('.')[0]);
  
  // For React 17
  if (majorVersion === 17) {
    return {
      viteVersion: '^2.9.15',
      pluginReactVersion: '^1.3.2'
    };
  }
  
  // For React 16
  if (majorVersion === 16) {
    return {
      viteVersion: '^2.8.6',
      pluginReactVersion: '^1.2.0'
    };
  }
  
  // For React 18 and later (or if it's underterminable)
  return {
    viteVersion: '^4.3.9',
    pluginReactVersion: '^4.0.0'
  };
}

/**
 * Ensure index.html exists for Vite project
 * Looks for entry point files and creates an appropriate index.html if missing
 */
export const ensureIndexHtml = async (context: ViteSetupContext): Promise<void> => {
  const { webcontainerInstance, setStatus } = context;
  if (!webcontainerInstance) return;
  
  setStatus?.('Checking for index.html...');
  
  try {
    try {
      await webcontainerInstance.fs.stat('/index.html');
      console.log('index.html exists in root');
      return;
    } catch (e) {
      try {
        await webcontainerInstance.fs.stat('/public/index.html');
        console.log('index.html exists in public folder');
        return;
      } catch (e) {
        console.log('No index.html found, creating one');
      }
    }
    
    let foundEntry = null;
    
    for (const entry of entryPoints) {
      try {
        await webcontainerInstance.fs.stat(entry.path);
        foundEntry = entry;
        console.log(`Found entry point: ${entry.path}`);
        break;
      } catch (e) {
        // Continue checking
      }
    }
    
    let indexHtml = '';
    
    if (foundEntry) {
      const template = htmlTemplates[foundEntry.type] || htmlTemplates.fallback;
      indexHtml = template.replace('{{ENTRY_POINT}}', foundEntry.path);
    } else {
      indexHtml = htmlTemplates.fallback;
    }
    
    await webcontainerInstance.fs.writeFile('/index.html', indexHtml);
    console.log('Created index.html');
    
  } catch (e) {
    console.error('Error ensuring index.html:', e);
  }
};

/**
 * Choose the best available start script - prioritizing Vite scripts
 */
export const chooseViteStartScript = (availableScripts: string[]): string => {
  // Vite-specific scripts first
  const scriptPriority = ['dev', 'start', 'serve', 'develop'];
  
  for (const script of scriptPriority) {
    if (availableScripts.includes(script)) {
      return script;
    }
  }
  
  if (availableScripts.length > 0) {
    return availableScripts[0];
  }
  
  return 'dev'; // Default to 'dev' for Vite
};

/**
 * Configure iframe sandbox permissions for Vite
 */
export const configureViteSandbox = (iframeRef: React.RefObject<HTMLIFrameElement>): void => {
  if (!iframeRef.current) return;
  
  iframeRef.current.sandbox.add('allow-forms');
  iframeRef.current.sandbox.add('allow-modals');
};