import { MessageCircle } from 'lucide-react'

const OldPrompts = () => {

    const oldPrompts = [
        { text: "Generating Code For An Application", duration: "5 days ago" },
        { text: "Creating A Portfolio Website", duration: "2 weeks ago" },
        { text: "Creating A Web Page From Scratch", duration: "1 month ago" }
      ];

    return (
        <div className="flex gap-10 my-40">
          {oldPrompts.map((item, index) => (
            <button key={index} className="border px-4 py-2 rounded-lg hover:opacity-80 w-[150px] h-30 text-left">
                <MessageCircle size={24} className="pb-1" /> 
                {item.text}
                <p className="text-xs text-gray-500 pt-1">{item.duration}</p>
            </button>
          ))}
        </div>
    );
};

export default OldPrompts;