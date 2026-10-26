import React from 'react';

const DashboardReturnCount = ({ value }) => {
    return (
        <div className="dashboard-card dashboard-return-count">
            <div className="card-label">반품수</div>
            <div className="card-value text-red">{value.toLocaleString()}</div>
        </div>
    );
};

export default DashboardReturnCount;