import React from 'react';

interface TypographyProps {
  children: React.ReactNode;
}
export function TypographySmall({ children }: TypographyProps) {
  return <small className="text-sm font-medium leading-none">{children}</small>;
}
