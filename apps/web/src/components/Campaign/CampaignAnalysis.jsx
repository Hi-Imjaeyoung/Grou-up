import React, { useState, useEffect } from "react";
import { useNavigate } from 'react-router-dom';
import "../../styles/Campaign/CampaignAnalysis.css";
import DateRangeCalendar from "../Date/DateRangeCalendar";
import { getMyCampaignsAnalysis, getDailyMarginSummary } from "../../services/CampaignService";

// 이 함수는 props나 state를 사용하지 않으므로 컴포넌트 밖에 두는 것이 좋아.
const calculateRoas = (sales, cost) => {
    if (!cost || cost === 0) {
        return "0%";
    }
    return `${Math.round((sales / cost) * 100).toLocaleString()}%`;
};
// ✨ 1. 캠페인 타입과 CSS 클래스를 매핑하는 객체와 헬퍼 함수 추가
const tagClassMapping = {
    '매출 최적화': 'TagBlue',
    '수동 성과형': 'TagGreen',
    '간편 매출 스타트': 'TagYellow',
    'default': 'TagGray' // 기본값 또는 알 수 없는 타입을 위한 클래스
};
const getTagClass = (campaignType) => {
    return tagClassMapping[campaignType] || tagClassMapping['default'];
};

const CampaignAnalysis = () => {
    const navigate = useNavigate();
    const [activeTab, setActiveTab] = useState('total');
    const [campaignCardData, setCampaignCardData] = useState(null);
    const [productReportData, setProductReportData] = useState([]);
    const [marginReportData, setMarginReportData] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

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

    const handleNavigateToDetails = (campaignName, campaignId) => {
        if (!campaignId) {
            console.error("이동할 캠페인 ID가 없습니다.");
            return;
        }
        navigate(`/campaigns/${campaignId}?title=${campaignName}`);
    };

    // // [개선] 마진 계산 페이지 이동 함수는 이제 사용하지 않으므로 삭제하거나 주석 처리!
    const handleNavigateToMargin = () => {
        navigate(`/margin-calculator`);
    };

    useEffect(() => {
        const fetchData = async () => {
            setLoading(true);
            setError(null); // [개선] 새로운 fetch 시작 시 이전 에러 상태 초기화
            try {
                // 1. 캠페인 분석 데이터 가져오기
                const response = await getMyCampaignsAnalysis({ start: startDate, end: endDate });
                const apiCardData = response.sumOfAdSalesAndAdCostByCampaignType;
                console.log(response);
                const formattedCardData = {
                    total: {
                        adSpend: apiCardData['총 매출']?.adCost?.toLocaleString() || '0',
                        adRevenue: apiCardData['총 매출']?.adSales?.toLocaleString() || '0',
                        roas: calculateRoas(apiCardData['총 매출']?.adSales, apiCardData['총 매출']?.adCost)
                    },
                    salesOptimization: {
                        adSpend: apiCardData['매출 최적화']?.adCost?.toLocaleString() || '0',
                        adRevenue: apiCardData['매출 최적화']?.adSales?.toLocaleString() || '0',
                        roas: calculateRoas(apiCardData['매출 최적화']?.adSales, apiCardData['매출 최적화']?.adCost)
                    },
                    manualPerformance: {
                        adSpend: apiCardData['수동 성과형']?.adCost?.toLocaleString() || '0',
                        adRevenue: apiCardData['수동 성과형']?.adSales?.toLocaleString() || '0',
                        roas: calculateRoas(apiCardData['수동 성과형']?.adSales, apiCardData['수동 성과형']?.adCost)
                    },
                    easyStart: {
                        adSpend: apiCardData['간편 매출 스타트']?.adCost?.toLocaleString() || '0',
                        adRevenue: apiCardData['간편 매출 스타트']?.adSales?.toLocaleString() || '0',
                        roas: calculateRoas(apiCardData['간편 매출 스타트']?.adSales, apiCardData['간편 매출 스타트']?.adCost)
                    }
                };
                setCampaignCardData(formattedCardData);

                // 2. 상품 보고서 데이터 가공하기
                const apiProductData = response.adSalesAndAdCostByCampaignName || {};
                const formattedProductData = Object.entries(apiProductData).map(([name, data]) => ({
                    id: data.campaignId,
                    type: data.campAdType,
                    name: name,
                    adSpend: data.adCost.toLocaleString(),
                    adRevenue: data.adSales.toLocaleString(),
                    roas: calculateRoas(data.adSales, data.adCost),
                }));
                setProductReportData(formattedProductData);

                // 3. 마진 보고서 데이터 가져오기
                const marginResponse = await getDailyMarginSummary({ start: startDate, end: endDate });
                const apiMarginData = marginResponse || [];

                // 4. 마진 보고서 데이터 가공하기
                const formattedMarginData = apiMarginData.map(item => {
                    // [핵심 수정] stale state인 productReportData 대신,
                    // 바로 위에서 만든 최신 데이터인 formattedProductData를 사용!
                    const productInfo = formattedProductData.find(p => p.name === item.marProductName);

                    return {
                        id: productInfo ? productInfo.id : null, // 찾은 ID를 할당
                        name: item.marProductName,
                        adMargin: item.marAdMargin.toLocaleString(),
                        netProfit: item.marNetProfit.toLocaleString(),
                    };
                });
                setMarginReportData(formattedMarginData);

            } catch (err) {
                setError(err);
                console.error("캠페인 분석 데이터 로딩 실패:", err);
            } finally {
                setLoading(false);
            }
        };

        fetchData();
    }, [startDate, endDate]); // navigate는 dependency에 포함할 필요 없어.

    const handleTabClick = (tabKey) => {
        setActiveTab(tabKey);
    };

    const renderCampaignCard = (key, label) => (
        <button
            className={`CampaignCard CampaignCard--${key} ${activeTab === key ? 'active-tab' : ''}`}
            onClick={() => handleTabClick(key)}
        >
            <span className="CampaignCard_label">{label}</span>
            <div className="CampaignCard_content">
                <div className="MetricRow">
                    <span className="MetricLabel">광고비</span>
                    <span className="MetricValue">{campaignCardData?.[key]?.adSpend}</span>
                </div>
                <div className="MetricRow">
                    <span className="MetricLabel">광고 매출</span>
                    <span className="MetricValue">{campaignCardData?.[key]?.adRevenue}</span>
                </div>
                <div className="MetricRow">
                    <span className="MetricLabel">ROAS</span>
                    <span className="MetricValue">{campaignCardData?.[key]?.roas}</span>
                </div>
            </div>
        </button>
    );

    if (loading && !campaignCardData) {
        return <div className="CampaignAnalysisPage"><h2>데이터를 불러오는 중입니다... 📈</h2></div>;
    }

    if (error) {
        return <div className="CampaignAnalysisPage"><h2>엑셀 업로드 먼저 부탁드려요 !</h2></div>;
    }

    return (
        <div className={`CampaignAnalysisPage ${loading ? 'reloading' : ''}`}>
            <div className="PageHeader">
                <h2 className="PageTitle">광고 캠페인 분석</h2>
                <div className="date-selection-container">
                    <button className="date-selection-button" onClick={toggleCalendar}>
                        {startDate.replaceAll('-', '.')} ~ {endDate.replaceAll('-', '.')} <span className="dropdown-arrow">▼</span>
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
            </div>

            <div className="CampaignCardContainer">
                {renderCampaignCard('total', '전체')}
                {renderCampaignCard('salesOptimization', '매출 최적화')}
                {renderCampaignCard('manualPerformance', '수동 성과형')}
                {renderCampaignCard('easyStart', '간편 매출 스타트')}
            </div>

            <div className="ContentGrid">
                <div className="ContentBlock">
                    <h3>상품 보고서</h3>
                    <div className="TableContainer">
                        <table>
                            <thead>
                                <tr>
                                    <th>타입</th>
                                    <th>캠페인 이름</th>
                                    <th>광고비</th>
                                    <th>광고매출</th>
                                    <th>ROAS</th>
                                </tr>
                            </thead>
                            <tbody>
                                {productReportData.map((item) => (
                                    // [개선] key 속성에 고유한 id 값을 사용
                                    <tr key={item.id}>
                                        {/* ✨ 2. <td> 안의 span 클래스를 헬퍼 함수로 동적으로 할당 */}
                                        <td>
                                            <span className={`Tag ${getTagClass(item.type)}`}>
                                                {item.type}
                                            </span>
                                        </td>
                                        <td className="clickable-cell" onClick={() => handleNavigateToDetails(item.name, item.id)}>
                                            {item.name} <span className="ExternalLinkIcon">↗</span>
                                        </td>
                                        <td>{item.adSpend}</td>
                                        <td>{item.adRevenue}</td>
                                        <td>{item.roas}</td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                </div>

                <div className="ContentBlock">
                    <h3>마진 보고서</h3>
                    <div className="TableContainer">
                        <table>
                            <thead>
                                <tr>
                                    <th>캠페인 이름</th>
                                    <th>광고 마진</th>
                                    <th>순이익</th>
                                </tr>
                            </thead>
                            <tbody>
                                {marginReportData.map((item) => (
                                    <tr key={item.id || item.name}>
                                        {/* [개선] 마진 보고서도 클릭하면 상세 페이지로 이동하도록 통일 */}
                                        <td className="clickable-cell" onClick={() => handleNavigateToMargin()}>
                                            {item.name} <span className="ExternalLinkIcon">↗</span>
                                        </td>
                                        <td>{item.adMargin}</td>
                                        <td>{item.netProfit}</td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default CampaignAnalysis;