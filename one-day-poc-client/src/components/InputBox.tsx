import React, { FC, useState, useRef, useEffect } from 'react';
import { Paperclip, SendHorizontal } from 'lucide-react';

const InputBox: FC = () => {
  const [text, setText] = useState('');
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  useEffect(() => {
    if (textareaRef.current) {
      textareaRef.current.style.height = '40px';
      textareaRef.current.style.height = `${Math.min(textareaRef.current.scrollHeight, 150)}px`;
    }
  }, [text]);

  return (
    <div className="flex items-center w-full max-w-5xl border-black rounded-2xl bg-gray-500 bg-opacity-50 px-5 py-5 shadow-lg">
      <textarea
        ref={textareaRef}
        rows={1}
        placeholder="Tell us what we can do for you?"
        value={text}
        onChange={(e) => setText(e.target.value)}
        className="flex-1 bg-transparent px-4 py-3 text-white outline-none placeholder-white resize-none overflow-y-auto"
        style={{ minHeight: '45px', maxHeight: '150px' }}
      />
      <button
        className="p-3 flex items-center justify-center rounded-full bg-transparent hover:bg-gray-800 transition"
        type="button"
      >
        <Paperclip size={22} className="text-white" />
      </button>
      <button
        className="p-3 flex items-center justify-center bg-transparent rounded-full hover:bg-gray-800  transition ml-2"
        type="button"
      >
        <SendHorizontal size={22} className="text-white" />
      </button>
    </div>
  );
};

export default InputBox;
