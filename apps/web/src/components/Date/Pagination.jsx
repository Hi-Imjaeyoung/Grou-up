import React, { useMemo } from "react";

const Pagination = ({ currentPage, totalPages, onPageChange }) => {
    const paginationRange = useMemo(() => {
        const range = [];
        const maxVisiblePages = 5; // 최대 표시 페이지 수
        const startPage = Math.max(1, currentPage - Math.floor(maxVisiblePages / 2));
        const endPage = Math.min(totalPages, startPage + maxVisiblePages - 1);

        for (let i = startPage; i <= endPage; i++) {
            range.push(i);
        }
        return range;
    }, [currentPage, totalPages]);

    return (
        <div className="pagination-controls">
            <button
                className="pagination-button"
                onClick={() => onPageChange(1)}
                disabled={currentPage === 1}
            >
                {"<<"}
            </button>
            <button
                className="pagination-button"
                onClick={() => onPageChange(currentPage - 1)}
                disabled={currentPage === 1}
            >
                {"<"}
            </button>
            {paginationRange.map((page) => (
                <button
                    key={page}
                    className={`pagination-button ${page === currentPage ? "active" : ""}`}
                    onClick={() => onPageChange(page)}
                >
                    {page}
                </button>
            ))}
            <button
                className="pagination-button"
                onClick={() => onPageChange(currentPage + 1)}
                disabled={currentPage === totalPages}
            >
                {">"}
            </button>
            <button
                className="pagination-button"
                onClick={() => onPageChange(totalPages)}
                disabled={currentPage === totalPages}
            >
                {">>"}
            </button>
        </div>
    );
};

export default Pagination;
