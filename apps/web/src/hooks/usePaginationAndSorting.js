import { useMemo, useState } from "react";

const usePaginationAndSorting = ({ data = [], itemsPerPage = 7 }) => {
    // 현재 페이지 상태
    const [page, setPage] = useState(1);

    // 정렬 상태 (key: 정렬할 필드, direction: asc/desc)
    const [sortConfig, setSortConfig] = useState(null);

    // 정렬된 데이터
    const sortedData = useMemo(() => {
        if (!sortConfig) return data;
        const sorted = [...data].sort((a, b) => {
            const aValue = a[sortConfig.key];
            const bValue = b[sortConfig.key];

            // 비교값이 문자열일 경우
            if (typeof aValue === "string" && typeof bValue === "string") {
                return sortConfig.direction === "asc"
                    ? aValue.localeCompare(bValue)
                    : bValue.localeCompare(aValue);
            }

            // 비교값이 숫자일 경우
            if (typeof aValue === "number" && typeof bValue === "number") {
                return sortConfig.direction === "asc"
                    ? aValue - bValue
                    : bValue - aValue;
            }

            return 0; // 비교값이 동일한 경우
        });
        return sorted;
    }, [data, sortConfig]);

    // 페이지네이션 데이터 계산
    const paginatedData = useMemo(() => {
        const startIndex = (page - 1) * itemsPerPage;
        const endIndex = startIndex + itemsPerPage;
        return sortedData.slice(startIndex, endIndex); // sortedData를 기반으로 페이지 데이터 계산
    }, [sortedData, page, itemsPerPage]);

    // 총 페이지 수 계산
    const totalPages = useMemo(() => Math.ceil(data.length / itemsPerPage), [data, itemsPerPage]);

    // 정렬 변경 함수
    const changeSort = (key) => {
        setSortConfig((prevConfig) => {
            if (prevConfig && prevConfig.key === key) {
                // 같은 키를 클릭하면 방향 변경
                return {
                    key,
                    direction: prevConfig.direction === "asc" ? "desc" : "asc",
                };
            }
            return { key, direction: "asc" }; // 기본 정렬은 오름차순
        });
    };

    // 페이지 변경 함수
    const changePage = (newPage) => {
        if (newPage < 1 || newPage > totalPages) return; // 범위를 벗어난 페이지 처리
        setPage(newPage);
    };

    return {
        paginatedData, // 현재 페이지 데이터
        totalPages, // 총 페이지 수
        page, // 현재 페이지 번호
        changePage, // 페이지 변경 함수
        changeSort, // 정렬 변경 함수
        sortConfig, // 현재 정렬 상태
    };
};

export default usePaginationAndSorting;
