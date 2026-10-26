import React, { useState, useMemo, useEffect } from 'react';
import { getMarginOverview, getMarginOverviewGraph } from '../../services/margin';
import DailyTrendChart from './DailyTrendChart';
import DashboardPieChartCard from './DashboardPieChartCard';
import DashboardDetailCard from './DashboardDetailCard';
import DateRangeCalendar from '../Date/DateRangeCalendar';
import '../../styles/margin/MarginOverview.css';

const formatCurrency = (value) => {
    if (typeof value !== 'number') return '0';
    return value.toLocaleString();
};

const COLORS = ['#CADBFF', '#628CE8', '#F48785', '#FFD16F', '#816D79', '#5C62B8'];

const MarginOverview = () => {
    const today = new Date();
    const firstDayOfMonth = new Date(today.getFullYear(), today.getMonth(), 1).toISOString().slice(0, 10);
    const todayDate = today.toISOString().slice(0, 10);

    const [startDate, setStartDate] = useState(firstDayOfMonth);
    const [endDate, setEndDate] = useState(todayDate);
    const [showCalendar, setShowCalendar] = useState(false);

    const toggleCalendar = () => setShowCalendar(v => !v);

    const handleDateRangeChange = ({ startDate, endDate }) => {
        setStartDate(startDate);
        setEndDate(endDate);
    };

    const [overviewData, setOverviewData] = useState([]);
    const [graphData, setGraphData] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [hoveredProduct, setHoveredProduct] = useState(null);

    useEffect(() => {
        const fetchAllData = async () => {
            setLoading(true);
            setError(null);
            try {
                const [overviewRes, graphRes] = await Promise.all([
                    getMarginOverview({ start: startDate, end: endDate }),
                    getMarginOverviewGraph({ start: startDate, end: endDate })
                ]);
                setOverviewData(overviewRes.data || []);
                setGraphData(graphRes.data || []);
            } catch (err) {
                setError("데이터를 불러오는 데 실패했습니다.");
                console.error(err);
            } finally {
                setLoading(false);
            }
        };
        fetchAllData();
    }, [startDate, endDate]);

    const processedData = useMemo(() => {
        if (!overviewData || overviewData.length === 0) {
            return { pieDataRevenue: [], pieDataNetProfit: [], detailData: {}, totalRevenue: 0, totalNetProfit: 0 };
        }
        const pieDataRevenue = overviewData.map(item => ({ name: item.campaignName, value: item.marSales || 0 }));
        const pieDataNetProfit = overviewData.map(item => ({ name: item.campaignName, value: item.marNetProfit || 0 }));
        const detailData = overviewData.reduce((acc, item) => {
            acc[item.campaignName] = item;
            return acc;
        }, {});
        const totalRevenue = overviewData.reduce((sum, item) => sum + (item.marSales || 0), 0);
        const totalNetProfit = overviewData.reduce((sum, item) => sum + (item.marNetProfit || 0), 0);
        return { pieDataRevenue, pieDataNetProfit, detailData, totalRevenue, totalNetProfit };
    }, [overviewData]);

    const firstCampaignName = processedData.pieDataRevenue[0]?.name;
    const detailDisplayData = hoveredProduct ? processedData.detailData[hoveredProduct] : (firstCampaignName ? processedData.detailData[firstCampaignName] : null);

    const PieTooltip = ({ active, payload }) => {
        if (active && payload && payload.length) {
            return (
                <div className="pie-tooltip-custom">
                    <p>{`${payload[0].name}: ${formatCurrency(payload[0].value)}원`}</p>
                </div>
            );
        }
        return null;
    };

    if (loading) return <div className="loading-container">로딩 중...</div>;
    if (error) return <div className="loading-container error-message">{error}</div>;

    return (
        <div className="form-main-content">
            <header className="dashboard-header">
                <h1 className="dashboard-title">마진 계산기 - 대시보드</h1>

                <div className="date-selection-container">
                    <button className="date-selection-button" onClick={toggleCalendar}>
                        {startDate.replaceAll('-', '.')} ~ {endDate.replaceAll('-', '.')}
                    </button>
                    {showCalendar && (
                        <>
                            <div className="date-picker-overlay" onClick={toggleCalendar}></div>
                            <div className="date-picker-modal">
                                <DateRangeCalendar
                                    initialStartDate={startDate}
                                    initialEndDate={endDate}
                                    onDateRangeChange={handleDateRangeChange}
                                    onClose={toggleCalendar}
                                />
                            </div>
                        </>
                    )}
                </div>
            </header>

            {/* 이제 나머지 컨텐츠는 헤더의 '형제'가 됩니다. */}
            <div className="dashboard-page-container">
                <section className="dashboard-cards-section">
                    <DashboardPieChartCard
                        title="광고 매출"
                        data={processedData.pieDataRevenue}
                        totalValue={processedData.totalRevenue}
                        onHover={setHoveredProduct}
                        formatCurrency={formatCurrency}
                        COLORS={COLORS}
                        PieTooltip={PieTooltip}
                    />
                    <DashboardPieChartCard
                        title="순이익"
                        data={processedData.pieDataNetProfit}
                        totalValue={processedData.totalNetProfit}
                        onHover={setHoveredProduct}
                        formatCurrency={formatCurrency}
                        COLORS={COLORS}
                        PieTooltip={PieTooltip}
                    />
                    <DashboardDetailCard
                        data={detailDisplayData}
                        hoveredProduct={hoveredProduct}
                        formatCurrency={formatCurrency}
                    />
                </section>

                <DailyTrendChart
                    chartApiData={graphData}
                    title="일별 매출 / 순이익 추이"
                    formatCurrency={formatCurrency}
                />
            </div>
        </div>
    );
};

export default MarginOverview;