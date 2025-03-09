import React, { useEffect, useState } from 'react';
import { PrototypeFrameProps } from './Types';
import { WebContainer } from '@webcontainer/api';
import { getPrototypeFiles } from '../api/FrontEndAPI';

/**
 * PrototypeFrame Component
 * 
 * Renders a prototype in an isolated WebContainer environment within an iframe.
 * This component handles:
 * - Booting up a WebContainer instance
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
const PrototypeFrame: React.FC<PrototypeFrameProps> = ({ files, width = '100%', height = '100%' }) => {
    const [url, setUrl] = useState('');
    const [webcontainerInstance, setWebcontainerInstance] = useState<WebContainer | null>(null);

    /**
     * Effect to initialise WebContainer on component mount
     * Boots up a new WebContainer instance and stores it in state
     */
    useEffect(() => {
        async function bootWebContainer() {
            try {
                if (!webcontainerInstance) {
                    const instance = await WebContainer.boot();
                    setWebcontainerInstance(instance);
                    console.log('WebContainer booted successfully');
                }
            } catch (error) {
                console.error('Failed to boot WebContainer:', error);
            }
        }
        
        bootWebContainer();

        return () => {
            if (webcontainerInstance) {
                // idk yet if we will need to clean up anything but this is where it's done
                console.log('WebContainer cleanup');
            }
        };
    }, []); // Empty dependency array for single mount

    /**
     * Effect to load and mount prototype files when WebContainer is ready
     * - Fetches prototype files from server
     * - Sets up package.json if not provided
     * - Mounts files in WebContainer
     * - Installs dependencies
     * - Starts development server
     */
    useEffect(() => {
        async function loadFiles() {
            if (!webcontainerInstance || !files) return;

            try {
                if (!files['package.json']) {
                    files['package.json'] = {
                        file: {
                            contents: JSON.stringify({
                                name: 'prototype-app',
                                type: 'module',
                                scripts: {
                                    dev: 'vite'
                                },
                                dependencies: {
                                    'react': '^18.2.0',
                                    'vite': '^4.0.0'
                                }
                            })
                        }
                    };
                }

                await webcontainerInstance.mount(files);

                const installProcess = await webcontainerInstance.spawn('npm', ['install']);
                await installProcess.exit;

                const server = await webcontainerInstance.spawn('npm', ['run', 'dev']);
                
                server.output.pipeTo(new WritableStream({
                    write(data) {
                        const text = data.toString();
                        if (text.includes('Local:')) {
                            const matches = text.match(/http:\/\/localhost:\d+/);
                            if (matches) {
                                setUrl(matches[0]);
                            }
                        }
                    }
                }));
            } catch (error) {
                console.error('Failed to load files:', error);
            }
        }

        if (webcontainerInstance && files) {
            loadFiles();
        }
    }, [webcontainerInstance, files]);

    return (
        <iframe
            src={url}
            style={{
                width: width,
                height: height,
                border: '1px solid #ccc',
                borderRadius: '4px'
            }}
            title="Prototype Preview"
            sandbox="allow-scripts allow-same-origin"
        />
    );
};

export default PrototypeFrame;