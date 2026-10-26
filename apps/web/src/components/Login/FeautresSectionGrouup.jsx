import React from "react";
import awake from "../../images/awake.png"; // 이미지 경로
import logo from "../../images/grouup_logo.png"; // 이미지 경로
import "../../styles/Login/FeaturesSectionGrouup.css"; // CSS 파일 import

const FeaturesSectionGrouup = () => {
    return (
        <div className="grouup-features-section">
            <img src={logo} alt="Grouup Logo" className="grouup-logo" />
            <div className="grouup-main-text">
                <span className="grouup-highlight">그로우업</span>으로<br />
                <span className="grouup-sub-text">스마트하게 <span className="mobile-manage">관리하세요</span></span>
            </div>
            <img src={awake} alt="Awake" className="grouup-emoji" />
        </div>
    );
};

export default FeaturesSectionGrouup;
