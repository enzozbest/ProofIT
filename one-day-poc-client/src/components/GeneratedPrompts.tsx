import React, { FC } from 'react';

type GeneratedPromptsProps = {
  prompts: string[];
};

const GeneratedPrompts: FC<GeneratedPromptsProps> = ({ prompts }) => {
  return (
    <div className="mt-5 flex flex-nowrap gap-4 w-full max-w-5xl overflow-x-auto justify-center pb-9">
      {prompts.map((text, index) => (
        <div
          key={index}
          className="border px-6 py-4 rounded-lg bg-gray-100 text-center whitespace-nowrap hover:opacity-80"
        >
          {text} →
        </div>
      ))}
    </div>
  );
};

export default GeneratedPrompts;
