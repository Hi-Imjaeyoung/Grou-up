import React, { useState, useEffect } from "react";
import TabNavigation from "./TabNavigation";
import { getMyCampaigns } from "../../services/campaign";
import MarginCalculatorForm from "../Margin_v2/MarginCalculatorForm";
import MarginCalculatorResult from "../Margin_v2/MarginCalculatorResult";
import DateRangeCalendar from "../Date/DateRangeCalendar";
import '../../styles/DateRangeSelectCalendar.css';

const MarginTabNavigation = () => {
    const [activeComponent, setActiveComponent] = useState("MarginCalculatorForm");
    const [campaigns, setCampaigns] = useState([]);

    // 컴포넌트 상단
    const today = new Date();

    // 1일 오전 12시가 아니라, 예시대로 12시에 맞추고 싶다면
    const firstDayDate = new Date(today.getFullYear(), today.getMonth(), 1);
    firstDayDate.setHours(12, 0, 0, 0);

    // 마지막 일자를 “오늘”로 (23:59:59.999까지)
    const lastDayDate = new Date(today);
    lastDayDate.setHours(23, 59, 59, 999);

    // ISO 문자열(YYYY-MM-DD)로 변환
    const firstDay = firstDayDate.toISOString().slice(0, 10);
    const lastDay = lastDayDate.toISOString().slice(0, 10);

    // state 초기화 (문자열)
    const [startDate, setStartDate] = useState(firstDay);
    const [endDate, setEndDate] = useState(lastDay);
    const [showDatePicker, setShowDatePicker] = useState(false); // 달력 표시 상태
    const handleComponentChange = (component) => {
        setActiveComponent(component);
    };

    useEffect(() => {
        const fetchCampaigns = async () => {
            try {
                const response = await getMyCampaigns();
                setCampaigns(response.data || []);
            } catch (error) {
                console.error("캠페인 데이터를 가져오는 중 오류 발생:", error);
            }
        };
        fetchCampaigns();
    }, []);

    // 달력에서 날짜 범위 선택 시 호출되는 함수
    const handleDateRangeChange = (dateRange) => {
        const start = new Date(dateRange.startDate);
        start.setHours(12, 0, 0, 0); // 시작일: 정오 (12:00:00.000)

        const end = new Date(dateRange.endDate);
        end.setHours(23, 59, 59, 999); // 종료일: 자정 직전 (23:59:59.999)

        console.log(start, end)
        setStartDate(start.toISOString().slice(0, 10));
        setEndDate(end.toISOString().slice(0, 10));
    };
    // 날짜 선택 버튼 클릭 시 달력 토글
    const toggleDatePicker = () => {
        setShowDatePicker(!showDatePicker);
    };

    // 캠페인 순서 업데이트 핸들러
    const handleCampaignOrderChange = (newCampaigns) => {
        setCampaigns(newCampaigns);
    };

    return (
        <div className="main-content">
            <div className="min-h-screen bg-gray-100">
                <div className="mt-8">
                    <main className="container mx-auto p-6">
                        <div className="flex items-center justify-between">
                            <TabNavigation onComponentChange={handleComponentChange} />
                            {activeComponent === "MarginCalculatorResult" && (
                                <div className="date-selection-container">
                                    <button
                                        onClick={toggleDatePicker}
                                        className="date-selection-button"
                                    >
                                        {startDate} ~ {endDate}
                                        <span className="dropdown-arrow">▼</span>
                                    </button>

                                    {showDatePicker && (
                                        <div className="date-picker-modal">
                                            <DateRangeCalendar
                                                onDateRangeChange={handleDateRangeChange}
                                                initialStartDate={startDate}
                                                initialEndDate={endDate}
                                                onClose={toggleDatePicker}
                                            />
                                        </div>
                                    )}
                                </div>
                            )}
                        </div>

                        <div className="mt-8">
                            {activeComponent === "MarginCalculatorForm" && (
                                <MarginCalculatorForm
                                    campaigns={campaigns}
                                    onCampaignOrderChange={handleCampaignOrderChange}
                                />
                            )}
                            {activeComponent === "MarginCalculatorResult" && (
                                <MarginCalculatorResult
                                    campaigns={campaigns}
                                    startDate={startDate}
                                    endDate={endDate}
                                    isActive={activeComponent === "MarginCalculatorResult"}
                                />
                            )}
                        </div>
                    </main>
                </div>
            </div>
        </div>
    );
};

export default MarginTabNavigation;