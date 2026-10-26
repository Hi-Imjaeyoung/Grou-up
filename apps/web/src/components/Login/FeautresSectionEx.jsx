import React from "react";
import marginReportGIF from "../../images/marginReportGIF.gif";
import salesReportGIF from "../../images/salesReportGIF.gif";
import "../../styles/Login/FeaturesSectionEx.css"; // CSS 파일 import

const FeaturesSectionEx = () => {
    return (
        <div className="min-h-custom bg-gradient-to-br from-purple-500 to-blue-400 flex items-center justify-center">
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4 p-8">
                <div className="relative col-span-2 flex justify-center md:col-span-2"> {/* GIF 카드가 두 칸 차지 */}
                    <img src={salesReportGIF} alt="Sales Report" className="sales-card" />
                </div>
                <div className="relative hidden md:block"> {/* 비어 있는 공간 (데스크톱에서만 보이도록 설정) */}
                </div>
                <div className="relative col-span-1 flex items-center justify-center"> {/* 텍스트가 왼쪽에 오도록 설정 */}
                    <p className="data-question">
                        <span className="highlight">이렇게 많은 <span className="red-text">데이터</span></span><br />
                        <span className="highlight"> <span className="yellow-text">어떻게</span> 관리하고</span><br />
                        <span className="highlight">계시나요?</span>
                    </p>
                </div>
                <div className="relative col-span-2 flex justify-center md:col-span-2"> {/* GIF 카드가 두 칸 차지 */}
                    <img src={marginReportGIF} alt="Margin Report" className="margins-card" />
                </div>
            </div>
        </div>
    );
};

export default FeaturesSectionEx;
