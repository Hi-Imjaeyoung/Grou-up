
import React from "react";
// import Joyride from 'react-joyride'; // Joyride import 수정

// import { useProductTour } from "../../hooks/useProductTour";
import { useDashboardData } from "../../hooks/useDashboardData";

import DashboardCalendar from "./DashboardCalendar";
import DashboardSales from "./DashboardSales";
import DashboardActualSales from "./DashboardActualSales";
import DashboardReturnCount from "./DashboardReturnCount";
import DashboardMargin from "./DashboardMargin";
import DashboardAdCost from "./DashboardAdCost";
import DashboardRoas from "./DashboardRoas";
import DashboardSun from "./DashboardSun";
import DashboardCampaign from "./DashboardCampaign";
import DashboardSalesReport from "./DashboardSalesReport";
import DashboardMarginReport from "./DashboardMarginReport";
import { dashboardTourSteps } from "../constants/tourSteps";
import "../../styles/Dashboard/DashboardGridV2.css";

const DashboardGridV2 = () => {
    // 1. '데이터 총괄 브레인'을 호출해서 데이터 관련 모든 것을 받아옵니다.
    const {
        isLoading,
        startDate,
        campaigns,
        tableData,
        latestDate,
        totalSales,
        totalActualSales,
        totalReturnCount,
        margin,
        totalAdCost,
        roas,
        sun,
        handleDateChange
    } = useDashboardData();

    // '투어 전문가'에게 대본과 고유 키를 전달하며 호출
    // const { runTour, steps, stepIndex, handleJoyrideCallback } = useProductTour({
    //     steps: dashboardTourSteps,
    //     isLoading: isLoading,
    //     tourKey: 'hasCompletedDashboardTour' // 대시보드 투어 전용 키
    // });

    // 데이터 로딩 중에는 로딩 화면을 보여줍니다.
    if (isLoading) {
        return <div className="dashboard-loading">데이터를 불러오는 중입니다...</div>;
    }

    // 3. 받아온 정보들로 화면을 그리기만 합니다.
    return (
        <div className="dashboard-container">
            {/* <Joyride
                run={runTour}
                steps={steps}
                stepIndex={stepIndex}
                callback={handleJoyrideCallback}
                continuous
                showProgress
                showSkipButton
                disableScrollParentFix
                styles={{
                    options: { zIndex: 10000, primaryColor: '#5E87E3' },
                    tooltip: { borderRadius: '12px', fontSize: '15px' },
                    buttonNext: { backgroundColor: '#5E87E3', borderRadius: '20px' }
                }}
                locale={{ back: '이전', close: '닫기', last: '완료', next: '다음', skip: '건너뛰기' }}
            /> */}

            <div className="tour-step-calendar">
                {startDate && (<DashboardCalendar initialDate={startDate} onDateChange={handleDateChange} />)}
            </div>
            <div className="tour-step-sales"> <DashboardSales value={totalSales} /> </div>
            <div className="tour-step-actual-sales"> <DashboardActualSales value={totalActualSales} /> </div>
            <div className="tour-step-return-count"> <DashboardReturnCount value={totalReturnCount} /> </div>
            <div className="tour-step-margin"> <DashboardMargin value={margin} /> </div>
            <div className="tour-step-ad-cost"> <DashboardAdCost value={totalAdCost} /> </div>
            <div className="tour-step-roas"> <DashboardRoas value={roas} /> </div>
            <div className="tour-step-sun"> <DashboardSun value={sun} /> </div>
            <div className="tour-step-campaign"> <DashboardCampaign selectedDate={latestDate} /> </div>
            <div className="tour-step-sales-report"> <DashboardSalesReport tableData={tableData} /> </div>
            <div className="tour-step-margin-report"> <DashboardMarginReport selectedDate={latestDate} /> </div>
        </div>
    );
};

export default DashboardGridV2;