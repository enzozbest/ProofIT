import React, { useEffect, useState, useRef } from 'react';
import { PrototypeFrameProps } from '../types/Types';
import { useWebContainer } from './UseWebContainer';
import { normaliseFiles } from './FileHandler';
import { WebContainerProcess } from '@webcontainer/api';

/**
 * Hook for creating and managing a prototype frame using WebContainer
 */
const usePrototypeFrame = <T extends PrototypeFrameProps>(props: T) => {
  const { files } = props;
  const [url, setUrl] = useState('');
  const [status, setStatus] = useState('Initialising...');
  const { instance: webcontainerInstance, loading, error } = useWebContainer();
  const iframeRef = useRef<HTMLIFrameElement>(null);
  
  // Track active processes for cleanup
  const activeProcessesRef = useRef<WebContainerProcess[]>([]);

  /**
   * Effect to set up server-ready listener
   */
  useEffect(() => {
    if (!webcontainerInstance) return;

    webcontainerInstance.on('server-ready', (port, serverUrl) => {
      console.log('Server ready on port', port, 'at URL', serverUrl);
      setUrl(serverUrl);
      setStatus('Server running');
    });
  }, [webcontainerInstance]);

  /**
   * Effect to load and mount prototype files when WebContainer is ready
   */
  useEffect(() => {
    if (webcontainerInstance && files) {
      console.log('WebContainer and files are both available, mounting files');
      loadFiles();
    } else {
      console.log('Cannot mount files yet:', {
        hasFiles: !!files,
        hasWebContainer: !!webcontainerInstance,
      });
    }

    async function loadFiles() {
      if (!webcontainerInstance || !files) return;

      try {
        await resetEnvironment();
        await mountFiles();
        await installDependencies();
        await startServer();
        configureSandbox();
      } catch (error: unknown) {
        handleError(error);
      }
    }
  }, [webcontainerInstance, files]);

  /**
   * Reset the WebContainer environment
   */
  const resetEnvironment = async () => {
    if (!webcontainerInstance) return;
    
    setStatus('Resetting environment...');
    setUrl('');
    
    // Kill all running processes
    await killActiveProcesses();
    
    // Clean filesystem
    await cleanFileSystem();
  };

  /**
   * Kill all active processes
   */
  const killActiveProcesses = async () => {
    for (const process of activeProcessesRef.current) {
      try {
        process.kill();
      } catch (e) {
        console.log('Error killing process:', e);
      }
    }
    
    activeProcessesRef.current = [];
  };

  /**
   * Clean the filesystem for a fresh start
   */
  const cleanFileSystem = async () => {
    if (!webcontainerInstance) return;
    
    try {
      const entries = await webcontainerInstance.fs.readdir('/');
      console.log('Current root entries:', entries);
      
      // Remove critical directories
      for (const dir of ['src', 'public', 'node_modules']) {
        if (entries.includes(dir)) {
          await webcontainerInstance.fs.rm(`/${dir}`, { recursive: true, force: true });
        }
      }
      
      // Remove other non-hidden files
      for (const entry of entries) {
        if (!entry.startsWith('.') && !['src', 'public', 'node_modules'].includes(entry)) {
          try {
            await webcontainerInstance.fs.rm(`/${entry}`);
            console.log(`Removed file: ${entry}`);
          } catch (e) {
            console.log(`Error removing ${entry}:`, e);
          }
        }
      }
      
      console.log('Filesystem reset complete');
    } catch (e) {
      console.log('Error during filesystem reset:', e);
    }
  };

  /**
   * Mount files to the WebContainer
   */
  const mountFiles = async () => {
    if (!webcontainerInstance || !files) return;
    
    console.log('Files to normalise:', files);

    setStatus('Normalising files...');
    const normalisedFiles = normaliseFiles(files);
    
    console.log('Files to mount: ', normalisedFiles);
    setStatus('Mounting files...');
    await webcontainerInstance.mount(normalisedFiles);
    console.log('Files mounted successfully');
  };

  /**
   * Install dependencies
   */
  const installDependencies = async () => {
    if (!webcontainerInstance) return;
    
    setStatus('Installing dependencies...');
    const installProcess = await webcontainerInstance.spawn('npm', ['install']);
    
    // Track and log the process
    activeProcessesRef.current.push(installProcess);
    installProcess.output.pipeTo(
      new WritableStream({
        write(data) {
          console.log('Install output:', data);
        }
      })
    );
    
    const exitCode = await installProcess.exit;
    
    if (exitCode !== 0) {
      throw new Error(`npm install failed with exit code ${exitCode}`);
    }
  };

  /**
   * Start the development server
   */
  const startServer = async () => {
    if (!webcontainerInstance) return;
    
    setStatus('Starting development server...');
    
    const availableScripts = await getAvailableScripts();
    const startScript = chooseStartScript(availableScripts);
    
    console.log(`Using script: ${startScript}`);
    await runServerWithAutoInstall(startScript);
  };

  const runServerWithAutoInstall = async (scriptName: string) => {
    if (!webcontainerInstance) return;
    
    const startProcess = await webcontainerInstance.spawn('npm', ['run', scriptName]);
    activeProcessesRef.current.push(startProcess);
    setStatus('Running...');
    
    let outputBuffer = '';
    
    startProcess.output.pipeTo(
      new WritableStream({
        write(data) {
          console.log('Server output:', data);
          outputBuffer += data;
          
          // Check for missing dependency pattern
          const missingDep = detectMissingDependency(outputBuffer);
          if (missingDep) {
            console.log(`Detected missing dependency: ${missingDep}`);
            // Reset the buffer after finding a dependency
            outputBuffer = '';
            
            // Auto-install the dependency and restart server
            (async () => {
              await killActiveProcesses();
              await installSpecificDependency(missingDep);
              await runServerWithAutoInstall(scriptName);
            })();
          }
        }
      })
    );
    
    startProcess.exit.then((code) => {
      console.log(`Server process exited with code ${code}`);
      if (code !== 0 && !outputBuffer.includes('missing dependency')) {
        setStatus(`Server crashed with exit code ${code}`);
      }
    });
  };

  const detectMissingDependency = (output: string): string | null => {
    const patterns = [
      /Cannot find module '([^']+)'/i,
      /Error: Cannot find module '([^']+)'/i,
      /Module not found: Error: Can't resolve '([^']+)'/i,
      /Module not found: Error: Can't resolve '([^']+)'/i,
      /Cannot resolve module '([^']+)'/i
    ];
    
    for (const pattern of patterns) {
      const match = output.match(pattern);
      if (match && match[1]) {
        const dep = match[1];
        
        if (!dep.startsWith('./') && !dep.startsWith('../') && !dep.startsWith('/')) {
          const packageName = dep.startsWith('@') 
            ? dep.split('/').slice(0, 2).join('/') 
            : dep.split('/')[0];
            
          return packageName;
        }
      }
    }
    
    return null;
  };

  const installSpecificDependency = async (dependency: string) => {
    if (!webcontainerInstance) return;
    
    setStatus(`Installing missing dependency: ${dependency}...`);
    
    try {
      await updatePackageJson(dependency);
    } catch (e) {
      console.error('Error updating package.json:', e);
    }
    
    const installProcess = await webcontainerInstance.spawn('npm', ['install', dependency]);
    
    installProcess.output.pipeTo(
      new WritableStream({
        write(data) {
          console.log(`Installing ${dependency} output:`, data);
        }
      })
    );
    
    const exitCode = await installProcess.exit;
    
    if (exitCode !== 0) {
      throw new Error(`Failed to install ${dependency} with exit code ${exitCode}`);
    }
    
    setStatus(`Dependency ${dependency} installed successfully. Restarting...`);
  };

  const updatePackageJson = async (dependency: string) => {
    if (!webcontainerInstance) return;
    
    try {
      const packageJsonText = await webcontainerInstance.fs.readFile('/package.json', 'utf-8');
      const packageJson = JSON.parse(packageJsonText);
      
      if (!packageJson.dependencies) {
        packageJson.dependencies = {};
      }
      
      if (!packageJson.dependencies[dependency]) {
        packageJson.dependencies[dependency] = "latest";
        
        await webcontainerInstance.fs.writeFile(
          '/package.json', 
          JSON.stringify(packageJson, null, 2)
        );
        
        console.log(`Updated package.json with ${dependency}`);
      }
    } catch (e) {
      console.error('Error updating package.json:', e);
    }
  };

  /**
   * Get available npm scripts
   */
  const getAvailableScripts = async () => {
    try {
      const packageJsonText = await webcontainerInstance?.fs.readFile('/package.json', 'utf-8');
      if (packageJsonText) {
        const packageJson = JSON.parse(packageJsonText);
        return Object.keys(packageJson.scripts || {});
      }
    } catch (e) {
      console.log('Error reading package.json:', e);
    }
    return [];
  };

  /**
   * Choose the best available start script
   */
  const chooseStartScript = (availableScripts: string[]) => {
    // Priority order of scripts to try
    const scriptPriority = ['start', 'dev', 'serve', 'develop'];
    
    for (const script of scriptPriority) {
      if (availableScripts.includes(script)) {
        return script;
      }
    }
    
    // If no recognized script is found but we have scripts
    if (availableScripts.length > 0) {
      return availableScripts[0]; // Use the first available script
    }
    
    // Fallback to 'start' even if not found - will throw appropriate error
    return 'start';
  };

  /**
   * Configure iframe sandbox permissions
   */
  const configureSandbox = () => {
    if (!iframeRef.current) return;
    
    iframeRef.current.sandbox.add('allow-forms');
    iframeRef.current.sandbox.add('allow-modals');
  };

  /**
   * Handle errors in a user-friendly way
   */
  const handleError = (error: unknown) => {
    console.error('Error:', error);
    
    let errorMessage = 'Unknown error occurred';
    
    if (error instanceof Error) {
      errorMessage = error.message;
    } else if (typeof error === 'string') {
      errorMessage = error;
    } else if (error && typeof error === 'object' && 'message' in error) {
      errorMessage = String(error.message);
    }
    
    setStatus(`Error: ${errorMessage}`);
  };

  return {
    status,
    iframeRef,
    url,
    loading,
    error: error || undefined
  };
};

export default usePrototypeFrame;
