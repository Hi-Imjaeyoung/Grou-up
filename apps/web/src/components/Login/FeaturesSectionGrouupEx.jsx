import React from "react";
import marginFileUpload from "../../images/marginFileUpload.png";
import mainDashboard from "../../images/mainDashboard.png"
import totalKeyword from "../../images/totalKeyword.png"
import excludeKeyword from "../../images/excludeKeyword.png"
import marginResult from "../../images/marginResult.png"
import marginSetting from "../../images/marginSetting.png"
import "../../styles/Login/FeaturesSectionGrouupEx.css";

const FeaturesSectionGrouupEx = () => {
    return (
        <div className="grouup-ex-section">
            <img src={marginFileUpload} alt="File Upload" className="margin-upload-image" />
            <div className="ex-upload-text">
                <span className="ex-highlight">엑셀파일<span className="ex-white">만</span></span><br />
                <span className="ex-sub-text">업로드하면 자동 분석!</span>
            </div>
            <img src={mainDashboard} alt="Main Dashboard" className="dashboard-image" />
            <div className="ex-dashboard-text">
                <span className="ex-dash-highligt">한 눈에 파악 가능한</span><br />
                <span className="ex-dashsub-highligt">대시보드<span className="ex-white"> 부터</span></span><br />
            </div>
            <img src={totalKeyword} alt="Total Keyword Analysis" className="keyword-image" />
            <img src={excludeKeyword} alt="Exclude Keyword Analysis" className="keyword-image" />
            <div className="ex-keyword-text">
                <span className="ex-keyword-highlight">키워드 분석</span>
            </div>
            <img src={marginResult} alt="Margin Result" className="margin-image" />
            <img src={marginSetting} alt="Margin Setting" className="margin-image" />
            <div className="ex-margin-text">
                <span className="ex-margin-highlight"><span className="ex-white">자동</span> 계산해주는</span><br />
                <span className="ex-margin-subtext-highlight">마진 계산기<span className="ex-white"></span></span>
            </div>
        </div>
    );
};

export default FeaturesSectionGrouupEx;
