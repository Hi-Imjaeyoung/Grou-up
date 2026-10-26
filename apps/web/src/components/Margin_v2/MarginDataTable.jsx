// src/components/margin/MarginDataTable.jsx
import React from 'react';
import { useMarginData } from '../../hooks/useMarginData';
import MarginTablePresenter from './MarginTablePresenter';

// 1. props에 'data'를 추가합니다. (부모가 이미 가져온 데이터)
const MarginDataTable = ({ data: initialData, startDate, endDate, campaignId, onDataChange, onSave, onRefresh }) => {
    // 2. 훅에 initialData를 전달합니다.
    const {
        data,
        dateRange,
        handleInputChange,
        handleCellClick
    } = useMarginData(startDate, endDate, campaignId, onDataChange, initialData, onRefresh);

    return (
        <MarginTablePresenter
            data={data}
            dateRange={dateRange}
            handleInputChange={handleInputChange}
            handleCellClick={handleCellClick}
            campaignId={campaignId}
            handleSave={onSave}
        />
    );
};

export default MarginDataTable;