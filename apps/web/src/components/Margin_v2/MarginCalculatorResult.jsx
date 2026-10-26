import React, { useState, useEffect, useCallback, useRef } from "react";
// import CampaignDataTable from "./MarginDataTable";
import MarginDataTable from "./MarginDataTable";
import MarginResultModal from "./MarginResultModal";
import MarginNetTable from "../Margin_v2/MarginNetTable";
import { getMarginByCampaignId } from "../../services/margin";
import { updateEfficiencyAndAdBudget } from "../../services/margin";
import DateRangeCalendar from "../Date/DateRangeCalendar";
import '../../styles/margin/MarginCalculatorResult.css'

const MarginCalculatorResult = ({ campaigns }) => {

    const [expandedCampaignId, setExpandedCampaignId] = useState(new Set());
    const [tableData, setTableData] = useState([]);
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [modifiedData, setModifiedData] = useState({});
    const [isDataLoaded, setIsDataLoaded] = useState(false);

    const marginNetTableRef = useRef(null);

    const today = new Date();
    const firstDayOfMonth = new Date(today.getFullYear(), today.getMonth(), 1).toISOString().slice(0, 10);
    const todayDate = today.toISOString().slice(0, 10);

    const [startDate, setStartDate] = useState(firstDayOfMonth);
    const [endDate, setEndDate] = useState(todayDate);
    const [showCalendar, setShowCalendar] = useState(false);

    const toggleCalendar = () => setShowCalendar(v => !v);

    const refreshMarginNetTable = useCallback(() => {
        if (marginNetTableRef.current && typeof marginNetTableRef.current.refreshData === 'function') {
            marginNetTableRef.current.refreshData();
        }
    }, []);

    const handleDateRangeChange = ({ startDate, endDate }) => {
        setStartDate(startDate);
        setEndDate(endDate);
        setTimeout(() => {
            refreshMarginNetTable();
        }, 100);
    };

    const fetchMarginResults = useCallback(async () => {
        if (!campaigns || campaigns.length === 0 || !startDate || !endDate) return;

        setIsDataLoaded(false);

        try {
            const allCampaignData = await Promise.all(campaigns.map(async ({ campaignId }) => {
                const response = await getMarginByCampaignId({ startDate, endDate, campaignId });
                // ✅ 데이터 껍데기 벗기기
                const realData = response?.data?.[0]?.data || [];

                return { campaignId, data: realData };
            }));

            setTableData(allCampaignData);

            setTimeout(() => {
                setIsDataLoaded(true);
            }, 300);

        } catch (error) {
            console.error("마진 결과 데이터 로딩 중 에러 발생:", error);
            setTableData([]);
            setIsDataLoaded(true);
        }
    }, [startDate, endDate, campaigns]);

    useEffect(() => {
        fetchMarginResults();
        refreshMarginNetTable();
    }, [fetchMarginResults, refreshMarginNetTable]);

    useEffect(() => {
        const initialExpandedIds = new Set(campaigns.map(campaign => campaign.campaignId));
        setExpandedCampaignId(initialExpandedIds);
    }, [campaigns]);

    const toggleExpandCampaign = (campaignId) => {
        setExpandedCampaignId(prev => {
            const newExpanded = new Set(prev);
            newExpanded.has(campaignId) ? newExpanded.delete(campaignId) : newExpanded.add(campaignId);
            return newExpanded;
        });
    };

    const handleOptionMarginClick = () => {
        setIsModalOpen(true);
    };

    // ✅ [신규 추가] 특정 캠페인 하나만 빠르게 새로고침하는 함수 (화면 깜빡임 없이 데이터 교체)
    const refreshOneCampaign = useCallback(async (targetCampaignId) => {
        try {
            // 1. 해당 캠페인만 API 호출
            const response = await getMarginByCampaignId({ startDate, endDate, campaignId: targetCampaignId });
            const newMarginList = response?.data?.[0]?.data || [];

            // 2. 전체 데이터 중 해당 캠페인만 쏙 바꿔치기 (Partial Update)
            // 이렇게 하면 자식 컴포넌트(MarginDataTable)의 props.data가 바뀌면서 즉시 화면이 갱신됨
            setTableData(prevTableData =>
                prevTableData.map(item =>
                    item.campaignId === targetCampaignId
                        ? { ...item, data: newMarginList }
                        : item
                )
            );

            // 3. 전체 집계표 갱신
            setTimeout(() => {
                refreshMarginNetTable();
            }, 100);

        } catch (error) {
            console.error(`캠페인(${targetCampaignId}) 새로고침 실패:`, error);
        }
    }, [startDate, endDate, refreshMarginNetTable]);


    const handleSave = async (campaignId) => {
        try {
            const changedData = modifiedData[campaignId] || {};

            const data = {
                campaignId: campaignId,
                data: Object.values(changedData).map(item => ({
                    id: item?.id,
                    mardate: item.marDate,
                    marTargetEfficiency: item.marTargetEfficiency,
                    marAdBudget: item.marAdBudget
                })),
            };

            if (data.data.length === 0) {
                alert("바뀐 데이터가 없습니다.");
                return;
            }

            // 1. 서버 저장
            await updateEfficiencyAndAdBudget(data);
            alert("저장되었습니다.");

            // ✅ 2. 저장 후 해당 캠페인 데이터만 조용히 갱신 (카드는 열린 상태 유지)
            await refreshOneCampaign(campaignId);

            // 3. 임시 데이터 초기화
            setModifiedData(prev => {
                const next = { ...prev };
                delete next[campaignId];
                return next;
            });

        } catch (error) {
            console.error("저장 중 오류 발생:", error);
            alert("저장 실패");
        }
    };

    useEffect(() => {
    }, [modifiedData]);

    const handleDataChange = useCallback((campaignId, newData) => {
        setModifiedData(prev => ({
            ...prev,
            [campaignId]: newData
        }));
    }, []);

    return (
        <div className="form-main-content">
            <div className="flex items-center justify-end mb-4">
                <button
                    className="add-button"
                    onClick={handleOptionMarginClick}
                >
                    기간별 원가 수정
                </button>
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
            </div>

            {isDataLoaded ? (
                <MarginNetTable
                    ref={marginNetTableRef}
                    startDate={startDate}
                    endDate={endDate}
                />
            ) : (
                <div style={{
                    padding: '40px',
                    textAlign: 'center',
                    color: '#666',
                    backgroundColor: '#f9f9f9',
                    borderRadius: '8px',
                    margin: '20px 0'
                }}>
                    <div>데이터를 불러오는 중...</div>
                </div>
            )}

            <div className="campaign-list">
                {(campaigns || []).map((campaign) => (
                    <div
                        key={campaign.campaignId}
                        className={`campaign-card ${expandedCampaignId.has(campaign.campaignId) ? "expanded" : ""}`}
                    >
                        <div
                            className="campaign-header"
                            onClick={() => toggleExpandCampaign(campaign.campaignId)}
                        >
                            <h3>{campaign.title}</h3>

                            {/* ✅ [추가] 저장 버튼 복구 */}
                            <div className="button-container">
                                <button
                                    className="add-button"
                                    onClick={(e) => {
                                        e.stopPropagation();
                                        handleSave(campaign.campaignId);
                                    }}>
                                    목표효율/예산 저장
                                </button>
                            </div>
                        </div>

                        {expandedCampaignId.has(campaign.campaignId) && (
                            <MarginDataTable
                                data={tableData.find(item => item.campaignId === campaign.campaignId)?.data || []}
                                startDate={startDate}
                                endDate={endDate}
                                campaignId={campaign.campaignId}
                                onDataChange={handleDataChange}
                                onSave={handleSave}

                                // ✅ [핵심] 빈 셀 클릭 시 -> 이 함수가 실행되어 -> 데이터만 교체됨 (카드 안 닫힘)
                                onRefresh={() => refreshOneCampaign(campaign.campaignId)}
                            />
                        )}
                    </div>
                ))}
            </div>

            <MarginResultModal
                isOpen={isModalOpen}
                onClose={() => setIsModalOpen(false)}
            />
        </div>
    );
};

export default MarginCalculatorResult;