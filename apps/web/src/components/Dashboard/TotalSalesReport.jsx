import React, { useEffect, useState } from "react";
import { getTotalSales } from "../../services/margin";
import usePaginationAndSorting from "../../hooks/usePaginationAndSorting";
import "../../styles/SalesReport.css";
import DateControls from "../Date/DateControls";
import { formatNumber } from "../../utils/formatUtils";

const TotalSalesReport = ({ setTotalSalesData }) => {
    const [date, setDate] = useState(() => {
        const yesterday = new Date();
        yesterday.setDate(yesterday.getDate() - 1);
        return yesterday; // 기본값: 어제 날짜
    });

    const [data, setData] = useState([]);
    const { paginatedData, changeSort, sortConfig } =
        usePaginationAndSorting({ data, itemsPerPage: 7 });

    // API 데이터 호출
    useEffect(() => {
        const fetchData = async () => {
            try {
                const response = await getTotalSales({ date: date.toISOString().split("T")[0] });
                const fetchedData = response.data || [];
                const sortedData = fetchedData.sort((a, b) => new Date(a.date) - new Date(b.date));
                const processedData = fetchedData.map((item) => ({
                    date: item.marDate,
                    totalSales: item.marSales,
                    totalAdCost: item.marAdCost,
                    roas: Math.round((item.marSales / item.marAdCost) * 100) || 0,
                }));
                setData(sortedData);
                setTotalSalesData(processedData); // 부모 컴포넌트로 데이터 전달
            } catch (error) {
                console.error("Error fetching data:", error);
            }
        };
        fetchData();
    }, [date, setTotalSalesData]);

    // 날짜를 하루 전으로 이동
    const handlePrevDay = () => {
        const newDate = new Date(date);
        newDate.setDate(newDate.getDate() - 1);
        setDate(newDate);
    };

    // 날짜를 하루 후로 이동
    const handleNextDay = () => {
        const newDate = new Date(date);
        newDate.setDate(newDate.getDate() + 1);
        setDate(newDate);
    };

    return (
        <div className="sales-report">
            <div className="report-header">
                <h3 className="report-title">종합 보고서</h3>
                <DateControls
                    date={date}
                    onPrevDay={handlePrevDay}
                    onNextDay={handleNextDay}
                    onDateChange={(date) => setDate(date)}
                />
            </div>
            <table className="sales-table">
                <thead>
                    <tr>
                        <th onClick={() => changeSort("marDate")}>
                            날짜 {sortConfig?.key === "marDate" && (sortConfig.direction === "asc" ? "↑" : "↓")}
                        </th>
                        <th onClick={() => changeSort("marAdCost")}>
                            총 광고비 {sortConfig?.key === "marAdCost" && (sortConfig.direction === "asc" ? "↑" : "↓")}
                        </th>
                        <th onClick={() => changeSort("marSales")}>
                            총 매출 {sortConfig?.key === "marSales" && (sortConfig.direction === "asc" ? "↑" : "↓")}
                        </th>
                        <th onClick={() => changeSort("marRoas")}>
                            ROAS {sortConfig?.key === "marRoas" && (sortConfig.direction === "asc" ? "↑" : "↓")}
                        </th>
                    </tr>
                </thead>
                <tbody>
                    {paginatedData.length > 0 ? (
                        paginatedData.map((item, index) => (
                            <tr key={index}>
                                <td>{item.marDate}</td>
                                <td>{formatNumber(item.marAdCost)}</td>
                                <td>{formatNumber(item.marSales)}</td>
                                <td>{item.marRoas}%</td>
                            </tr>
                        ))
                    ) : (
                        <tr>
                            <td colSpan="4" >
                                <div className="empty-message">
                                    광고 보고서를 업로드 해주세요
                                </div>
                            </td>
                        </tr>
                    )}
                </tbody>
            </table>
        </div>
    );
};

export default TotalSalesReport;
