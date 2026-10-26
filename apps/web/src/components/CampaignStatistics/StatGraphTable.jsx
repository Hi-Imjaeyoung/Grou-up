import React from 'react';
import '../../styles/campaignStats/StatTable.css'
const StatGraphTable = ({ margin, search, nonSearch, startDate, endDate }) => {
    // 날짜를 배열로 생성
    const dateLabels = [];
    const start = new Date(startDate);
    const end = new Date(endDate);

    // 날짜를 추가
    for (let dt = start; dt <= end; dt.setDate(dt.getDate() + 1)) {
        dateLabels.push(dt.toISOString().split('T')[0]); // 'YYYY-MM-DD' 형식으로 변환
    }

    // Data 생성: 각 항목에 대해 search와 nonSearch의 값을 합산
    const getData = (key) => dateLabels.map(date => {
        const searchValue = search[date] ? search[date][key] : 0;
        const nonSearchValue = nonSearch[date] ? nonSearch[date][key] : 0;

        return searchValue + nonSearchValue; // 두 값을 더함
    });

    const getMarginData = (key) => dateLabels.map(date => {
        if (!margin || !margin.length || !margin[0].data) return 0;
        const marginForDate = margin[0].data.find(item => item.marDate === date);
        return marginForDate ? marginForDate[key] : 0;
    });

    const AdCostData = getData('adCost');
    const impressionsData = getData('impressions');
    const clicksData = getData('clicks');
    const totalSalesData = getData('totalSales');

    const marTargetEfficiencyData = getMarginData('marTargetEfficiency')
    const marAdBudgetData = getMarginData('marAdBudget')

    // clickRateData 생성
    const clickRateData = impressionsData.map((impressionss, index) => {
        const clicks = clicksData[index] || 0;
        return impressionss > 0 ? ((clicks / impressionss) * 100).toFixed(2) : 0; // 클릭율 계산
    });

    // cvrData 생성
    const cvrData = clicksData.map((clicks, index) => {
        const sales = totalSalesData[index] || 0;
        return clicks > 0 ? ((sales / clicks) * 100).toFixed(2) : 0; // 전환율 계산
    });

    const formatNumber = (num) => {
        if (num >= 10000) {
            return (num / 10000).toFixed(1) + '만'; // 1만, 2.1만 형식
        } else if (num >= 1000) {
            return (num / 1000).toFixed(0) + '천'; // 1천, 2천 형식
        }
        return num.toString(); // 천 이하의 숫자는 그대로 반환
    };


    return (
        <div>
            <div style={{ overflowX: 'auto', padding: '0px', width: 'auto' }}>
                <table style={{ fontSize: '8px', borderCollapse: 'collapse' }}>
                    <tbody>
                        <tr>
                            <td className="sticky-cell" style={{ padding: '5px', border: '1px solid #ddd' }}>날짜</td>
                            {dateLabels.map((date, index) => (
                                <th key={index} style={{ padding: '5px', border: '1px solid #ddd', whiteSpace: 'nowrap' }}>{date.slice(5)}</th>
                            ))}
                        </tr>
                    </tbody>
                    <tbody>
                        {/* 목표효율 Row */}
                        <tr>
                            <td className="sticky-cell" style={{ padding: '5px', border: '1px solid #ddd' }}>목표효율 (%)</td>
                            {marTargetEfficiencyData.map((targetEfficiencyData, index) => (
                                <td key={index} style={{ padding: '5px', border: '1px solid #ddd' }}>{targetEfficiencyData}</td>
                            ))}
                        </tr>
                        {/* 광고예산 */}
                        <tr>
                            <td className="sticky-cell" style={{ padding: '5px', border: '1px solid #ddd' }}>광고예산</td>
                            {marAdBudgetData.map((adBudgetData, index) => (
                                <td key={index} style={{ padding: '5px', border: '1px solid #ddd' }}>{adBudgetData}</td>
                            ))}
                        </tr>
                        {/* CVR Row */}
                        <tr>
                            <td className="sticky-cell" style={{ padding: '5px', border: '1px solid #ddd' }}>전환율 (%)</td>
                            {cvrData.map((cvr, index) => (
                                <td key={index} style={{ padding: '5px', border: '1px solid #ddd' }}>{cvr}</td>
                            ))}
                        </tr>

                        {/* Click Rate Row */}
                        <tr>
                            <td className="sticky-cell" style={{ padding: '5px', border: '1px solid #ddd' }}>클릭율 (%)</td>
                            {clickRateData.map((rate, index) => (
                                <td key={index} style={{ padding: '5px', border: '1px solid #ddd' }}>{rate}</td>
                            ))}
                        </tr>

                        {/* impressionss Row */}
                        <tr>
                            <td className="sticky-cell" style={{ padding: '5px', border: '1px solid #ddd' }}>노출 수</td>
                            {impressionsData.map((impressions, index) => (
                                <td key={index} style={{ padding: '5px', border: '1px solid #ddd', whiteSpace: 'nowrap' }}>
                                    {formatNumber(impressions)} {/* 포맷된 숫자 표시 */}
                                </td>
                            ))}
                        </tr>

                        {/* Clicks Row */}
                        <tr>
                            <td className="sticky-cell" style={{ padding: '5px', border: '1px solid #ddd' }}>클릭 수</td>
                            {clicksData.map((click, index) => (
                                <td key={index} style={{ padding: '5px', border: '1px solid #ddd' }}>{click}</td>
                            ))}
                        </tr>

                        {/* 광고전환판매수 */}
                        <tr>
                            <td className="sticky-cell" style={{ padding: '5px', border: '1px solid #ddd' }}>광고전환판매수</td>
                            {totalSalesData.map((sales, index) => (
                                <td key={index} style={{ padding: '5px', border: '1px solid #ddd' }}>{sales}</td>
                            ))}
                        </tr>
                    </tbody>
                </table>
            </div>
        </div>
    );
}

export default StatGraphTable;



