import React, { FC } from 'react';
import { useAuth } from '../../contexts/AuthContext';
import { useNavigate } from 'react-router-dom';

type GeneratedPromptsProps = {
  prompts: string[];
};


/**
 * GeneratedPrompts component displays a horizontally scrollable list of prompt buttons.
 * 
 * This component renders an array of text prompts as interactive buttons, typically used
 * for suggested actions or example queries the user can click on. The buttons are styled
 * with hover effects and include a right arrow indicator.
 * 
 * @returns {JSX.Element} A horizontally scrollable container with clickable prompt buttons
 */
const GeneratedPrompts: FC<GeneratedPromptsProps> = ({ prompts }) => {
  const navigate = useNavigate();
  const { isAuthenticated, login } = useAuth();

  const handleSubmit = (promptText: string) => {
    if (!isAuthenticated) {
      login(promptText);
      return;
    }
    navigate('/generate', { state: { initialMessage: promptText, isPredefined: true } });
  }

  return (
    <div className="mt-5 flex md:flex-nowrap flex-wrap gap-4 w-full max-w-6xl overflow-x-auto justify-center pb-9 no-scrollbar text-sm">
      {prompts.map((text, index) => (
        <button
          key={index}
          className="border-2 border-white px-6 py-4 rounded-lg bg-transparent  text-center whitespace-nowrap hover:bg-white/20 hover:border-white hover: transition duration-300"
          onClick={() => handleSubmit(text)}
        >
          {text} →
        </button>
      ))}
    </div>
  );
};

export default GeneratedPrompts;
