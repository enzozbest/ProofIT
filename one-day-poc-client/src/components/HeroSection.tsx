import React, { FC } from 'react';
import { BotMessageSquare } from 'lucide-react';

const HeroSection: FC = () => {
  return (
    <div className="flex flex-col items-center text-center my-10">
      <BotMessageSquare size={64} />
      <h1 className="text-3xl font-bold mt-5">Let's get building!</h1>
    </div>
  );
};

export default HeroSection;
