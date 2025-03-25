import { Minus, Square, X, RefreshCw } from 'lucide-react';

/* The Prototype Window component is a decorative component that resembles a browser window. 
 *
 * @component
 * @returns {JSX.Element} The prototype window
 */

export default function PrototypeWindow({
  children,
}: {
  children: React.ReactNode,
}){

  return (
    <div className="w-full h-full flex flex-col me-8 bg-white/15 backdrop-blur-lg rounded-xl border-[0.2vmin] border-white overflow-hidden text-white">
      <div className="w-full flex flex-row justify-between p-1 px-3 gap-1 ">
        <button 
          className="bg-transparent rounded-full hover:bg-white hover:text-[#731ecb] transition p-1 rounded-xl">
        <RefreshCw size={12} strokeWidth={2} className="text-neutral/50" />
        </button>
        <div className='gap-1 flex flex-row p-1'>
          <Minus size={14} strokeWidth={2} className="text-neutral/50" />
          <Square size={12} strokeWidth={2} className="text-neutral/50" />
          <X size={14} strokeWidth={2} className="text-neutral/50" />
        </div>
      </div>
      <div className="h-10  w-100px  h-[2px] border-b-[0.2vmin] border-white "></div>
      <div className="h-[300px] w-[200px] flex items-center justify-center ">
        {children}
      </div>
    </div>
  );
}
