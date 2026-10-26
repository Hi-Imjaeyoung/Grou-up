import React, { useState, useEffect } from "react";
import { getCampaignDetails } from "../services/capaigndetails";
import { formatNumber } from "../utils/formatUtils"; // 유틸리티 함수 가져오기
import "../styles/Table.css";
import "../styles/KeyTotal.css";
import SortableHeader from "../components/SortableHeader";

// ✨ 1. 스켈레톤 UI를 위한 예시 데이터 생성
const createPlaceholderData = (count = 5) => {
    return Array.from({ length: count }, (_, i) => ({
        id: `0000${i}`,
        name: '상품 옵션 예시',
        copSales: 0,
        copAdcost: 0,
        copAdsales: 0,
        copRoas: 0,
        copImpressions: 0,
        copClicks: 0,
        copClickRate: 0,
        copCvr: 0,
        isPlaceholder: true,
    }));
};
const placeholderData = createPlaceholderData(5);

const CampaignOptionDetailsComponent = ({ campaignId, startDate, endDate }) => {
    const [sortConfig, setSortConfig] = useState({ key: null, direction: "asc" });
    const [campaignDetails, setCampaignDetails] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        const fetchCampaignDetails = async () => {
            if (!startDate || !endDate) {
                setError("시작 날짜와 종료 날짜를 설정해주세요.");
                setLoading(false); // 로딩 상태 종료
                return;
            }

            setLoading(true);
            try {
                const response = await getCampaignDetails({
                    start: startDate,
                    end: endDate,
                    campaignId
                });
                setCampaignDetails(response.data || []);
                setError(null);
            } catch (error) {
                setError("데이터를 불러오는 중 오류가 발생했습니다.");
                setCampaignDetails([]); // 에러 발생 시 데이터 비우기
                console.error(error);
            } finally {
                setLoading(false);
            }
        };

        fetchCampaignDetails();
    }, [campaignId, startDate, endDate]);

    const handleSort = (key) => {
        let direction = "asc";
        if (sortConfig.key === key && sortConfig.direction === "asc") {
            direction = "desc";
        }
        setSortConfig({ key, direction });
    };

    // ✨ 2. 정렬 로직을 렌더링 직전에 처리하도록 변경 (메모이제이션을 사용하면 더 좋지만 일단 이렇게)
    const sortedCampaignDetails = [...campaignDetails].sort((a, b) => {
        if (!sortConfig.key) return 0;
        const order = sortConfig.direction === "asc" ? 1 : -1;
        const aValue = a[sortConfig.key];
        const bValue = b[sortConfig.key];

        // 숫자와 문자열 타입 모두 처리할 수 있는 범용적인 정렬
        if (aValue < bValue) return -1 * order;
        if (aValue > bValue) return 1 * order;
        return 0;
    });

    // ✨ 3. 에러가 있을 경우 에러 메시지를 먼저 보여줌
    if (error) return <div>{error}</div>;

    return (
        <div className="keyword-content">
            <table>
                <thead>
                    <tr>
                        <SortableHeader label="옵션ID" sortKey="id" onSort={handleSort} />
                        <SortableHeader label="옵션명" sortKey="name" onSort={handleSort} />
                        <SortableHeader label="주문수" sortKey="copSales" onSort={handleSort} />
                        <SortableHeader label="광고비" sortKey="copAdcost" onSort={handleSort} />
                        <SortableHeader label="광고매출" sortKey="copAdsales" onSort={handleSort} />
                        <SortableHeader label="ROAS" sortKey="copRoas" onSort={handleSort} />
                        <SortableHeader label="노출" sortKey="copImpressions" onSort={handleSort} /> {/* 오타 수정: copimpressionss -> copImpressions */}
                        <SortableHeader label="클릭수" sortKey="copClicks" onSort={handleSort} />
                        <SortableHeader label="클릭률" sortKey="copClickRate" onSort={handleSort} />
                        <SortableHeader label="전환율" sortKey="copCvr" onSort={handleSort} />
                    </tr>
                </thead>
                <tbody>
                    {/* ✨ 4. 로딩 중, 데이터 있음, 데이터 없음(로딩 완료 후) 세 가지 상태를 처리 */}
                    {loading ? (
                        // 로딩 중일 때: 스켈레톤 UI 렌더링
                        placeholderData.map((item, index) => (
                            <tr key={`placeholder-${index}`}>
                                <td>{item.id}</td>
                                <td>{item.name}</td>
                                <td>---</td>
                                <td>---</td>
                                <td>---</td>
                                <td>--%</td>
                                <td>---</td>
                                <td>---</td>
                                <td>--%</td>
                                <td>--%</td>
                            </tr>
                        ))
                    ) : sortedCampaignDetails.length > 0 ? (
                        // 로딩이 끝났고 데이터가 있을 때: 실제 데이터 렌더링
                        sortedCampaignDetails.map((item, index) => (
                            <tr key={index}>
                                <td>{item.id}</td>
                                <td>{item.name}</td>
                                <td>{formatNumber(item.copSales)}</td>
                                <td>{formatNumber(item.copAdcost)}</td>
                                <td>{formatNumber(item.copAdsales)}</td>
                                <td>{formatNumber(item.copRoas)}%</td>
                                <td>{formatNumber(item.copImpressions)}</td>
                                <td>{formatNumber(item.copClicks)}</td>
                                <td>{formatNumber(item.copClickRate)}%</td>
                                <td>{formatNumber(item.copCvr)}%</td>
                            </tr>
                        ))
                    ) : (
                        // 로딩이 끝났는데 데이터가 없을 때: '데이터 없음' 메시지 렌더링
                        placeholderData.map((item, index) => (
                            <tr key={`placeholder-${index}`}>
                                <td>{item.id}</td>
                                <td>{item.name}</td>
                                <td>---</td>
                                <td>---</td>
                                <td>---</td>
                                <td>--%</td>
                                <td>---</td>
                                <td>---</td>
                                <td>--%</td>
                                <td>--%</td>
                            </tr>
                        ))
                    )}
                </tbody>
            </table>
        </div>
    );
};

export default CampaignOptionDetailsComponent;