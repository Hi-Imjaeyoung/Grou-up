import React from 'react';

const Button = ({ children, variant = "primary", ...props }) => {
  const getButtonStyle = () => {
    switch (variant) {
      case "kakao":
        return "text-black bg-[#FEE500] hover:bg-yellow-300 w-full py-3 px-4 rounded-lg font-bold";
      case "google":
        return "text-black bg-[#82a4c9] w-full py-3 px-4 rounded-lg font-bold";
      default:
        return "text-white bg-[#5c62b8]  w-full py-3 px-4 rounded-lg";
    }
  };

  return (
    <button {...props} className={getButtonStyle()}>
      {children}
    </button>
  );
};
export default Button;