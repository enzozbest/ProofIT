import React, { FC } from 'react';

type GeneratedPromptsProps = {
  prompts: string[];
};

const GeneratedPrompts: FC<GeneratedPromptsProps> = ({ prompts }) => {
  return (
    <div className="mt-5 flex flex-nowrap gap-4 w-full max-w-5xl overflow-x-auto justify-center pb-9">
      {prompts.map((text, index) => (
        <button
          key={index}
          className="border-2 border-white px-6 py-4 rounded-lg bg-transparent text-white text-center whitespace-nowrap hover:bg-white/20 hover:border-white hover:text-white transition duration-300"
        >
          {text} →
        </button>
      ))}
    </div>
  );
};

export default GeneratedPrompts;
