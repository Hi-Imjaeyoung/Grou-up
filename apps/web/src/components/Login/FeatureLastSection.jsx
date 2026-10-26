import React from "react";
import logo from "../../images/grouup_logo.png"; // 로고 이미지 경로
import "../../styles/Login/FeatureLastSection.css"; // CSS 파일 import

const FeatureLastSection = () => {
    return (
        <div className="header-section">
            <h1 className="header-text">
                <span className="blue-highlight">그로우업</span>으로 <span className="red-highlight">한번에</span><br className="mobile-break" />
                <span className="sub-text">해결하세요</span>
            </h1>
            <img src={logo} alt="Grouup Logo" className="Lastlogo" />
        </div>
    );
};

export default FeatureLastSection;
