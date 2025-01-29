import React, { useEffect, useRef, useState } from 'react';

const PrototypeFrame = ({htmlContent, cssContent, jsContent, width = '100%', height = '100%' }) => {
    const iframeRef = useRef(null);

    /*
    const [content, setContent] = useState({ htmlContent: '', cssContent: '', jsContent: '' });
    useEffect(() => {
        const fetchIframeContent = async () => {
            try {
                const response = await fetch(url);
                const data = await response.json();
                setContent({
                    htmlContent: data.htmlContent,
                    cssContent: data.cssContent,
                    jsContent: data.jsContent
                })
            }
            catch (error) {
                console.error('Error fetching iframe content:', error);
            }
        };
        fetchIframeContent();
    }, [url]);
    */

    useEffect(() => {
        const iframeDoc = iframeRef.current.contentDocument;
        iframeDoc.open();
        iframeDoc.write(`
            <!DOCTYPE html>
            <html>
                <head>
                    <style>
                        ${cssContent}
                    </style>
                </head>
                <body>
                    <p>Prototype Frame</p>
                    ${htmlContent}
                    <script>
                        ${jsContent}
                    </script>
                </body>
            </html>
        `);
        iframeDoc.close();
    }, [htmlContent, cssContent, jsContent]);

    return (
        <iframe
        ref={iframeRef}
        data-testid="prototype-iframe"
        width={width}
        height={height}
        style={{ border: 'none' }}
        title="Prototype Frame"
        />
    );
};

export default PrototypeFrame;