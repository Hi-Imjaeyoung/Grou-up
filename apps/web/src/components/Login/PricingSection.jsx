import React from "react";
import "../../styles/Login/PricingSection.css"; // CSS 파일 import
import sun from "../../images/sun.png"; // 이미지 경로
import whatissun from "../../images/whatIsSun.png"; // 이미지 경로
import sunprice from "../../images/sunPrice.png"; // 이미지 경로

const PricingSection = () => {
    return (
        <section id="pricing" className="features-section">
            <div className="pricing-section">
                <h2 className="pricing-title">그로우업으로 매출 UP!</h2>
                <p className="pricing-subtitle">광고 분석부터 마진 계산까지 <br />합리적인 가격으로 매출 분석해보세요.</p>
                <div className="pricing-note">
                    <p className="note-title">14일 무료체험 </p>
                    <p className="note-title"> + </p>
                    <span className="note-title sun-text">
                        <img src={sun} alt="sun" className="sun-icon" />
                        첫 결제시
                    </span>
                    <p className="note-title">햇빛 2배 이벤트</p>
                    <p className="note-subtitle">(최대 20개)</p>
                </div>



            </div>
            <div className="image-container">
                <img src={whatissun} alt="whatissun" className="whatissun" />
                <img src={sunprice} alt="sunprice" className="sunprice" />
            </div>
            <div className="pricing-button">
                <a href="#free-trial" className="price-button">14일 무료체험 바로가기</a>
            </div>
        </section>
    );
};

export default PricingSection;
