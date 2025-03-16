import React, { useEffect, useState, useRef } from 'react';
import { PrototypeFrameProps } from '../types/Types';
import { useWebContainer } from './useWebContainer';

/**
 * PrototypeFrame Component
 *
 * Renders a prototype in an isolated WebContainer environment within an iframe.
 * This component handles:
 * - Loading prototype files from the server
 * - Setting up a development environment with necessary dependencies
 * - Starting a development server
 * - Displaying the prototype in an iframe
 *
 * @component
 * @param {Object} props
 * @param {string} [props.width='100%']
 * @param {string} [props.height='100%']
 */
const PrototypeFrame: React.FC<PrototypeFrameProps> = ({
  files,
  width = '100%',
  height = '100%',
}) => {
  const [url, setUrl] = useState('');
  const [status, setStatus] = useState('Initialising...');
  const { instance: webcontainerInstance, loading, error } = useWebContainer();
  const iframeRef = useRef<HTMLIFrameElement>(null);

  /**
   * Normalises file structure to ensure compatibility with WebContainer API
   * Properly handles nested paths like "src/index.js" by creating directory structure
   */
  const normalizeFiles = (files: Record<string, any>): Record<string, any> => {
    const result: Record<string, any> = {};
    
    Object.keys(files).forEach(path => {
      const fileData = files[path];
      
      if (path.includes('/')) {
        const segments = path.split('/');
        const fileName = segments.pop() || '';
        
        let current = result;
        for (const segment of segments) {
          if (!current[segment]) {
            current[segment] = { directory: {} };
          }
          current = current[segment].directory;
        }
        
        if (fileData.file) {
          const contents = fileData.file.contents || '';
          current[fileName] = { file: { contents } };
        }
      } else {
        if (fileData.file && Object.keys(fileData.file).length === 0) {
          result[path] = { file: { contents: "" } };
        } else if (fileData.file && fileData.file.contents) {
          result[path] = { file: { contents: fileData.file.contents } };
        } else if (fileData.directory) {
          result[path] = { directory: normalizeFiles(fileData.directory) };
        }
      }
    });
    
    return result;
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

      setStatus('Normalizing files...');
      const normalizedFiles = normalizeFiles(files);
      
      setStatus('Mounting files...');

      try {
        console.log('Files to mount:', JSON.stringify(normalizedFiles, null, 2));
        await webcontainerInstance.mount(normalizedFiles);
        console.log('Files mounted successfully');

        setStatus('Installing dependencies...');
        const installProcess = await webcontainerInstance.spawn('npm', [
          'install',
        ]);
        const exitCode = await installProcess.exit;

        if (exitCode !== 0) {
          throw new Error(`npm install failed with exit code ${exitCode}`);
        }

        setStatus('Starting development server...');
        await webcontainerInstance.spawn('npm', ['run', 'start']);
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

  if (loading) {
    return <div>Loading WebContainer...</div>;
  }

  if (error) {
    return <div>Error initializing WebContainer: {error.message}</div>;
  }

  return (
    <div
      style={{
        width: '100%',
        height: '100%',
        display: 'flex',
        flexDirection: 'column',
      }}
    >
      <div style={{ marginBottom: '10px' }}>Status: {status}</div>
      <iframe
        ref={iframeRef}
        src={url}
        style={{
          flexGrow: 1,
          width: '100%',
          border: '1px solid #ccc',
          borderRadius: '4px',
        }}
        title="Prototype Preview"
        sandbox="allow-scripts allow-same-origin"
      />
    </div>
  );
};

export default PrototypeFrame;
