import React, { useState } from "react";
import SalesReport from "./SalesReport";
import "../../styles/Dashboard.css";
import TotalSalesReport from "./TotalSalesReport";
import TotalGraph from "./TotalGraph";
import MarginReport from "./MarginReport";

const DashboardGrid = () => {
    const [totalSalesData, setTotalSalesData] = useState([]);

    return (
        <div className="dashboard-grid">
            <div className="dashboard-item">
                <TotalSalesReport setTotalSalesData={setTotalSalesData} />
            </div>
            <div className="dashboard-item">
                <TotalGraph data={totalSalesData} />
            </div>
            <div className="dashboard-item">
                <SalesReport />
            </div>
            <div className="dashboard-item">
                <MarginReport />
            </div>
        </div>
    );
};

export default DashboardGrid;
