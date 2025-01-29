import React from "react";

const InputBox = () => {
  return (
    <div className="flex items-center border rounded-lg p-2 w-full max-w-lg">
      <input
        type="text"
        placeholder="Make me a robot! or a time machine or ..!"
        className="flex-1 bg-transparent px-2 py-1 outline-none"
      />
      <button className="p-2 bg-white items-center justify-center w-10 h-10">
        📎
      </button>
      <button className="p-2 bg-white text-black items-center justify-center w-10 h-10 ">
        ➜
      </button>

    </div>
  );
};

export default InputBox;
