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
    {
      text: 'Creating A Web Page From Scratch That Is Fully Responsive and Optimized for SEO',
      duration: '1 month ago',
    },
  ];

  return (
    <div className="flex gap-4 my-10 justify-center w-full max-w-5xl overflow-x-auto">
      {oldPrompts.map((item, index) => (
        <button
          key={index}
          className="border-2 border-white px-6 py-4 rounded-lg bg-transparent  text-left whitespace-normal hover:bg-white/20 hover:border-white hover: transition duration-300 w-[220px] flex flex-col items-start"
        >
          <MessageCircle size={24} className=" mb-2" />
          <span className="font-medium line-clamp-3 overflow-hidden text-ellipsis">
            {item.text}
          </span>
          <p className="text-xs text-gray-300 pt-1">{item.duration}</p>
        </button>
      ))}
    </div>
  );
};

export default OldPrompts;
