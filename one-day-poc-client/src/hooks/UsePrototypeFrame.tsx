import React, { useEffect, useState, useRef } from 'react';
import { PrototypeFrameProps } from '../types/Types';
import { useWebContainer } from './UseWebContainer';
import { normaliseFiles } from './FileHandler';
import { WebContainerProcess } from '@webcontainer/api';

const usePrototypeFrame = <T extends PrototypeFrameProps>(props: T) => {
  const { files } = props;
  const [url, setUrl] = useState('');
  const [status, setStatus] = useState('Initialising...');
  const { instance: webcontainerInstance, loading, error } = useWebContainer();
  const iframeRef = useRef<HTMLIFrameElement>(null);
  
  // Use a ref to track active processes
  const activeProcessesRef = useRef<WebContainerProcess[]>([]);

  /**
   * Function to reset the WebContainer
   */
  const resetWebContainer = async () => {
    if (!webcontainerInstance) return;
    
    setStatus('Resetting environment...');
    setUrl('');
    
    for (const process of activeProcessesRef.current) {
      try {
        process.kill();
      } catch (e) {
        console.log('Error killing process:', e);
      }
    }
    
    activeProcessesRef.current = [];
    
    try {
      const entries = await webcontainerInstance.fs.readdir('/');
      console.log('Current root entries:', entries);
      
      if (entries.includes('src')) {
        await webcontainerInstance.fs.rm('/src', { recursive: true, force: true });
      }
      if (entries.includes('public')) {
        await webcontainerInstance.fs.rm('/public', { recursive: true, force: true });
      }
      if (entries.includes('node_modules')) {
        await webcontainerInstance.fs.rm('/node_modules', { recursive: true, force: true });
      }
      
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
      
      console.log('Filesystem selectively reset');
    } catch (e) {
      console.log('Error during selective reset:', e);
    }
  };

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
    async function loadFiles() {
      if (!webcontainerInstance || !files) return;

      await resetWebContainer();

      setStatus('Normalising files...');
      const normalisedFiles = normaliseFiles(files);

      // console.log('Normalised files:', normalisedFiles);

      setStatus('Mounting files...');

      try {
        // console.log('Files to mount:', JSON.stringify(normalisedFiles, null, 2));
        await webcontainerInstance.mount(normalisedFiles);
        console.log('Files mounted successfully');

        setStatus('Installing dependencies...');
        const installProcess = await webcontainerInstance.spawn('npm', ['install']);
        // Track the active process
        activeProcessesRef.current.push(installProcess);
        
        const exitCode = await installProcess.exit;

        if (exitCode !== 0) {
          throw new Error(`npm install failed with exit code ${exitCode}`);
        }

        setStatus('Starting development server...');
        const startProcess = await webcontainerInstance.spawn('npm', ['run', 'start']);
        // Track the active process
        activeProcessesRef.current.push(startProcess);

        setStatus('Running...');
        startProcess.output.pipeTo(
          new WritableStream({
            write(data) {
              console.log('Server output:', data);
            }
          })
        );

        if (iframeRef.current) {
          iframeRef.current.sandbox.add('allow-scripts');
          iframeRef.current.sandbox.add('allow-same-origin');
          iframeRef.current.sandbox.add('allow-forms');
          iframeRef.current.sandbox.add('allow-modals');
        }
      } catch (error: unknown) {
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
      }
    }

    if (webcontainerInstance && files) {
      console.log('WebContainer and files are both available, mounting files', {
        hasFiles: !!files,
        hasWebContainer: !!webcontainerInstance,
      });
      loadFiles();
    } else {
      console.log('Cannot mount files yet:', {
        hasFiles: !!files,
        hasWebContainer: !!webcontainerInstance,
      });
    }
  }, [webcontainerInstance, files]);

  return {
    status,
    iframeRef,
    url
  };
};

export default usePrototypeFrame;
