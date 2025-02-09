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
    <div className="grid grid-cols-3 gap-6 my-16">
      {oldPrompts.map((item, index) => (
        <div
          key={index}
          className="border px-4 py-5 rounded-lg shadow-md bg-gray-100 hover:shadow-lg transition w-[240px] text-center"
        >
          <MessageCircle size={24} className="mb-2 text-gray-600 mx-auto" />
          <p className="text-gray-800 font-semibold">{item.text}</p>
          <p className="text-xs text-gray-500 mt-1">{item.duration}</p>
        </div>
      ))}
    </div>
  );
};

export default OldPrompts;
