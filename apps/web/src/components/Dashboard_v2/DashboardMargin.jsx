import React from 'react';

const DashboardMargin = ({ value }) => {
    return (
        <div className="dashboard-card dashboard-margin">
            <div className="card-label">마진</div>
            <div className="card-value">
                {((value / 10000).toFixed(1)).toLocaleString()} 만원
            </div>
        </div>
    );
};

export default DashboardMargin;