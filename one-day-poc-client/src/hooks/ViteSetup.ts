import { WebContainer } from '@webcontainer/api';

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
      const viteConfig = `
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    host: true,
    hmr: {
      clientPort: 443, // Fix for WebContainer environment
    }
  }
});
`;
      await webcontainerInstance.fs.writeFile('/vite.config.js', viteConfig);
      console.log('Created vite.config.js');
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
    
    const requiredDeps = {
      'vite': 'latest',
      '@vitejs/plugin-react': 'latest',
      'react': packageJson.dependencies?.react || 'latest',
      'react-dom': packageJson.dependencies?.['react-dom'] || 'latest'
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