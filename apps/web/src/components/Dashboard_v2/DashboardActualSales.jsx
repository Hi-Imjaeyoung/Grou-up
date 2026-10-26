import React from 'react';

const DashboardActualSales = ({ value }) => {
    return (
        <div className="dashboard-card dashboard-actual-sales">
            <div className="card-label">판매수</div>
            <div className="card-value">{value.toLocaleString()}</div>
        </div>
    );
};

export default DashboardActualSales;