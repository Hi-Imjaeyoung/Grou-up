import React, { useState, useEffect } from 'react';
import { Doughnut } from 'react-chartjs-2';
import { Chart as ChartJS, ArcElement, Tooltip, Legend } from 'chart.js';
import "../../styles/Dashboard/DashboardCampaign.css";
import { getMyTotalSales } from '../../services/netSales';

ChartJS.register(ArcElement, Tooltip, Legend);
const DashboardCampaign = ({ selectedDate }) => {
    const [salesData, setSalesData] = useState(null);
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        const fetchData = async () => {
            setLoading(true);
            try {
                const response = await getMyTotalSales({ date: selectedDate });

                const data = response.data || response;
                setSalesData(data);
            } catch (error) {
                console.error("Error fetching sales data:", error);
                setSalesData(null);
            } finally {
                setLoading(false);
            }
        };
        if (selectedDate) {
            fetchData();
        }
    }, [selectedDate]);

    // 차트 데이터 구성
    const getChartData = () => {
        if (!salesData) {
            console.log("No salesData");
            return null;
        }


        const totalSales = salesData.totalSalesPrice || 0;
        const cancelPrice = Math.abs(salesData.totalCancelPrice || 0); // 음수를 양수로 변환
        const netSales = totalSales - cancelPrice; // 순 매출

        // 데이터가 모두 0이면 null 반환
        if (totalSales === 0 && cancelPrice === 0) {
            return null;
        }

        return {
            labels: ['순 매출', '취소 금액'],
            datasets: [{
                data: [netSales, cancelPrice],
                backgroundColor: [
                    '#2196F3', // 순 매출 - 파란색
                    '#FF6B6B', // 취소 금액 - 빨간색
                ],
                borderWidth: 2,
                borderColor: '#ffffff',
            }]
        };
    };

    const chartOptions = {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
            legend: {
                position: 'bottom',
                labels: {
                    padding: 10,
                    font: {
                        size: 11
                    }
                }
            },
            tooltip: {
                callbacks: {
                    label: function (context) {
                        const label = context.label || '';
                        const value = context.parsed || 0;
                        return `${label}: ₩${value.toLocaleString()}`;
                    }
                }
            }
        },
        cutout: '65%' // 도넛 구멍 크기
    };

    const chartData = getChartData();

    return (
        <div className="dashboard-card dashboard-campaign">
            <div className="card-label">총 매출(광고매출 + 자연판매)</div>

            {loading ? (
                <div style={{ textAlign: 'center', padding: '40px' }}>로딩 중...</div>
            ) : !chartData ? (
                <div style={{ textAlign: 'center', padding: '40px', fontSize: '15px', color: '#999' }}>
                    마진보고서 광고보고서 둘 다 업로드해주세요
                </div>
            ) : (
                <div style={{ padding: '10px 12px' }}>
                    {/* 차트 영역 */}
                    <div style={{ height: '190px', marginBottom: '1px' }}>
                        <Doughnut data={chartData} options={chartOptions} />
                    </div>

                    {/* 총계 정보 */}
                    <div style={{
                        textAlign: 'center',
                        fontSize: '13px',
                        color: '#666'
                    }}>
                        <div style={{ marginBottom: '6px' }}>
                            <strong>총 매출액:</strong> ₩{(salesData.totalSalesPrice || 0).toLocaleString()}

                            <strong> 취소 금액:</strong> ₩{Math.abs(salesData.totalCancelPrice || 0).toLocaleString()}
                        </div>
                        <div style={{
                            fontSize: '18px',
                            fontWeight: 'bold',
                            color: '#333',
                            marginTop: '10px',
                            borderTop: '1px solid #eee'
                        }}>
                            <strong>순 매출:</strong> ₩{((salesData.totalSalesPrice || 0) - Math.abs(salesData.totalCancelPrice || 0)).toLocaleString()}
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};
export default DashboardCampaign;