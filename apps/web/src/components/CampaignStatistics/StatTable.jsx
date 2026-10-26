import React from 'react';

const StatTable = ({ search, nonSearch }) => {
    // 계산 함수
    const calculateClickRate = (clicks, impressionss) => {
        return impressionss > 0 ? ((clicks / impressionss) * 100).toFixed(2) : 0;
    };

    const calculateConversionRate = (sales, clicks) => {
        return clicks > 0 ? ((sales / clicks) * 100).toFixed(2) : 0;
    };

    const calculateCPC = (adcost, clicks) => {
        return clicks > 0 ? (adcost / clicks).toFixed(0) : 0;
    };

    const calculateCPM = (adcost, impressionss) => {
        return impressionss > 0 ? ((adcost / impressionss) * 1000).toFixed(0) : 0;
    };

    const calculateROAS = (adsales, adcost) => {
        return adcost > 0 ? ((adsales / adcost) * 100).toFixed(2) : 0;
    };

    const calculateCostPerConversion = (adcost, totalSales) => {
        return totalSales > 0 ? (adcost / totalSales).toFixed(0) : 0;
    };

    // 숫자를 보기 좋게 포맷팅하는 함수
    const formatNumber = (num) => {
        return new Intl.NumberFormat('ko-KR').format(num);
    };

    return (
        <div>
            <table style={{ borderCollapse: 'collapse' }}>
                <thead>
                    <tr>
                        <th>노출 영역</th>
                        <th>광고비</th>
                        <th>광고매출</th>
                        <th>클릭수</th>
                        <th>노출수</th>
                        <th>총 주문</th>
                        <th>클릭률</th>
                        <th>전환율</th>
                        <th>CPC</th>
                        <th>CPM</th>
                        <th>ROAS</th>
                        <th>전환 당 비용</th>
                    </tr>
                </thead>
                <tbody>
                    <tr>
                        <td>검색</td>
                        <td>{formatNumber(search?.adCost || 0)}원</td>
                        <td>{formatNumber(search?.adSales || 0)}원</td>
                        <td>{formatNumber(search?.clicks || 0)}</td>
                        <td>{formatNumber(search?.impressions || 0)}</td>
                        <td>{formatNumber(search?.totalSales || 0)}</td>
                        <td>{calculateClickRate(search?.clicks || 0, search?.impressions || 0)}%</td>
                        <td>{calculateConversionRate(search?.totalSales || 0, search?.clicks || 0)}%</td>
                        <td>{formatNumber(calculateCPC(search?.adCost || 0, search?.clicks || 0))}원</td>
                        <td>{formatNumber(calculateCPM(search?.adCost || 0, search?.impressions || 0))}원</td>
                        <td>{calculateROAS(search?.adSales || 0, search?.adCost || 0)}%</td>
                        <td>{formatNumber(calculateCostPerConversion(search?.adCost || 0, search?.totalSales || 0))}원</td>
                    </tr>
                    <tr>
                        <td>비 검색</td>
                        <td>{formatNumber(nonSearch?.adCost || 0)}원</td>
                        <td>{formatNumber(nonSearch?.adSales || 0)}원</td>
                        <td>{formatNumber(nonSearch?.clicks || 0)}</td>
                        <td>{formatNumber(nonSearch?.impressions || 0)}</td>
                        <td>{formatNumber(nonSearch?.totalSales || 0)}</td>
                        <td>{calculateClickRate(nonSearch?.clicks || 0, nonSearch?.impressions || 0)}%</td>
                        <td>{calculateConversionRate(nonSearch?.totalSales || 0, nonSearch?.clicks || 0)}%</td>
                        <td>{formatNumber(calculateCPC(nonSearch?.adCost || 0, nonSearch?.clicks || 0))}원</td>
                        <td>{formatNumber(calculateCPM(nonSearch?.adCost || 0, nonSearch?.impressions || 0))}원</td>
                        <td>{calculateROAS(nonSearch?.adSales || 0, nonSearch?.adCost || 0)}%</td>
                        <td>{formatNumber(calculateCostPerConversion(nonSearch?.adCost || 0, nonSearch?.totalSales || 0))}원</td>
                    </tr>
                    <tr>
                        <td>합계</td>
                        <td>{formatNumber((search?.adCost || 0) + (nonSearch?.adCost || 0))}원</td>
                        <td>{formatNumber((search?.adSales || 0) + (nonSearch?.adSales || 0))}원</td>
                        <td>{formatNumber((search?.clicks || 0) + (nonSearch?.clicks || 0))}</td>
                        <td>{formatNumber((search?.impressions || 0) + (nonSearch?.impressions || 0))}</td>
                        <td>{formatNumber((search?.totalSales || 0) + (nonSearch?.totalSales || 0))}</td>
                        <td>{calculateClickRate((search?.clicks || 0) + (nonSearch?.clicks || 0), (search?.impressions || 0) + (nonSearch?.impressions || 0))}%</td>
                        <td>{calculateConversionRate((search?.totalSales || 0) + (nonSearch?.totalSales || 0), (search?.clicks || 0) + (nonSearch?.clicks || 0))}%</td>
                        <td>{formatNumber(calculateCPC((search?.adCost || 0) + (nonSearch?.adCost || 0), (search?.clicks || 0) + (nonSearch?.clicks || 0)))}원</td>
                        <td>{formatNumber(calculateCPM((search?.adCost || 0) + (nonSearch?.adCost || 0), (search?.impressions || 0) + (nonSearch?.impressions || 0)))}원</td>
                        <td>{calculateROAS((search?.adSales || 0) + (nonSearch?.adSales || 0), (search?.adCost || 0) + (nonSearch?.adCost || 0))}%</td>
                        <td>{formatNumber(calculateCostPerConversion((search?.adCost || 0) + (nonSearch?.adCost || 0), (search?.totalSales || 0) + (nonSearch?.totalSales || 0)))}원</td>
                    </tr>
                </tbody>
            </table>
        </div>
    );
};

export default StatTable;
