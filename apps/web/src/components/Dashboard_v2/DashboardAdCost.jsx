import React from 'react';

const DashboardAdCost = ({ value }) => {
    return (
        <div className="dashboard-card dashboard-ad-cost">
            <div className="card-label">광고비</div>
            <div className="card-value">{((value / 10000).toFixed(1)).toLocaleString()} 만원</div>
        </div>
    );
};

export default DashboardAdCost;