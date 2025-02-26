import React, { FC } from 'react';

const HeroSection: FC = () => {
  return (
    <div className="flex flex-col items-center justify-center text-center mb-5 px-6">
      <h1 className="text-white text-5xl md:text-6xl font-light">
        Enabling you from
      </h1>
      <h2 className="text-white text-6xl md:text-7xl font-bold text-[#f7a6ff]">
        day one
      </h2>
    </div>
  );
};

export default HeroSection;
