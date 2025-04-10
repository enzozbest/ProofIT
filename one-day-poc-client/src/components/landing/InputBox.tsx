import React, { FC, useState, useRef, useEffect } from 'react';
import { SendHorizontal } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import { useConversation } from '../../contexts/ConversationContext';

/**
 * GeneratedPrompts component displays a horizontally scrollable list of prompt buttons.
 *
 * This component renders an array of text prompts as interactive buttons, typically used
 * for suggested actions or example queries the user can click on. The buttons are styled
 * with hover effects and include a right arrow indicator.
 *
 * @component
 * @param {Object} props - Component properties
 * @param {string[]} props.prompts - Array of text strings to display as prompt buttons
 *
 * @returns {JSX.Element} A horizontally scrollable container with clickable prompt buttons
 */
const InputBox: FC<{ testError?: string | null }> = ({ testError = null }) => {
  const [text, setText] = useState('');
  const textareaRef = useRef<HTMLTextAreaElement>(null);
  const navigate = useNavigate();
  const { isAuthenticated, login } = useAuth();
  const [error, setError] = useState<string | null>(testError);
  const { createConversation } = useConversation();

  useEffect(() => {
    if (testError !== null) {
      setError(testError);
    }
  }, [testError]);

  /**
   * Handle form submission
   * - Redirects to sign-in if not authenticated
   * - Navigates to generate page with text input if authenticated
   */
  const handleSubmit = () => {
    if (!text.trim()) {
      return;
    }

    if (!isAuthenticated) {
      login(text, false);
      return;
    }

    if (text.trim()) {
      setError(null);
      createConversation();
      console.log("navigating to generate page, setting isPredefined to false")
      navigate('/generate', { state: { initialMessage: text, isPredefined: false } });
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
