import React from "react";
import { Paperclip, SendHorizontal } from "lucide-react";

const InputBox = () => {
  return (
    <div className="flex items-center border rounded-lg p-4 w-full max-w-2xl my-10 h-14">
      <input
        type="text"
        placeholder="Tell us what we can do for you."
        className="flex-1 bg-transparent px-4 py-2 outline-none text-lg"
      />
      <button className="p-3 bg-white flex items-center justify-center w-12 h-12 border rounded-lg">
        <Paperclip size={22} />
      </button>
      <button className="p-3 bg-white flex items-center justify-center w-12 h-12 border rounded-lg ml-2">
        <SendHorizontal size={22} />
      </button>
    </div>
  );
};

export default InputBox;
