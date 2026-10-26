import React, { useEffect } from "react";
import "../styles/KeywordComponent.css"; // 스타일 파일
import SortableHeader from './SortableHeader'; // SortableHeader 임포트

// ✨ 1. 스켈레톤 UI를 위한 예시 데이터 생성
const createPlaceholderData = (count = 5) => {
    return Array.from({ length: count }, (_, i) => ({
        exclusionKeyword: `제외 키워드 예시 ${i + 1}`,
        addTime: 'YYYY-MM-DD',
        isPlaceholder: true,
    }));
};
const placeholderData = createPlaceholderData(5);


const ExclusionKeywordComponent = ({
    keywords,
    selectedKeywords,
    setSelectedKeywords,
    isAllSelected,
    setIsAllSelected,
    sortConfig,
    setSortConfig,
    loading,
    error,
}) => {
    // useEffect는 부모에서 데이터를 내려주므로 특별한 로직이 없다면 비워두거나 제거해도 좋아.
    useEffect(() => {
    }, []);

    const filteredKeywords = keywords
        // .filter((item) => item.exclusionKeyword.includes("")) // 이 부분은 검색 기능 구현 시 사용될 것으로 보임
        .sort((a, b) => {
            if (!sortConfig.key) return 0;
            const order = sortConfig.direction === "asc" ? 1 : -1;
            const aValue = a[sortConfig.key];
            const bValue = b[sortConfig.key];
            if (aValue < bValue) return -1 * order;
            if (aValue > bValue) return 1 * order;
            return 0;
        });

    const handleSort = (key) => {
        let direction = "asc";
        if (sortConfig.key === key && sortConfig.direction === "asc") {
            direction = "desc";
        }
        setSortConfig({ key, direction });
    };

    const handleCheckboxChange = (keyword) => {
        setSelectedKeywords((prev) =>
            prev.includes(keyword)
                ? prev.filter((item) => item !== keyword)
                : [...prev, keyword]
        );
    };

    const handleSelectAll = () => {
        if (isAllSelected) {
            setSelectedKeywords([]);
        } else {
            setSelectedKeywords(filteredKeywords.map((item) => item.exclusionKeyword));
        }
        setIsAllSelected(!isAllSelected);
    };

    if (loading) return <div>Loading...</div>;
    if (error) return <div>{error}</div>;

    // ✨ 2. '데이터 없음' 메시지 처리를 tbody 안에서 하도록 변경 (이 if문은 삭제)
    /*
    if (filteredKeywords.length === 0) {
        return (
            <div className="keyword-table">
                <p className="no-data-message">등록된 제외 키워드가 없습니다!</p>
            </div>
        );
    }
    */

    return (
        <div className="keyword-table">
            <table>
                <thead>
                    <tr>
                        <SortableHeader label="제외 키워드" sortKey="exclusionKeyword" onSort={handleSort} />
                        <SortableHeader label="등록 날짜" sortKey="addTime" onSort={handleSort} />
                        <th>
                            <input
                                type="checkbox"
                                checked={filteredKeywords.length > 0 && isAllSelected && selectedKeywords.length === filteredKeywords.length}
                                onChange={handleSelectAll}
                            />
                        </th>
                    </tr>
                </thead>
                <tbody>
                    {/* ✨ 3. 데이터 유무에 따라 실제 데이터 또는 스켈레톤 UI를 렌더링 */}
                    {filteredKeywords.length > 0 ? (
                        filteredKeywords.map((keyword, index) => (
                            <tr key={index}>
                                <td>{keyword.exclusionKeyword}</td>
                                <td>{keyword.addTime}</td>
                                <td>
                                    <input
                                        type="checkbox"
                                        checked={selectedKeywords.includes(keyword.exclusionKeyword)}
                                        onChange={() => handleCheckboxChange(keyword.exclusionKeyword)}
                                    />
                                </td>
                            </tr>
                        ))
                    ) : (
                        // 데이터가 없을 때 보여줄 스켈레톤 UI
                        placeholderData.map((item, index) => (
                            <tr key={`placeholder-${index}`}>
                                <td>{item.exclusionKeyword}</td>
                                <td>{item.addTime}</td>
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

export default ExclusionKeywordComponent;