import React, { FC } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';

type GeneratedPromptsProps = {
  prompts: string[];
};

const GeneratedPrompts: FC<GeneratedPromptsProps> = ({ prompts }) => {
  const navigate = useNavigate();
  const { isAuthenticated, login } = useAuth();

  const handleSubmit = (promptText: string) => {
    if (!isAuthenticated) {
      login(promptText);
      return;
    }
    navigate('/generate', { state: { initialMessage: promptText } });
  }

  return (
    <div className="mt-5 flex flex-nowrap gap-4 w-full max-w-6xl overflow-x-auto justify-center pb-9">
      {prompts.map((text, index) => (
        <button
          key={index}
          className="border border-white/50 px-5 py-3 rounded-full bg-transparent text-center whitespace-nowrap hover:bg-white/10 transition duration-300 flex items-center"
          onClick={() => handleSubmit(text)}
        >
          {text} →
        </button>
      ))}
    </div>
  );
};

export default GeneratedPrompts;
