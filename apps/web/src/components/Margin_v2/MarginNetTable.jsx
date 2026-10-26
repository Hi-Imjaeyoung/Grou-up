import React, { useEffect, useState, useCallback, useRef } from "react";
import { getNetProfitAndReturnCost } from "../../services/margin";
import "../../styles/margin/MarginNetTable.css";
import { formatNumber, formatInteger } from "../../utils/formatUtils";
import "../../styles/numberColor.css";

const MarginNetTable = ({ startDate, endDate }) => {
    const [dailyData, setDailyData] = useState([]);
    const [fullDateRange, setFullDateRange] = useState([]);
    const scrollContainerRef = useRef(null);

    useEffect(() => {
        const scrollContainer = scrollContainerRef.current;
        if (!scrollContainer) return;

        const handleWheel = (e) => {
            e.preventDefault();
            scrollContainer.scrollLeft += e.deltaY;
        };

        scrollContainer.addEventListener('wheel', handleWheel, { passive: false });

        return () => {
            scrollContainer.removeEventListener('wheel', handleWheel);
        };
    }, []);

    const generateDateRange = useCallback(() => {
        if (!startDate || !endDate) return;
        const start = new Date(startDate + 'T00:00:00');
        const end = new Date(endDate + 'T00:00:00');
        const dates = [];
        for (let d = new Date(start); d <= end; d.setDate(d.getDate() + 1)) {
            const month = d.getMonth() + 1;
            const day = d.getDate();
            dates.push(`${month}/${day}`);
        }
        setFullDateRange(dates);
    }, [startDate, endDate]);

    useEffect(() => {
        const fetchData = async () => {
            if (!startDate || !endDate) return;
            try {
                const response = await getNetProfitAndReturnCost({ startDate, endDate });
                setDailyData(Array.isArray(response.data) ? response.data : []);
            } catch (error) {
                console.error("데이터를 가져오는 중 오류 발생:", error);
                setDailyData([]);
            }
        };
        fetchData();
        generateDateRange();
    }, [startDate, endDate, generateDateRange]);

    const convertToAPIDateFormat = (dateMD) => {
        const [month, day] = dateMD.split('/').map(num => num.padStart(2, '0'));
        const year = new Date(startDate).getFullYear();
        return `${year}-${month}-${day}`;
    };

    const getDataForDate = (date, field) => {
        const apiDate = convertToAPIDateFormat(date);
        const found = dailyData.find(item => item.marDate === apiDate);
        return found ? (found[field] || 0) : 0;
    };

    const totalSales = dailyData.reduce((sum, item) => sum + (item.marSales || 0), 0);
    const totalNetProfit = dailyData.reduce((sum, item) => sum + (item.margin || 0), 0);

    return (
        <div className="net-profit-table-wrapper" ref={scrollContainerRef}>
            <table>
                <thead>
                    <tr>
                        <th className="header-tabs">
                            <button className="tab-button active">전체 마진</button>
                        </th>
                        <th className="header-total">
                            <div className="header-content">합계</div>
                        </th>
                        {fullDateRange.map((date) => (
                            <th key={date}>
                                <div className="header-content">{date}</div>
                            </th>
                        ))}
                    </tr>
                </thead>
                <tbody>
                    <tr>
                        <td className="row-header">매출</td>
                        <td className="total-cell">
                            <div className="cell-content">
                                <span className="cell-value">{formatInteger(totalSales)}</span>
                            </div>
                        </td>
                        {fullDateRange.map((date) => (
                            <td key={`sales-${date}`}>
                                <div className="cell-content">
                                    <span className="cell-value">{formatInteger(getDataForDate(date, 'marSales'))}</span>
                                </div>
                            </td>
                        ))}
                    </tr>
                    <tr>
                        <td className="row-header">순수익</td>
                        <td className="total-cell">
                            <div className="cell-content">
                                <span className="cell-value positive-profit">{formatInteger(totalNetProfit)}</span>
                            </div>
                        </td>
                        {fullDateRange.map((date) => {
                            const netProfit = getDataForDate(date, 'margin');
                            return (
                                <td key={`profit-${date}`}>
                                    <div className="cell-content">
                                        <span className={`cell-value ${netProfit >= 0 ? 'positive-profit' : 'negative-profit'}`}>
                                            {formatInteger(netProfit)}
                                        </span>
                                    </div>
                                </td>
                            );
                        })}
                    </tr>
                </tbody>
            </table>
        </div>
    );
};

export default MarginNetTable;