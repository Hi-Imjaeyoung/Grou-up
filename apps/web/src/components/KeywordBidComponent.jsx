import React, { useState } from "react";
import "../styles/KeywordComponent.css"; // 스타일 파일
// import "../styles/Table.css";
import SortableHeader from '../components/SortableHeader';

const KeywordComponent = ({ selectedKeywords, setSelectedKeywords, keywords, loading, error }) => {
    const [editableBids, setEditableBids] = useState({}); // 각 키워드에 대한 입찰가 상태 관리
    const [sortConfig, setSortConfig] = useState({ key: 'keyKeyword', direction: 'asc' }); // 정렬 상태 관리
    // ✨ 1. 스켈레톤 UI를 위한 예시 데이터 생성 함수와 데이터
    const createPlaceholderData = (count = 3) => {
        return Array.from({ length: count }, (_, i) => ({
            keyKeyword: `데이터가 없습니다!`,
            impressions: 0,
            clicks: 0,
            clickRate: 0,
            totalSales: 0,
            cvr: 0,
            adSales: 0,
            adCost: 0,
            roas: 0,
            cpc: 0,
            bid: 0, // '설정 입찰가'를 위한 필드 추가
            isPlaceholder: true,
        }));
    };
    const placeholderData = createPlaceholderData(3);
    const filteredKeywords = keywords
        .sort((a, b) => {
            if (!sortConfig.key) return 0;
            const order = sortConfig.direction === "asc" ? 1 : -1;
            const aValue = a[sortConfig.key];
            const bValue = b[sortConfig.key];

            // 값이 숫자인 경우
            if (typeof aValue === 'number' && typeof bValue === 'number') {
                return (aValue - bValue) * order;
            }

            // 값이 문자열인 경우
            if (typeof aValue === 'string' && typeof bValue === 'string') {
                return aValue.localeCompare(bValue) * order;
            }

            return 0;
        });

    const handleSort = (key) => {
        let direction = "asc";
        if (sortConfig.key === key && sortConfig.direction === "asc") {
            direction = "desc";
        }
        setSortConfig({ key, direction });
    };

    const handleCheckboxChange = (item) => {
        const keywordData = {
            keyword: item.keyKeyword,
            bid: editableBids[item.keyKeyword] || item.cpc // 사용자가 입력한 값 또는 기본값 사용
        };

        setSelectedKeywords((prev) => {
            if (prev.some((kw) => kw.keyword === keywordData.keyword)) {
                return prev.filter((kw) => kw.keyword !== keywordData.keyword);
            } else {
                return [...prev, keywordData];
            }
        });
    };

    const handleBidChange = (keyword, value) => {
        setEditableBids((prev) => ({
            ...prev,
            [keyword]: value // 키워드에 대한 입찰가 업데이트
        }));
    };

    if (loading) return <div>Loading...</div>; // 로딩 상태 표시
    if (error) return <div>{error}</div>; // 에러 상태 표시
    return (
        <div className="keyword-table">
            <table>
                <thead>
                    <tr>
                        <SortableHeader label="키워드" sortKey="keyKeyword" onSort={handleSort} />
                        <SortableHeader label="노출" sortKey="impressions" onSort={handleSort} />
                        <SortableHeader label="클릭" sortKey="clicks" onSort={handleSort} />
                        <SortableHeader label="클릭률" sortKey="clickRate" onSort={handleSort} />
                        <SortableHeader label="주문" sortKey="totalSales" onSort={handleSort} />
                        <SortableHeader label="전환율" sortKey="cvr" onSort={handleSort} />
                        <SortableHeader label="광고매출" sortKey="adSales" onSort={handleSort} />
                        <SortableHeader label="광고비" sortKey="adCost" onSort={handleSort} />
                        <SortableHeader label="ROAS" sortKey="roas" onSort={handleSort} />
                        <SortableHeader label="CPC" sortKey="cpc" onSort={handleSort} />
                        <SortableHeader label="설정 입찰가" sortKey="bid" onSort={handleSort} />
                        <th>
                            <input
                                type="checkbox"
                                checked={filteredKeywords.length > 0 && selectedKeywords.length === filteredKeywords.length}
                                onChange={() => {
                                    if (selectedKeywords.length === filteredKeywords.length) {
                                        setSelectedKeywords([]);
                                    } else {
                                        const allKeywords = filteredKeywords.map(item => ({
                                            keyword: item.keyKeyword,
                                            bid: editableBids[item.keyKeyword] || item.cpc
                                        }));
                                        setSelectedKeywords(allKeywords);
                                    }
                                }}
                            />
                        </th>
                    </tr>
                </thead>
                <tbody>
                    {/* ✨ 3. 데이터 유무에 따라 실제 데이터 또는 스켈레톤 UI를 렌더링 */}
                    {filteredKeywords.length > 0 ? (
                        filteredKeywords.map((item, index) => (
                            <tr key={index}>
                                <td>{item.keyKeyword}</td>
                                <td>{item.impressions.toLocaleString()}</td>
                                <td>{item.clicks.toLocaleString()}</td>
                                <td>{item.clickRate.toLocaleString()}%</td>
                                <td>{item.totalSales.toLocaleString()}</td>
                                <td>{item.cvr.toLocaleString()}%</td>
                                <td>{item.adSales.toLocaleString()}원</td>
                                <td>{item.adCost.toLocaleString()}원</td>
                                <td>{item.roas.toLocaleString()}%</td>
                                <td>{item.cpc.toLocaleString()}원</td>
                                <td>
                                    <input
                                        type="number"
                                        style={{
                                            border: '1px solid #007bff',
                                            borderRadius: '4px',
                                            padding: '4px',
                                            width: '100%',
                                            boxShadow: '0 2px 4px rgba(0, 0, 0, 0.1)',
                                            transition: 'border-color 0.3s, box-shadow 0.3s',
                                        }}
                                        value={editableBids[item.keyKeyword] !== undefined ? editableBids[item.keyKeyword] : Math.round(item.cpc)}
                                        onChange={(e) => handleBidChange(item.keyKeyword, e.target.value)}
                                    />
                                </td>
                                <td>
                                    <input
                                        type="checkbox"
                                        checked={selectedKeywords.some(kw => kw.keyword === item.keyKeyword)}
                                        onChange={() => handleCheckboxChange(item)}
                                    />
                                </td>
                            </tr>
                        ))
                    ) : (
                        // 데이터가 없을 때 보여줄 스켈레톤 UI
                        placeholderData.map((item, index) => (
                            <tr key={`placeholder-${index}`}>
                                <td>{item.keyKeyword}</td>
                                <td>---</td>
                                <td>---</td>
                                <td>--%</td>
                                <td>---</td>
                                <td>--%</td>
                                <td>---원</td>
                                <td>---원</td>
                                <td>--%</td>
                                <td>---원</td>
                                <td>
                                    <input type="number" value={110} disabled style={{
                                        border: '1px solid #007bff',
                                        borderRadius: '4px',
                                        padding: '4px',
                                        width: '100%',
                                        boxShadow: '0 2px 4px rgba(0, 0, 0, 0.1)',
                                        transition: 'border-color 0.3s, box-shadow 0.3s',
                                    }} />
                                </td>
                                <td>
                                    <input type="checkbox" disabled />
                                </td>
                            </tr>
                        ))
                    )}
                </tbody>
            </table>
        </div>
    );
};

export default KeywordComponent;