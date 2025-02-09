import React, { FC } from 'react';
import { MessageCircle } from 'lucide-react';

type OldPrompt = {
  text: string;
  duration: string;
};

const OldPrompts: FC = () => {
  const oldPrompts: OldPrompt[] = [
    { text: 'Generating Code For An Application', duration: '5 days ago' },
    { text: 'Creating A Portfolio Website', duration: '2 weeks ago' },
    { text: 'Creating A Web Page From Scratch', duration: '1 month ago' },
  ];

  return (
    <div className="flex gap-10 my-20">
      {oldPrompts.map((item, index) => (
        <button
          key={index}
          className="border py-2 rounded-lg bg-gray-100 hover:opacity-80 w-[200px] h-30 text-left"
        >
          <MessageCircle size={24} className="pb-1" />
          {item.text}
          <p className="text-xs text-gray-500 pt-1">{item.duration}</p>
        </button>
      ))}
    </div>
  );
};

export default OldPrompts;
