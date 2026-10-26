// src/SunShineLog.js (또는 App.js 등 최상위 컴포넌트)
import React from "react";
import SunshineChargeHistory from './SunshineChargeHistory'; // 경로 확인!
import SunshineUsageHistory from './SunshineUsageHistory';   // 경로 확인!
import './SunShineLog.css'; // SunShineLog 전용 CSS

const SunShineLog = () => {
    return (
        <div className="sunshine-log-container">
            {/* 다른 섹션들이 있다면 여기에 추가 */}
            {/* <div className="other-sections"> ... </div> */}

            <div className="history-sections-wrapper">
                <div className="charge-history-wrapper">
                    <SunshineChargeHistory />
                </div>
                <div className="usage-history-wrapper">
                    <SunshineUsageHistory />
                </div>
            </div>
        </div>
    );
};

export default SunShineLog;