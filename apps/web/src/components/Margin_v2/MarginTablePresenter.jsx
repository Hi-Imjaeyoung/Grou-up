import React, { useEffect, useRef } from 'react';
import { formatNumber } from "../../utils/formatUtils";
import "../../styles/margin/MarginDataTable.css"; // 고유 클래스명으로 수정된 CSS 파일을 import 해주세요

const MarginTablePresenter = ({ data, dateRange, isInitialLoading, handleInputChange, handleCellClick, handleSave, campaignId }) => {
    const tableContainerRef = useRef(null);
    const hasScrolledRef = useRef(false);

    useEffect(() => {
        hasScrolledRef.current = false;
    }, [dateRange]);

    useEffect(() => {
        // 데이터가 없거나, 날짜가 없거나, **이미 스크롤을 했다면(true)** 실행하지 않음
        if (!data.length || !dateRange.length || hasScrolledRef.current) return;

        const validDates = data
            .map(item => item.marDate)
            .filter(Boolean)
            .sort();

        // 유효한 데이터가 없으면 중단 (계속 재실행되는 것 방지)
        if (validDates.length === 0) return;

        const lastDataDate = validDates[validDates.length - 1];
        const isLastDateInRange = dateRange.some(d => d.fullDate === lastDataDate);

        const performScroll = () => {
            if (!tableContainerRef.current) return;

            const container = tableContainerRef.current;
            const lastRef = document.querySelector(`[data-date="${lastDataDate}"]`);

            if (isLastDateInRange && lastRef) {
                const scrollLeft = lastRef.offsetLeft
                    - container.clientWidth
                    + lastRef.offsetWidth;

                container.scrollTo({
                    left: scrollLeft,
                    behavior: 'smooth',
                });
            } else {
                container.scrollTo({
                    left: 0,
                    behavior: 'smooth',
                });
            }

            // 3. [추가] 스크롤 실행 후 "완료됨"으로 표시 -> 이후 데이터가 바껴도 스크롤 안 함
            hasScrolledRef.current = true;
        };

        const timeoutId = setTimeout(() => {
            requestAnimationFrame(performScroll);
        }, 100);

        return () => clearTimeout(timeoutId);
    }, [isInitialLoading, dateRange, data]);

    const options = [
        { optionName: "목표효율", key: "marTargetEfficiency", editable: true, totalType: 'averageValue', unit: '%', showSaveButton: true, isGroupStart: true },
        { optionName: "광고예산", key: "marAdBudget", editable: true, totalType: 'sum', showSaveButton: true },
        { optionName: "광고수익률", key: "marAdRevenue", totalType: 'averageRate', unit: '%', isGroupStart: true, className: 'margin-table-highlight-row' },
        { optionName: "집행광고비 * 1.1", key: "marAdCost", totalType: 'sum', className: 'margin-table-highlight-row' },
        { optionName: "CPC단가", key: "marCpcUnitCost", totalType: 'averageValue', isGroupStart: true },
        { optionName: "노출수", key: "marImpressions", totalType: 'sum' },
        { optionName: "클릭률", key: "marCtr", totalType: 'averageRate', unit: '%' },
        { optionName: "전환률", key: "marCvr", totalType: 'averageRate', unit: '%' },
        { optionName: "광고 전환 판매 수", key: "marAdConversionSales", totalType: 'sum', isGroupStart: true, className: 'margin-table-highlight-row' },
        { optionName: "실제 판매 수", key: "marActualSales", totalType: 'sum', className: 'margin-table-highlight-row' },
        {
            optionName: "순이익",
            key: "marNetProfit",
            totalType: 'sum',
            className: 'margin-table-highlight-row',
            hasTooltip: true,
            tooltipText: "반품 비용은 포함되지 않습니다."
        },
        { optionName: "반품 수", key: "marReturnCount", totalType: 'sum', isGroupStart: true },
        { optionName: "반품 비용", key: "marReturnCost", totalType: 'sum' },
    ];

    return (
        <div className="margin-table-wrapper" ref={tableContainerRef}>
            <table className="margin-table-data">
                <thead>
                    <tr>
                        {/* ✅ 캠페인 이름 셀은 비워두고 관련 클래스 제거 */}
                        <th className="margin-table-sticky-col margin-table-first-col margin-table-header-cell"></th>
                        <th className="margin-table-sticky-col margin-table-second-col margin-table-header-cell">합계/평균</th>
                        {dateRange.map(({ fullDate, displayDate }) => (
                            <th key={fullDate} data-date={fullDate} className="margin-table-header-cell margin-table-date-cell">{displayDate}</th>
                        ))}
                    </tr>
                </thead>
                <tbody>
                    {options.map((option) => (
                        <tr
                            key={option.key || option.groupTitle}
                            className={`${option.isGroupStart ? 'margin-table-group-start' : ''} ${option.className || ''}`.trim()}
                        >
                            <td className="margin-table-sticky-col margin-table-first-col margin-table-option-name">
                                <div className="margin-table-option-container">
                                    <span className="margin-table-option-text">
                                        {option.optionName}
                                        {option.hasTooltip && (
                                            <span className="margin-table-tooltip-container">
                                                <span className="margin-table-tooltip-icon">?</span>
                                                <span className="margin-table-tooltip">{option.tooltipText}</span>
                                            </span>
                                        )}
                                    </span>
                                    {option.showSaveButton && (
                                        <button className="margin-table-save-button" onClick={() => handleSave(campaignId)}>저장</button>
                                    )}
                                </div>
                            </td>
                            <td className="margin-table-sticky-col margin-table-second-col margin-table-total-cell">
                                <b>
                                    {/* 합계 계산 로직 (수정 없음) */}
                                    {(() => {
                                        if (!data || data.length === 0) return option.totalType === 'none' ? '-' : '0';
                                        switch (option.totalType) {
                                            case 'sum': {
                                                const sum = data.reduce((total, item) => total + (Number(item[option.key]) || 0), 0);
                                                const finalSum = option.key === 'marAdCost' ? sum * 1.1 : sum;
                                                return formatNumber(Math.round(finalSum));
                                            }
                                            case 'averageValue': {
                                                if (option.key === 'marCpcUnitCost') {
                                                    const totalAdCost = data.reduce((sum, item) => sum + (item.marAdCost || 0), 0) * 1.1;
                                                    const totalClicks = data.reduce((sum, item) => sum + (item.marClicks || 0), 0);
                                                    return totalClicks > 0 ? formatNumber(Math.round(totalAdCost / totalClicks)) : '0';
                                                }
                                                const relevantData = data.filter(item => item[option.key] != null && item[option.key] > 0);
                                                const totalValue = relevantData.reduce((total, item) => total + (Number(item[option.key]) || 0), 0);
                                                return relevantData.length > 0 ? formatNumber(Math.round(totalValue / relevantData.length)) : '0';
                                            }
                                            case 'averageRate': {
                                                let totalRate = 0;
                                                if (option.key === 'marAdRevenue') {
                                                    const totalSales = data.reduce((sum, item) => sum + (item.marSales || 0), 0);
                                                    const totalAdCost = data.reduce((sum, item) => sum + (item.marAdCost || 0), 0) * 1.1;
                                                    totalRate = totalAdCost > 0 ? (totalSales / totalAdCost) * 100 : 0;
                                                } else if (option.key === 'marCtr') {
                                                    const totalClicks = data.reduce((sum, item) => sum + (item.marClicks || 0), 0);
                                                    const totalImpressions = data.reduce((sum, item) => sum + (item.marImpressions || 0), 0);
                                                    totalRate = totalImpressions > 0 ? (totalClicks / totalImpressions) * 100 : 0;
                                                } else if (option.key === 'marCvr') {
                                                    const totalConversions = data.reduce((sum, item) => sum + (item.marAdConversionSalesCount || 0), 0);
                                                    const totalClicks = data.reduce((sum, item) => sum + (item.marClicks || 0), 0);
                                                    totalRate = totalClicks > 0 ? (totalConversions / totalClicks) * 100 : 0;
                                                }
                                                return totalRate.toFixed(1) + '%';
                                            }
                                            default:
                                                return '-';
                                        }
                                    })()}
                                </b>
                            </td>
                            {dateRange.map(({ fullDate }) => {
                                const itemForDate = data.find(item => item.marDate === fullDate);
                                let value = '-';

                                if (itemForDate) {
                                    if (option.key === "marAdRevenue") {
                                        const adCost = (itemForDate.marAdCost || 0) * 1.1;
                                        value = adCost > 0 ? `${Math.round((itemForDate.marSales / adCost) * 100)}%` : '0%';
                                    } else if (option.key === "marAdCost") {
                                        value = formatNumber(Math.round((itemForDate.marAdCost || 0) * 1.1));
                                    } else if (option.key === "marCpcUnitCost") {
                                        const adCost = (itemForDate.marAdCost || 0) * 1.1;
                                        const clicks = itemForDate.marClicks || 0;
                                        value = clicks > 0 ? formatNumber(Math.round(adCost / clicks)) : '0';
                                    } else if (option.key === "marCtr") {
                                        const impressions = itemForDate.marImpressions || 0;
                                        value = impressions > 0 ? `${((itemForDate.marClicks / impressions) * 100).toFixed(1)}%` : '0.0%';
                                    } else if (option.key === "marCvr") {
                                        const clicks = itemForDate.marClicks || 0;
                                        value = clicks > 0 ? `${((itemForDate.marAdConversionSalesCount / clicks) * 100).toFixed(1)}%` : '0.0%';
                                    } else if (option.key === "marNetProfit") {
                                        value = formatNumber(Math.round(itemForDate.marNetProfit));
                                    } else if (option.unit === '%') {
                                        value = `${itemForDate[option.key] || 0}%`;
                                    } else {
                                        value = formatNumber(itemForDate[option.key]) ?? '-';
                                    }
                                }

                                let cellClass = "margin-table-data-cell";
                                if (option.key === "marNetProfit" && itemForDate?.marNetProfit < 0) {
                                    cellClass += " margin-table-negative-profit";
                                }

                                return (
                                    <td key={fullDate} className={`${cellClass} margin-table-date-cell`} onClick={() => !itemForDate && handleCellClick(fullDate)}>
                                        <b>
                                            {option.editable && itemForDate ? (
                                                <input type="number" value={itemForDate[option.key] || ''} onChange={(e) => handleInputChange(e, fullDate, option.key)} />
                                            ) : (
                                                value
                                            )}
                                        </b>
                                    </td>
                                );
                            })}
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    );
};

export default MarginTablePresenter;