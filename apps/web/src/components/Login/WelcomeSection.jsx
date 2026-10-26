import React from "react";

const WelcomeSection = () => {
    return (
        <div className="flex flex-col items-center justify-center bg-gradient-to-br from-purple-700 to-blue-600 text-white p-12">
            <h1 className="text-5xl font-bold mb-6">환영합니다</h1>
            <p className="text-lg mb-8 opacity-90">
                우리의 서비스는 혁신적이고 신뢰할 수 있습니다.
                <br />
                지금 바로 경험해보세요!
            </p>
        </div>
    );
};

export default WelcomeSection;
