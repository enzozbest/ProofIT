import React, { FC, useState, useRef, useEffect } from 'react';
import { SendHorizontal } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';

const InputBox: FC = () => {
  const [text, setText] = useState('');
  const textareaRef = useRef<HTMLTextAreaElement>(null);
  const navigate = useNavigate();
  const [error, setError] = useState<string | null>(null);
  const { isAuthenticated, login } = useAuth();

  useEffect(() => {
    if (textareaRef.current) {
      textareaRef.current.style.height = '40px';
      textareaRef.current.style.height = `${Math.min(textareaRef.current.scrollHeight, 150)}px`;
    }
  }, [text]);

  const handleSubmit = () => {
    if (!isAuthenticated) {
      login(text);
      return;
    }

    if (text.trim()) {
      setError(null);
      navigate('/generate', { state: { initialMessage: text } });
    }
  };

  const handleKeyPress = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSubmit();
    }
  };

  return (
    <div className="flex flex-col items-center w-full max-w-5xl border-black rounded-2xl bg-gray-500 bg-opacity-50 px-5 py-5 shadow-lg">
      <div className="flex items-center w-full">
        <textarea
          ref={textareaRef}
          rows={1}
          placeholder="Tell us what we can do for you?"
          value={text}
          onChange={(e) => setText(e.target.value)}
          onKeyPress={handleKeyPress}
          className="flex-1 bg-transparent px-4 py-3 outline-none placeholder-white resize-none overflow-y-auto"
          style={{ minHeight: '45px', maxHeight: '150px' }}
        />
        <button
          className="p-3 flex items-center justify-center rounded-full bg-transparent hover:bg-gray-800 transition"
          type="button"
        >
        </button>
        <button
          className="p-3 flex items-center justify-center bg-transparent rounded-full hover:bg-gray-800 transition ml-2"
          type="button"
          onClick={handleSubmit}
        >
          <SendHorizontal size={22} />
        </button>
      </div>
      {error && <p className="mt-2 text-red-500 text-sm">{error}</p>}
    </div>
  );
};

export default InputBox;
