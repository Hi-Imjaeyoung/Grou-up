import React from "react";
import Dev from "../../images/Dev.gif"; // 개발자 이미지
import Seller from "../../images/Seller.gif"; // 셀러 이미지
import Together from "../../images/Together.png"; // 주먹 이미지
import "../../styles/Login/FeatureSectionMeetSellerOrDev.css"; // CSS 파일 import
const FeatureSectionMeetSellerOrDev = () => {
    return (
        <div className="meet-section">
            <h1 className="main-title">
                <span>그 래 서 !</span>
            </h1>
            <h2 className="sub-title">개발자와 셀러가 만났습니다</h2>
            <div className="images-container">
                <div className="dev-container">
                    <img src={Dev} alt="Developer" className="dev-image" />
                </div>
                <div className="seller-container">
                    <img src={Seller} alt="Seller" className="seller-image" />
                </div>
                <img src={Together} alt="Together" className="together-image" />
            </div>
        </div>
    );
};


export default FeatureSectionMeetSellerOrDev;
