import React, { useState, useRef, useEffect } from "react";
import "../styles/KeywordComponent.css";
import SortableHeader from '../components/SortableHeader';
import KeywordOptionModal from './KeywordOptionModal';

const KeywordComponent = ({ campaignId, startDate, endDate, selectedKeywords, setSelectedKeywords, keywords, loading, error }) => {
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [selectedKeyword, setSelectedKeyword] = useState(null);
    const [sortConfig, setSortConfig] = useState({ key: 'keyKeyword', direction: 'asc' });

    // --- 기능 추가를 위한 State ---
    const [isDragging, setIsDragging] = useState(false);
    const [dragStartIndex, setDragStartIndex] = useState(null);
    const [lastSelectedIndex, setLastSelectedIndex] = useState(null); // ✨ Shift 클릭 기준점 state
    const tbodyRef = useRef(null);
    // KeywordComponent 최상단에 추가
    const createPlaceholderData = (count = 3) => {
        return Array.from({ length: count }, (_, i) => ({
            keyKeyword: `데이터가 없습니다!`,
            impressions: 0,
            clicks: 0,
            clickRate: 0,
            totalSales: 0,
            cvr: 0,
            cpc: 0,
            adCost: 0,
            adSales: 0,
            roas: 0,
            // 실제 데이터에는 없는, 플레이스홀더임을 구분하기 위한 플래그
            isPlaceholder: true,
        }));
    };

    const placeholderData = createPlaceholderData(3);

    // --- 드래그 중 텍스트 선택 방지 및 커서 변경 Effect ---
    useEffect(() => {
        if (isDragging) {
            document.body.style.userSelect = 'none';
            document.body.style.cursor = 'grabbing';
        } else {
            document.body.style.userSelect = 'auto';
            document.body.style.cursor = 'auto';
        }
        return () => {
            document.body.style.userSelect = 'auto';
            document.body.style.cursor = 'auto';
        };
    }, [isDragging]);

    // --- 정렬된 키워드 목록 ---
    const filteredKeywords = keywords.sort((a, b) => {
        const aValue = a[sortConfig.key];
        const bValue = b[sortConfig.key];
        if (aValue < bValue) return sortConfig.direction === 'asc' ? -1 : 1;
        if (aValue > bValue) return sortConfig.direction === 'asc' ? 1 : -1;
        return 0;
    });

    const handleSort = (key) => {
        let direction = 'asc';
        if (sortConfig.key === key && sortConfig.direction === 'asc') {
            direction = 'desc';
        }
        setSortConfig({ key, direction });
    };

    // --- ✨ Shift 클릭 기능이 통합된 체크박스 핸들러 ---
    const handleCheckboxChange = (event, clickedItem, index) => {
        event.stopPropagation();

        // Shift 키가 눌렸고, 이전에 선택된 기준점이 있을 경우
        if (event.nativeEvent.shiftKey && lastSelectedIndex !== null) {
            const start = Math.min(lastSelectedIndex, index);
            const end = Math.max(lastSelectedIndex, index);

            const rangeItems = filteredKeywords.slice(start, end + 1);

            setSelectedKeywords(prevSelected => {
                // 현재 선택된 키워드 Set (빠른 조회를 위해)
                const prevSelectedKeywords = new Set(prevSelected.map(kw => kw.keyword));

                // 새로 추가될 아이템들
                const newItemsToAdd = rangeItems
                    .filter(item => !prevSelectedKeywords.has(item.keyKeyword))
                    .map(item => ({ keyword: item.keyKeyword, bid: item.cpc }));

                return [...prevSelected, ...newItemsToAdd];
            });
        } else {
            // 일반 클릭 (Shift 키가 안 눌린 경우)
            const keywordData = {
                keyword: clickedItem.keyKeyword,
                bid: clickedItem.cpc
            };

            setSelectedKeywords(prev => {
                if (prev.some(kw => kw.keyword === keywordData.keyword)) {
                    return prev.filter(kw => kw.keyword !== keywordData.keyword);
                } else {
                    return [...prev, keywordData];
                }
            });

            // 마지막 선택 인덱스를 현재 인덱스로 업데이트!
            setLastSelectedIndex(index);
        }
    };

    const handleSelectAll = () => {
        if (selectedKeywords.length === filteredKeywords.length) {
            setSelectedKeywords([]);
        } else {
            const allKeywords = filteredKeywords.map(item => ({
                keyword: item.keyKeyword,
                bid: item.cpc
            }));
            setSelectedKeywords(allKeywords);
        }
    };

    const handleRowClick = async (item) => {
        setSelectedKeyword(item);
        if (item.totalSales == 0) {
            alert("판매 데이터가 없어요!")
        } else {
            setIsModalOpen(true);
        }
    };

    const closeModal = () => {
        setIsModalOpen(false);
        setSelectedKeyword(null);
    };

    // --- 드래그 선택 기능 관련 핸들러들 ---
    const handleMouseDown = (e, index) => {
        if (e.target.type === 'checkbox' || e.target.tagName === 'BUTTON' || e.nativeEvent.shiftKey) {
            return;
        }
        setIsDragging(true);
        setDragStartIndex(index);

        // 드래그 시작 시 해당 행의 체크박스 상태를 토글
        const item = filteredKeywords[index];
        const keywordData = { keyword: item.keyKeyword, bid: item.cpc };
        setSelectedKeywords(prev => {
            if (prev.some(kw => kw.keyword === keywordData.keyword)) {
                return prev.filter(kw => kw.keyword !== keywordData.keyword);
            } else {
                return [...prev, keywordData];
            }
        });
        // 드래그 시작 시점도 마지막 클릭으로 간주
        setLastSelectedIndex(index);
    };

    const handleMouseMove = (e, index) => {
        if (!isDragging || dragStartIndex === null) return;

        const start = Math.min(dragStartIndex, index);
        const end = Math.max(dragStartIndex, index);

        const rangeItems = filteredKeywords.slice(start, end + 1);

        setSelectedKeywords(prevSelected => {
            const startItemKeyword = filteredKeywords[dragStartIndex].keyKeyword;
            const wasInitiallySelected = prevSelected.some(kw => kw.keyword === startItemKeyword);

            let newSelected = [...prevSelected];
            const newSelectedKeywords = new Set(newSelected.map(kw => kw.keyword));

            if (wasInitiallySelected) {
                // 드래그 시작점이 선택된 상태였다면, 범위 내 아이템들을 모두 선택
                rangeItems.forEach(item => {
                    if (!newSelectedKeywords.has(item.keyKeyword)) {
                        newSelected.push({ keyword: item.keyKeyword, bid: item.cpc });
                    }
                });
            } else {
                // 드래그 시작점이 선택 해제된 상태였다면, 범위 내 아이템들을 모두 선택 해제
                const rangeKeywordsSet = new Set(rangeItems.map(item => item.keyKeyword));
                newSelected = newSelected.filter(kw => !rangeKeywordsSet.has(kw.keyword));
            }
            return newSelected;
        });
    };

    const handleMouseUp = () => {
        setIsDragging(false);
        setDragStartIndex(null);
    };

    const handleMouseLeave = () => {
        if (isDragging) {
            setIsDragging(false);
            setDragStartIndex(null);
        }
    };

    if (loading) return <div>Loading...</div>;
    if (error) return <div>{error}</div>;

    return (
        <div className="keyword-table" onMouseLeave={handleMouseLeave}>
            <table>
                <thead>
                    <tr>
                        <SortableHeader label="키워드" sortKey="keyKeyword" onSort={handleSort} />
                        <SortableHeader label="노출" sortKey="impressions" onSort={handleSort} />
                        <SortableHeader label="클릭" sortKey="clicks" onSort={handleSort} />
                        <SortableHeader label="클릭률" sortKey="clickRate" onSort={handleSort} />
                        <SortableHeader label="주문" sortKey="totalSales" onSort={handleSort} />
                        <SortableHeader label="전환율" sortKey="cvr" onSort={handleSort} />
                        <SortableHeader label="CPC" sortKey="cpc" onSort={handleSort} />
                        <SortableHeader label="광고비" sortKey="adCost" onSort={handleSort} />
                        <SortableHeader label="광고매출" sortKey="adSales" onSort={handleSort} />
                        <SortableHeader label="ROAS" sortKey="roas" onSort={handleSort} />
                        <th>
                            <input
                                type="checkbox"
                                checked={filteredKeywords.length > 0 && selectedKeywords.length === filteredKeywords.length}
                                onChange={handleSelectAll}
                            />
                        </th>
                    </tr>
                </thead>
                <tbody ref={tbodyRef} onMouseUp={handleMouseUp}>
                    {filteredKeywords.length > 0 ? (
                        filteredKeywords.map((item, index) => (
                            <tr
                                key={item.keyKeyword} // key는 고유한 값으로 사용하는 것이 좋음
                                className={isDragging && dragStartIndex !== null && (index >= Math.min(dragStartIndex, index) && index <= Math.max(dragStartIndex, index)) ? 'dragging-highlight' : ''}
                                onMouseDown={(e) => handleMouseDown(e, index)}
                                onMouseMove={(e) => handleMouseMove(e, index)}
                            >
                                <td style={{ color: item.keyExcludeFlag && item.keyKeyword !== '-' ? '#d3264f' : 'inherit' }}>
                                    {item.keyKeyword === '-' ? (
                                        // 조건이 참일 때: "비검색" 텍스트를 회색으로 표시
                                        <span style={{ color: '#888' }}>비검색</span>
                                    ) : (
                                        // 조건이 거짓일 때: 기존 내용을 그대로 표시
                                        <>
                                            {item.keyKeyword}
                                            {item.totalSales >= 1 && <button
                                                className="icon-button"
                                                onClick={(e) => {
                                                    e.stopPropagation();
                                                    handleRowClick(item);
                                                }}
                                                aria-label="Search"
                                            >
                                                🔍
                                            </button>}
                                            {item.keyBidFlag && <span className="badge">수동</span>}
                                        </>
                                    )}
                                </td>
                                <td style={{ color: item.keyExcludeFlag ? '#d3264f' : 'inherit' }}>
                                    {item.impressions.toLocaleString()}
                                </td>
                                <td style={{ color: item.keyExcludeFlag ? '#d3264f' : 'inherit' }}>
                                    {item.clicks.toLocaleString()}
                                </td>
                                <td style={{ color: item.keyExcludeFlag ? '#d3264f' : 'inherit' }}>
                                    {item.clickRate.toLocaleString()}%
                                </td>
                                <td style={{ color: item.keyExcludeFlag ? '#d3264f' : 'inherit' }}>
                                    {item.totalSales.toLocaleString()}
                                </td>
                                <td style={{ color: item.keyExcludeFlag ? '#d3264f' : 'inherit' }}>
                                    {item.cvr.toLocaleString()}%
                                </td>
                                <td style={{ color: item.keyExcludeFlag ? '#d3264f' : 'inherit' }}>
                                    {item.cpc.toLocaleString()}원
                                </td>
                                <td style={{ color: item.keyExcludeFlag ? '#d3264f' : 'inherit' }}>
                                    {item.adCost.toLocaleString()}원
                                </td>
                                <td style={{ color: item.keyExcludeFlag ? '#d3264f' : 'inherit' }}>
                                    {item.adSales.toLocaleString()}원
                                </td>
                                <td style={{ color: item.keyExcludeFlag ? '#d3264f' : 'inherit' }}>
                                    {item.roas.toLocaleString()}%
                                </td>
                                <td>
                                    <input
                                        type="checkbox"
                                        checked={selectedKeywords.some(kw => kw.keyword === item.keyKeyword)}
                                        // ✨ index를 인자로 전달하도록 수정!
                                        onChange={(e) => handleCheckboxChange(e, item, index)}
                                        onClick={(e) => e.stopPropagation()}
                                    />
                                </td>
                            </tr>
                        ))
                    ) : (
                        placeholderData.map((item, index) => (
                            <tr key={`placeholder-${index}`} className="placeholder-row">
                                <td><span className="placeholder-text">{item.keyKeyword}</span></td>
                                <td><span className="placeholder-text">---</span></td>
                                <td><span className="placeholder-text">---</span></td>
                                <td><span className="placeholder-text">--%</span></td>
                                <td><span className="placeholder-text">---</span></td>
                                <td><span className="placeholder-text">--%</span></td>
                                <td><span className="placeholder-text">---원</span></td>
                                <td><span className="placeholder-text">---원</span></td>
                                <td><span className="placeholder-text">---원</span></td>
                                <td><span className="placeholder-text">--%</span></td>
                                <td>
                                    <input type="checkbox" disabled />
                                </td>
                            </tr>
                        ))
                    )}
                </tbody>
            </table>

            {isModalOpen && selectedKeyword && (
                <KeywordOptionModal
                    onClose={closeModal}
                    salesOptions={selectedKeyword.keySalesOptions}
                    startDate={startDate}
                    endDate={endDate}
                />
            )}
        </div>
    );
};

export default KeywordComponent;