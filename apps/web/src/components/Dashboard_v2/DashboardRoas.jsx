import React from 'react';

const DashboardRoas = ({ value }) => {
    return (
        <div className="dashboard-card dashboard-roas">
            <div className="card-label">로아스</div>
            <div className="card-value">
                {value.toLocaleString('ko-KR', { maximumFractionDigits: 0 })}%
            </div>
        </div>
    );
};

export default DashboardRoas;