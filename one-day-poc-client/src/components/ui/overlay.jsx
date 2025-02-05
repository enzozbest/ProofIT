import React from "react";
import {
    X
} from "lucide-react";

export const MutedOverlay = ({ isVisible, onClose, children }) => {
    if (!isVisible) return null;

    return (
        <div className=" absolute w-full h-full bg-black bg-opacity-5 backdrop-blur-md flex flex-col items-center justify-center z-10" >
            <div className="p-3 w-96 flex flex-col items-center justify-center">
                {children}
                <button 
                    onClick={onClose} 
                    className="mt-4 px-4 py-2 text-destructive rounded-md"
                >
                    <X />
                </button>
            </div>
        </div>
    );
};

export default MutedOverlay;
