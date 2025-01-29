import React from "react";
import { Paperclip } from 'lucide-react'
import { SendHorizontal } from 'lucide-react'

const InputBox = () => {
  return (
    <div className="flex items-center border rounded-lg p-2 w-full max-w-lg my-10">
      <input
        type="text"
        placeholder="Tell us what we can do for you."
        className="flex-1 bg-transparent px-2 py-1 outline-none"
      />
      <button className="p-2 bg-white items-center justify-center w-8 h-8">
        <Paperclip size={18} />
      </button>
      <button className="p-2 bg-white items-center justify-center w-8 h-8 ">
        <SendHorizontal size={18} />
      </button>

    </div>
  );
};

export default InputBox;
