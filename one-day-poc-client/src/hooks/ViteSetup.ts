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
  
  setStatus?.('Performing comprehensive search for index.html...');
  try {
    const indexHtmlPath = await findIndexHtml(webcontainerInstance);
    if (indexHtmlPath) {
      console.log(`Found existing index.html at ${indexHtmlPath}`);
      
      if (indexHtmlPath !== '/index.html') {
        console.log(`Copying index.html from ${indexHtmlPath} to root...`);
        const content = await webcontainerInstance.fs.readFile(indexHtmlPath, 'utf-8');
        await webcontainerInstance.fs.writeFile('/index.html', content);
      }
      return;
    }
    
    console.log(' No index.html found anywhere, creating one...');
    // Find best entry point
    let entryPoint = '/src/index.js';
    let templateType: 'react' | 'javascript' | 'fallback' = 'javascript';
    
    for (const entry of entryPoints) {
      try {
        await webcontainerInstance.fs.stat(entry.path);
        entryPoint = entry.path;
        templateType = entry.type;
        console.log(`Found entry point: ${entryPoint}`);
        break;
      } catch (e) {
        // Entry point doesn't exist
      }
    }
    
    let template = htmlTemplates[templateType].replace('{{ENTRY_POINT}}', entryPoint);
    
    await webcontainerInstance.fs.writeFile('/index.html', template);
    console.log(`Created index.html with ${templateType} template`);
  } catch (e) {
    console.error('Error in ensureIndexHtml:', e);
  }
};

/**
 * Perform a comprehensive search for index.html in the project
 */
export const findIndexHtml = async (webcontainerInstance: WebContainer): Promise<string | null> => {
  console.log('Starting comprehensive search for index.html...');
  const commonLocations = [
    '/index.html',
    '/public/index.html',
    '/src/index.html',
    '/app/index.html',
    '/dist/index.html',
    '/client/index.html',
    '/static/index.html'
  ];
  try {
    const rootFiles = await webcontainerInstance.fs.readdir('/');
    console.log('Files in root directory:', rootFiles);
    
    if (rootFiles.includes('index.html')) {
      console.log('Found index.html in root directly');
      return '/index.html';
    }
    
    if (rootFiles.includes('src')) {
      const srcFiles = await webcontainerInstance.fs.readdir('/src');
      console.log('Files in src directory:', srcFiles);
      
      if (srcFiles.includes('index.html')) {
        console.log('Found index.html in /src directory');
        return '/src/index.html';
      }
    }
  } catch (e) {
    console.error('Error listing directory contents:', e);
  }
  
  for (const location of commonLocations) {
    try {
      await webcontainerInstance.fs.stat(location);
      console.log(`✅ Found index.html at ${location}`);
      return location;
    } catch (e) {
      // File doesn't exist at this location
    }
  }
  
  try {
    const rootEntries = await webcontainerInstance.fs.readdir('/', { withFileTypes: true });
    for (const entry of rootEntries) {
      if (entry.isDirectory) {
        const path = `/${entry.name}`;
        const result = await recursiveSearch(webcontainerInstance, path, 2);
        if (result) return result;
      }
    }
  } catch (e) {
    console.error('Error in directory search:', e);
  }
  
  return null;
};

/**
 * Fixed recursive search function
 */
async function recursiveSearch(
  webcontainerInstance: WebContainer, 
  dir: string, 
  maxDepth: number
): Promise<string | null> {
  if (maxDepth <= 0) return null;
  
  try {
    const entries = await webcontainerInstance.fs.readdir(dir, { withFileTypes: true });
    
    for (const entry of entries) {
      if (!entry.isDirectory && entry.name === 'index.html') {
        const path = `${dir}${dir.endsWith('/') ? '' : '/'}index.html`;
        console.log(`Found index.html during recursive search at ${path}`);
        return path;
      }
    }
    
    for (const entry of entries) {
      if (entry.isDirectory) {
        const subdir = `${dir}${dir.endsWith('/') ? '' : '/'}${entry.name}`;
        const result = await recursiveSearch(webcontainerInstance, subdir, maxDepth - 1);
        if (result) return result;
      }
    }
  } catch (e) {
    console.error(`Error searching directory ${dir}:`, e);
  }
  
  return null;
}

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