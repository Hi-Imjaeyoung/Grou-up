import React from "react";
import breakdown from "../../images/breakdown.png"
import "../../styles/Login/FeaturesStartSection.css"
const FeaturesStartSection = () => {
    return (
        <section id="features" className="features-section">
            <h2 className="features-title">
                쿠팡 <span className="features-highlight">광고 마진 정산</span>
            </h2>
            <h2 className="features-title features-question">어떻게 관리하시나요?</h2> {/* 추가된 클래스 */}
            <div className="features-dots">
                <span className="features-dot">.</span>
                <span className="features-dot">.</span>
                <span className="features-dot">.</span>
            </div>
            <div className="mb-6">
                <img src={breakdown} alt="이모티콘" className="features-image" />
            </div>

            <h3 className="features-subtitle">보기 어려운</h3>
            <h3 className="features-subtitle1">광고보고서</h3>
            <h3 className="features-subtitle2">마진 계산</h3>
            <h3 className="features-subtitle3">정산 시스템</h3>
        </section>
    );
};

export default FeaturesStartSection;