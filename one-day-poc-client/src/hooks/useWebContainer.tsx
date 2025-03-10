import { useState, useEffect } from 'react';
import { WebContainer } from '@webcontainer/api';

let globalWebContainerInstance: WebContainer | null = null;
let bootPromise: Promise<WebContainer> | null = null;

/**
 * Custom hook to manage WebContainer instance lifecycle
 * 
 * @returns {Object} The WebContainer instance and loading state
 */
export function useWebContainer() {
  const [instance, setInstance] = useState<WebContainer | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<Error | null>(null);

  useEffect(() => {
    let mounted = true;
    
    async function initWebContainer() {
      try {
        if (globalWebContainerInstance) {
          if (mounted) {
            setInstance(globalWebContainerInstance);
            setLoading(false);
          }
          return;
        }
        
        if (!bootPromise) {
          console.log('Starting WebContainer boot process');
          bootPromise = WebContainer.boot().then(newInstance => {
            console.log('WebContainer booted successfully');
            globalWebContainerInstance = newInstance;
            return newInstance;
          }).catch(err => {
            console.error('WebContainer boot failed:', err);
            bootPromise = null;
            throw err;
          });
        } else {
          console.log('Joining existing WebContainer boot process');
        }
        
        const webContainerInstance = await bootPromise;
        
        if (mounted) {
          setInstance(webContainerInstance);
          setLoading(false);
        }
      } catch (err) {
        console.error('Error in WebContainer initialisation:', err);
        
        if (mounted) {
          setError(err instanceof Error ? err : new Error(String(err)));
          setLoading(false);
        }
      }
    }
    
    initWebContainer();
    
    return () => {
      mounted = false;
    };
  }, []);

  return {
    instance,
    loading,
    error,
    isReady: !!instance && !loading
  };
}