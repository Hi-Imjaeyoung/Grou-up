import React from 'react';

const DashboardSales = ({ value }) => {
    return (
        <div className="dashboard-card dashboard-sales">
            <div className="card-label">광고매출</div>
            <div className="card-value">
                {((value / 10000).toFixed(1)).toLocaleString()} 만원
            </div>
        </div>
    );
};

export default DashboardSales;
