import React, { FC } from 'react';
import { Paperclip, SendHorizontal } from 'lucide-react';

const InputBox: FC = () => {
  return (
    <div className="flex items-center w-full max-w-3xl rounded-lg border px-6 py-4">
      <input
        type="text"
        placeholder="Tell us what we can do for you."
        className="flex-1 bg-transparent px-4 py-2 outline-none"
      />
      <button
        className="p-3 flex items-center justify-center rounded-full"
        type="button"
      >
        <Paperclip size={22} />
      </button>
      <button
        className="p-3 flex items-center justify-center rounded-full ml-2"
        type="button"
      >
        <SendHorizontal size={22} />
      </button>
    </div>
  );
};

export default InputBox;
