import React, { useEffect, useState, useContext, useCallback } from "react"; // useCallback 추가
import StatTable from "./StatTable";
import StatGraph from "./StatGraph";
import StatGraphTable from "./StatGraphTable";
import StatGraph2 from "./StatGraph2";
import StatGraph3 from "./StatGraph3";
import MemoDisplay from './MemoDisplay'; // 1. 방금 만든 메모 컴포넌트 import
import ProfitDonutChart from './ProfitDonutChart'; // 1. 방금 만든 도넛 차트 컴포넌트 import
import { getCampaignStats } from "../../services/keyword";
import { getMarginByCampaignId } from "../../services/margin.js";
import "../../styles/campaignStats/StatisticGrid.css";
import "../../styles/SwitchableGraphComponent.css"; // 인디케이터 CSS 파일 임포트
import { MyContext } from "../MyContext.jsx";

// 1. 중복되는 합산 로직을 별도의 함수로 분리 (리팩토링)
const aggregateStats = (statsObject) => {
    if (!statsObject) return {};
    return Object.values(statsObject).reduce((acc, stats) => {
        acc.adCost = (acc.adCost || 0) + (stats.adCost || 0);
        acc.adSales = (acc.adSales || 0) + (stats.adSales || 0);
        acc.clicks = (acc.clicks || 0) + (stats.clicks || 0);
        acc.impressions = (acc.impressions || 0) + (stats.impressions || 0);
        acc.totalSales = (acc.totalSales || 0) + (stats.totalSales || 0);
        return acc;
    }, {});
};

// 2. 인디케이터로 보여줄 그래프 컴포넌트들을 미리 매핑
const graphComponents = {
    roas: StatGraph,
    cost: StatGraph2,
    sales: StatGraph3,
};

// 그래프의 이름(key)과 화면에 표시될 이름(label)도 관리하면 좋아
const graphViews = [
    { key: 'roas', label: 'ROAS% / 광고비' },
    { key: 'cost', label: '비용' },
    { key: 'sales', label: '매출' },
];

const StatisticGrid = ({ campaignId, startDate, endDate }) => {
    const { memoData, getMemoAboutCampaign } = useContext(MyContext);
    // ✨ 1. state들을 로딩 상태와 함께 관리하도록 변경
    const [isLoading, setIsLoading] = useState(true); // 모든 데이터 로딩을 통제할 단일 state
    const [statsData, setStatsData] = useState({
        search: {},
        nonSearch: {},
        margin: {},
        aggregatedSearch: {},
        aggregatedNonSearch: {}
    });

    // 3. 어떤 그래프를 보여줄지 결정할 상태 추가
    const [activeGraph, setActiveGraph] = useState(graphViews[0].key); // 기본값으로 첫 번째 그래프 설정

    // ✨ 2. 데이터 fetching 로직을 하나로 통합하고 Promise.all 사용
    const fetchData = useCallback(async () => {
        setIsLoading(true); // 데이터 요청 시작 시 로딩 상태로 설정
        try {
            // 두 개의 API 요청을 동시에 보내고 모두 끝날 때까지 기다림
            const [campaignStatsResponse, marginResponse] = await Promise.all([
                getCampaignStats({ campaignId, start: startDate, end: endDate }),
                getMarginByCampaignId({ startDate, endDate, campaignId })
            ]);

            const campaignData = campaignStatsResponse.data || {};
            const marginData = marginResponse.data || {};

            const search = campaignData.search || {};
            const nonSearch = campaignData.nonSearch || {};

            // 모든 데이터가 준비되면 state를 한 번에 업데이트
            setStatsData({
                search: search,
                nonSearch: nonSearch,
                margin: marginData,
                aggregatedSearch: aggregateStats(search),
                aggregatedNonSearch: aggregateStats(nonSearch)
            });

            // 메모 데이터도 같이 불러옴
            getMemoAboutCampaign({ campaignId, start: startDate, end: endDate });

        } catch (error) {
            console.error("Error fetching campaign stats:", error);
            // 에러 발생 시 state를 초기 상태로 유지하거나 에러 상태를 별도로 관리
            setStatsData({
                search: {}, nonSearch: {}, margin: {},
                aggregatedSearch: {}, aggregatedNonSearch: {}
            });
        } finally {
            setIsLoading(false); // 요청이 성공하든 실패하든 로딩 상태 종료
        }
    }, [campaignId, startDate, endDate, getMemoAboutCampaign]);

    useEffect(() => {
        fetchData();
    }, [fetchData]);
    // 4. 현재 활성화된 그래프 컴포넌트를 가져온다.
    const ActiveGraphComponent = graphComponents[activeGraph];
    if (isLoading) {
        return <div className="grid-container-loading">데이터를 불러오는 중...</div>;
    }


    return (
        <div className="grid-container">
            <div className="grid-left-column">
                <div className="grid-item">
                    {/* 이제 데이터가 항상 준비되어 있으므로 복잡한 조건부 렌더링이 필요 없음 */}
                    <ProfitDonutChart
                        search={statsData.search}
                        nonSearch={statsData.nonSearch}
                    />
                </div>
                <div className="grid-memo">
                    <div style={{ height: '663px' }}>
                        {memoData ? <MemoDisplay memoData={memoData} /> : <div>메모 로딩 중...</div>}
                    </div>
                </div>
            </div>
            <div className="grid-right-column">
                <div className="grid-item">
                    <StatTable
                        search={statsData.aggregatedSearch}
                        nonSearch={statsData.aggregatedNonSearch}
                    />
                </div>

                {/* 5. 여기에 인디케이터와 그래프 렌더링 로직 추가 */}
                <div className="switchable-graph-container">
                    <div className="view-switcher">
                        {graphViews.map((view) => (
                            <button
                                key={view.key}
                                className={`dot-button ${activeGraph === view.key ? 'active' : ''}`}
                                onClick={() => setActiveGraph(view.key)}
                                aria-label={view.label}
                            />
                        ))}
                    </div>
                    <div className="graph-content-area">
                        {/* ActiveGraphComponent가 null이 아닐 때만 렌더링 */}
                        <div style={{ height: '300px' }}>
                            {ActiveGraphComponent && (
                                <ActiveGraphComponent
                                    search={statsData.search}
                                    nonSearch={statsData.nonSearch}
                                    memoData={memoData} // StatGraph에만 필요하다면 아래처럼 조건부로 전달 가능
                                    startDate={startDate}
                                    endDate={endDate}
                                />
                            )}
                        </div>
                    </div>
                </div>

                <div className="grid-item">
                    <div style={{ height: '250px' }}>
                        <StatGraphTable
                            margin={statsData.margin}
                            search={statsData.search}
                            nonSearch={statsData.nonSearch}
                            startDate={startDate}
                            endDate={endDate}
                        />
                    </div>
                </div>
            </div>
        </div>
    );
};

export default StatisticGrid;