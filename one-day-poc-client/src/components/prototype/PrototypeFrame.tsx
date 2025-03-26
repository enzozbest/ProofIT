import { PrototypeFrameProps } from '../../types/Types';
import usePrototypeFrame from '@/hooks/UsePrototypeFrame';

/* The PrototypeFrame component
 *
 * @component
 * @returns {JSX.Element} The prototype iframe only
 */

const PrototypeFrame: React.FC<PrototypeFrameProps> = ({ files }) => {
  const { status, iframeRef, url } = usePrototypeFrame({ files });
  return (
    <div className="w-full h-full flex flex-col">
      <div className="">Status: {status}</div>
      <iframe
        ref={iframeRef}
        src={url}
        className="w-full h-full"
        title="Prototype Preview"
        sandbox="allow-forms allow-modals allow-scripts allow-same-origin"
      />
    </div>
  );
};

export default PrototypeFrame;
