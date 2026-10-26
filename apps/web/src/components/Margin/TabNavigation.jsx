import React, { useState } from "react";
import "../../styles/MarginTabNavigation.css"; // 스타일 파일 추가

const TabNavigation = ({ onComponentChange }) => {
    const [activeTab, setActiveTab] = useState("MarginCalculatorForm"); // 기본 활성화 탭 설정

    const handleTabClick = (tab) => {
        setActiveTab(tab); // 클릭된 탭을 활성화
        onComponentChange(tab); // 상위 컴포넌트에 탭 변경 알림
    };

    return (
        <div className="margin-tab-container">
            <div className="margin-tabs-and-dates">
                {/* 탭 영역 */}
                <div className="margin-tabs">
                    {/* 첫 번째 버튼: 마진 계산식 입력 */}
                    <button
                        className={`margin-tab left ${activeTab === "MarginCalculatorForm" ? "active" : ""}`}
                        onClick={() => handleTabClick("MarginCalculatorForm")}
                    >
                        마진 계산식 입력
                    </button>

                    {/* 두 번째 버튼: 마진 결과 보기 */}
                    <button
                        className={`margin-tab right ${activeTab === "MarginCalculatorResult" ? "active" : ""}`}
                        onClick={() => handleTabClick("MarginCalculatorResult")}
                    >
                        마진 결과 보기
                    </button>
                </div>
            </div>
        </div>
    );
};

export default TabNavigation;
