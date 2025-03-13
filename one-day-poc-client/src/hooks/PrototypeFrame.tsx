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

      setStatus('Mounting files...');

      try {
        console.log('Files to mount:', JSON.stringify(files, null, 2));

        const mountPromise = webcontainerInstance.mount(files);
        const timeoutPromise = new Promise((_, reject) =>
          setTimeout(
            () =>
              reject(new Error('Mounting files timed out after 10 seconds')),
            10000
          )
        );

        await Promise.race([mountPromise, timeoutPromise]);
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
