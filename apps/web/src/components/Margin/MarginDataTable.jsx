import React, { useState, useEffect, useRef } from 'react';
import "../../styles/margin/MarginDataTable.css";
import { getMarginByCampaignId, createMarginTable } from "../../services/margin";
import { formatNumber } from "../../utils/formatUtils";

const MarginDataTable = ({ startDate, endDate, campaignId, onDataChange }) => {
    const [data, setData] = useState([]);
    const [modifiedData, setModifiedData] = useState({});
    const tableContainerRef = useRef(null);
    const todayRef = useRef(null);
    const startDateRef = useRef(null);
    const [isInitialLoading, setIsInitialLoading] = useState(true);


    // 날짜 범위 생성
    const dateRange = [];
    const currentDate = new Date(startDate);
    const end = new Date(endDate);

    while (currentDate <= end) {
        const yearMonthDay = currentDate.toISOString().split('T')[0];
        const month = String(currentDate.getMonth() + 1).padStart(2, '0');
        const day = String(currentDate.getDate()).padStart(2, '0');
        dateRange.push({ fullDate: yearMonthDay, displayDate: `${month}-${day}` });
        currentDate.setDate(currentDate.getDate() + 1);
    }

    // 데이터 가져오기
    useEffect(() => {
        const fetchData = async () => {
            try {
                const response = await getMarginByCampaignId({ startDate, endDate, campaignId });
                setData(response.data[0].data);
            } catch (error) {
                console.error("데이터를 가져오는 중 오류 발생:", error);
            }
        };
        fetchData();
    }, [startDate, endDate, campaignId]);


    // 오늘 날짜로 스크롤 또는 시작일로 스크롤
    useEffect(() => {
        if (!data.length || !dateRange.length || !isInitialLoading) return;

        // 데이터가 있는 마지막 날짜 구하기
        const validDates = data
            .map(item => item.marDate)
            .filter(Boolean)
            .sort();

        const lastDataDate = validDates[validDates.length - 1];
        const isLastDateInRange = dateRange.some(d => d.fullDate === lastDataDate);

        if (tableContainerRef.current) {
            const container = tableContainerRef.current;

            const lastRef = document.querySelector(`[data-date="${lastDataDate}"]`);
            if (isLastDateInRange && lastRef) {
                const scrollLeft = lastRef.offsetLeft
                    - container.clientWidth
                    + lastRef.offsetWidth
                    + 100;

                container.scrollTo({
                    left: scrollLeft,
                    behavior: 'smooth',
                });
            } else if (startDateRef.current) {
                container.scrollTo({
                    left: 0,
                    behavior: 'smooth',
                });
            }
        }
        setIsInitialLoading(false);
    }, [data, dateRange, isInitialLoading]);

    const options = [
        { optionName: "목표효율", key: "marTargetEfficiency", editable: true },
        { optionName: "광고예산", key: "marAdBudget", editable: true },
        { optionName: "광고수익률", key: "marAdRevenue" },
        { optionName: "집행광고비 * 1.1", key: "marAdCost" },
        { optionName: "CPC 단가", key: "marCpcUnitCost" },
        { optionName: "노출수", key: "marImpressions" },
        { optionName: "클릭률", key: "marClicks" },
        { optionName: "전환률", key: "marAdConversionSalesCount" },
        { optionName: "광고 전환 판매 수", key: "marAdConversionSales" },
        { optionName: "실제 판매 수", key: "marActualSales" },
        { optionName: "반품 개수", key: "marReturnCount" },
        { optionName: "반품 비용", key: "marReturnCost" },
        { optionName: "광고 마진", key: "marAdMargin" },
        { optionName: "순이익", key: "marNetProfit" },
    ];

    const handleInputChange = (e, fullDate, key) => {
        const newValue = e.target.value;
        setData(prevData =>
            prevData.map(item =>
                item.marDate === fullDate ? { ...item, [key]: Number(newValue) } : item
            )
        );
        setModifiedData(prev => {
            const updatedData = {
                ...(prev[fullDate] || {}),
                marDate: fullDate,
                [key]: Number(newValue)
            };

            const itemForDate = data.find(item => item.marDate === fullDate);
            if (key === "marAdBudget" && itemForDate) {
                updatedData.marTargetEfficiency = itemForDate.marTargetEfficiency;
            }
            if (key === "marTargetEfficiency" && itemForDate) {
                updatedData.marAdBudget = itemForDate.marAdBudget;
            }
            updatedData.id = itemForDate.id;
            return {
                ...prev,
                [fullDate]: updatedData
            };
        });
    };

    const handleCellClick = async (fullDate) => {
        try {
            console.log("선택한 날짜:", fullDate);
            console.log("캠페인 ID:", campaignId);

            const response = await createMarginTable({ targetDate: fullDate, campaignId });
            const marginId = response.data; // 백엔드가 리턴한 marginId

            const updateResponse = await getMarginByCampaignId({ startDate, endDate, campaignId });
            setData(updateResponse.data[0].data);


        } catch (error) {
            console.error("셀 클릭 후 마진 테이블 생성 실패:", error);
        }
    };

    useEffect(() => {
        onDataChange(modifiedData);
    }, [modifiedData]);

    const todayStr = new Date().toISOString().split('T')[0];

    return (
        <div className="table-container" ref={tableContainerRef}>
            <table className="campaign-data-table">
                <thead>
                    <tr>
                        <th className="option-name-header sticky-column"></th>
                        {dateRange.map(({ fullDate, displayDate }, index) => (
                            <th
                                key={fullDate}
                                ref={
                                    fullDate === todayStr
                                        ? todayRef
                                        : index === 0
                                            ? startDateRef
                                            : null
                                }
                                data-date={fullDate}
                                className="fixed-width-cell" // ✅ 클래스 추가
                            >
                                {displayDate}
                            </th>
                        ))}
                        <th className="sticky-column total-column">총합</th>
                    </tr>
                </thead>
                <tbody>
                    {options.map(option => (
                        <tr key={option.optionName}>
                            <td className="sticky-column">{option.optionName}</td>
                            {dateRange.map(({ fullDate }) => {
                                const itemForDate = data.find(item => item.marDate === fullDate);
                                let value = '-';
                                if (itemForDate) {
                                    if (option.key === "marAdRevenue") {
                                        value = itemForDate.marAdCost > 0
                                            ? ((itemForDate.marSales / itemForDate.marAdCost) * 100).toFixed(2) + '%'
                                            : '0.00%';
                                    } else if (option.key === "marAdCost") {
                                        value = formatNumber(Math.round(itemForDate.marAdCost * 1.1));
                                    } else if (option.key === "marCpcUnitCost") {
                                        value = itemForDate.marClicks > 0
                                            ? formatNumber(Math.round(itemForDate.marAdCost / itemForDate.marClicks))
                                            : '0';
                                    } else if (option.key === "marImpressions") {
                                        value = formatNumber(itemForDate.marImpressions);
                                    } else if (option.key === "marClicks") {
                                        value = itemForDate.marImpressions > 0
                                            ? ((itemForDate.marClicks / itemForDate.marImpressions) * 100).toFixed(2) + '%'
                                            : '0%';
                                    } else if (option.key === "marAdConversionSalesCount") {
                                        value = itemForDate.marClicks > 0
                                            ? formatNumber(((itemForDate.marAdConversionSalesCount / itemForDate.marClicks) * 100).toFixed(2)) + '%'
                                            : '0%';
                                    } else if (option.key === "marAdMargin") {
                                        value = formatNumber(itemForDate.marAdMargin);
                                    } else if (option.key === "marNetProfit") {
                                        value = formatNumber(Math.round(itemForDate.marNetProfit));
                                    } else if (option.key === "marReturnCost") {
                                        value = formatNumber(Math.round(itemForDate.marReturnCost));
                                    } else {
                                        value = itemForDate[option.key] || '-';
                                    }
                                }

                                let cellClass = "";
                                if (option.key === "marNetProfit") {
                                    const isNetProfitNegative = itemForDate && itemForDate.marNetProfit < 0;
                                    const isNetProfitZero = itemForDate && itemForDate.marNetProfit === 0;
                                    cellClass = isNetProfitNegative ? "negative-profit" : (isNetProfitZero ? "" : "positive-profit");
                                } else if (option.key === "marReturnCount") {
                                    cellClass = itemForDate && itemForDate.marReturnCount > 0 ? "red-return-count" : "";
                                }

                                return (
                                    <td
                                        key={fullDate}
                                        className={cellClass}
                                        onClick={() => {
                                            if (!(itemForDate)) {
                                                handleCellClick(fullDate);
                                            }
                                        }}
                                    >
                                        {option.editable && itemForDate ? (
                                            <input
                                                type="number"
                                                value={itemForDate[option.key] || ''}
                                                onChange={(e) => handleInputChange(e, fullDate, option.key)}
                                                placeholder="입력"
                                            />
                                        ) : (
                                            value
                                        )}
                                    </td>
                                );
                            })}
                            <td className="sticky-column total-column">
                                {["marAdRevenue", "marClicks", "marAdConversionSalesCount"].includes(option.key) ? (
                                    formatNumber(
                                        Math.floor(data.reduce((total, item) => {
                                            if (item.marAdCost > 0) {
                                                if (option.key === "marAdRevenue") {
                                                    return total + ((item.marAdMargin / item.marAdCost) * 100) || 0;
                                                } else if (option.key === "marClicks") {
                                                    return total + ((item.marClicks / item.marImpressions) * 100) || 0;
                                                } else if (option.key === "marAdConversionSalesCount") {
                                                    return total + ((item.marAdConversionSalesCount / item.marClicks) * 100) || 0;
                                                }
                                            }
                                            return total;
                                        }, 0) / dateRange.length)
                                    ) + '%'
                                ) : option.key === "marCpcUnitCost" ? (
                                    formatNumber(
                                        Math.floor(data.reduce((total, item) => {
                                            if (item.marClicks > 0) {
                                                return total + (item.marAdCost / item.marClicks) || 0;
                                            }
                                            return total;
                                        }, 0) / dateRange.length)
                                    )
                                ) : (
                                    formatNumber(
                                        Math.floor(data.reduce((total, item) => {
                                            if (["marAdCost", "marImpressions", "marActualSales", "marReturnCount", "marAdMargin", "marNetProfit", "marAdConversionSales", "marReturnCost"].includes(option.key)) {
                                                return total + (item[option.key] || 0);
                                            }
                                            return total;
                                        }, 0))
                                    )
                                )}
                            </td>
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    );
};

export default MarginDataTable;
