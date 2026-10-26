import { useState, useEffect } from "react";

const useMonthDates = () => {
    const [startDate, setStartDate] = useState("");
    const [endDate, setEndDate] = useState("");

    useEffect(() => {
        const today = new Date();
        const currentYear = today.getFullYear();
        const currentMonth = today.getMonth(); // 0-11 범위

        // 시작일과 종료일 계산
        const startOfMonth = new Date(currentYear, currentMonth, 1);
        const endOfMonth = new Date(currentYear, currentMonth + 1, 0); // 해당 월의 마지막 날
        // YYYY-MM-DD 형식으로 변환
        setStartDate(startOfMonth.toISOString().split("T")[0]);
        setEndDate(endOfMonth.toISOString().split("T")[0]);
    }, []);

    return { startDate, endDate };
};

export default useMonthDates;
