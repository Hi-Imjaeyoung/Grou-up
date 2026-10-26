// Totalsearchbar.js (리팩토링 버전)

import React, { useState } from "react";
import { useParams } from "react-router-dom";
import CampaignOptionDetailsComponent from "./CampaignOptionDetailsComponent";
import KeytotalComponent from "./KeyTotalComponent";
import StatisticGrid from "./CampaignStatistics/StatisticGrid";
import DateRangeCalendar from "./Date/DateRangeCalendar";
import "react-datepicker/dist/react-datepicker.css";
import "../styles/TabComponent.css";
import "../styles/DateRangeSelectCalendar.css";

// 탭에 따라 보여줄 컴포넌트를 미리 매핑해두자.
const tabComponents = {
    stats: StatisticGrid,
    campaign: CampaignOptionDetailsComponent,
    keywords: KeytotalComponent,
};

// 탭의 영문 이름을 한글로 바꿔줄 객체
const tabLabels = {
    stats: "통계",
    campaign: "옵션",
    keywords: "키워드",
};

const Totalsearchbar = ({ title }) => {
    const { campaignId } = useParams();
    const tabs = ["stats", "campaign", "keywords"];
    const [activeTab, setActiveTab] = useState("stats");

    // --- 날짜 관련 로직 (기존과 동일) ---
    const today = new Date();
    const firstDayDate = new Date(today.getFullYear(), today.getMonth(), 1);
    const lastDayDate = new Date(today);

    const formatDate = (date) => date.toISOString().slice(0, 10);

    const [startDate, setStartDate] = useState(formatDate(firstDayDate));
    const [endDate, setEndDate] = useState(formatDate(lastDayDate));

    const [showCalendar, setShowCalendar] = useState(false);
    const toggleCalendar = () => setShowCalendar((prev) => !prev);

    const handleTabChange = (tab) => setActiveTab(tab);

    const handleDateRangeChange = ({ startDate, endDate }) => {
        setStartDate(startDate);
        setEndDate(endDate);
    };

    // 렌더링할 현재 활성화된 탭의 컴포넌트를 가져온다.
    const ActiveComponent = tabComponents[activeTab];

    return (
        <div className="tab-container">
            <div className="title-and-datePicker">
                <h2 className="title">
                    <span className="analysis-title-prefix">광고 캠패인 분석  </span>
                    <span className="analysis-title-main">{title}</span>
                </h2>
                <div className="date-selection-container">
                    <button className="date-selection-button" onClick={toggleCalendar}>
                        {startDate} ~ {endDate} <span className="dropdown-arrow">▼</span>
                    </button>
                    {showCalendar && (
                        <>
                            <div className="date-picker-overlay" onClick={toggleCalendar}></div>
                            <div className="date-picker-modal">
                                <DateRangeCalendar
                                    initialStartDate={startDate}
                                    initialEndDate={endDate}
                                    onDateRangeChange={handleDateRangeChange}
                                    onClose={toggleCalendar}
                                />
                            </div>
                        </>
                    )}
                </div>
            </div>

            <div className="tabs-container">
                <div className="tabs">
                    {tabs.map((tab) => (
                        <button
                            key={tab}
                            // 클래스 이름을 만드는 로직을 훨씬 간결하게!
                            className={`tab ${activeTab === tab ? "active" : ""}`}
                            onClick={() => handleTabChange(tab)}
                        >
                            {tabLabels[tab]} {/* 객체에서 라벨을 가져온다 */}
                        </button>
                    ))}
                </div>
            </div>

            <div className={`tab-content ${activeTab}-active`}>
                {/* 컴포넌트를 여기서 한 번에 렌더링! */}
                <ActiveComponent
                    campaignId={campaignId}
                    startDate={startDate}
                    endDate={endDate}
                />
            </div>
        </div>
    );
};

export default Totalsearchbar;