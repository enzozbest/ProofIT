import { useNavigate } from 'react-router-dom';
import { BotMessageSquare } from 'lucide-react';

export default function Logo() {
  const navigate = useNavigate();

  return (
    <div
      className="flex items-center gap-2 text-xl cursor-pointer"
      onClick={() => navigate('/')}
    >
      <BotMessageSquare className="w-6 h-6" />
      <span className="font-normal">
        PROOF -<span className="font-bold"> IT!</span>
      </span>
    </div>
  );
}
