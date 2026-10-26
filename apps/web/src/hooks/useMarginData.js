import { useState, useEffect, useMemo, useCallback } from 'react';
import { createMarginTable } from "../services/margin";

// 파라미터에 initialData, onRefresh 확인
export const useMarginData = (startDate, endDate, campaignId, onDataChange, initialData, onRefresh) => {

    // 🚨 [수정 포인트 1] useState([]) 대신, initialData가 있으면 바로 넣고 시작합니다.
    // 이렇게 해야 카드를 다시 열었을 때 깜빡임 없이 데이터가 바로 보입니다.
    const [data, setData] = useState(initialData || []);

    const [modifiedData, setModifiedData] = useState({});

    const dateRange = useMemo(() => {
        const range = [];
        const currentDate = new Date(startDate);
        const end = new Date(endDate);
        while (currentDate <= end) {
            const yearMonthDay = currentDate.toISOString().split('T')[0];
            const month = String(currentDate.getMonth() + 1).padStart(2, '0');
            const day = String(currentDate.getDate()).padStart(2, '0');
            range.push({ fullDate: yearMonthDay, displayDate: `${month}-${day}` });
            currentDate.setDate(currentDate.getDate() + 1);
        }
        return range;
    }, [startDate, endDate]);

    // 🚨 [수정 포인트 2] 부모의 데이터가 나중에 로딩되거나 변경되었을 때를 대비해 동기화 유지
    useEffect(() => {
        if (initialData) {
            setData(initialData);
        }
    }, [initialData]);

    const handleInputChange = useCallback((e, fullDate, key) => {
        const newValue = e.target.value;
        setData(prevData =>
            prevData.map(item =>
                item.marDate === fullDate ? { ...item, [key]: Number(newValue) } : item
            )
        );
        setModifiedData(prev => {
            const itemForDate = data.find(item => item.marDate === fullDate) || {};
            const updatedData = {
                ...prev[fullDate],
                id: itemForDate.id,
                marDate: fullDate,
                marTargetEfficiency: itemForDate.marTargetEfficiency,
                marAdBudget: itemForDate.marAdBudget,
                [key]: Number(newValue)
            };
            return { ...prev, [fullDate]: updatedData };
        });
    }, [data]);

    const handleCellClick = useCallback(async (fullDate) => {
        try {
            await createMarginTable({ targetDate: fullDate, campaignId });
            console.log("데이터갱신")
            // 데이터 갱신 요청
            if (onRefresh) {
                onRefresh();
            }
        } catch (error) {
            console.error("셀 클릭 후 마진 테이블 생성 실패:", error);
        }
    }, [campaignId, onRefresh]);

    useEffect(() => {
        onDataChange(campaignId, modifiedData);
    }, [modifiedData, campaignId, onDataChange]);

    return {
        data,
        dateRange,
        handleInputChange,
        handleCellClick
    };
};