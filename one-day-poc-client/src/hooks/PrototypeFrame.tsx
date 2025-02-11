import React, { useEffect,useState } from 'react';
import { PrototypeFrameProps } from './Types';

const PrototypeFrame: React.FC<PrototypeFrameProps> = ({ prototypeId, width = '100%', height = '100%' }) => {
    const [url, setUrl] = useState('');

    useEffect(() => {
        setUrl(`http://localhost:8000/webcontainer/${prototypeId}`);
    }, [prototypeId]);


    return (
        <iframe
            src={url}
            style={{
                width: width,
                height: height,
                border: '1px solid #ccc',
                borderRadius: '4px',
                color: 'black',
                fontSize: '50px'
            }}
            title="Prototype Preview"
            sandbox="allow-scripts allow-same-origin"
        />
    );
};

export default PrototypeFrame;