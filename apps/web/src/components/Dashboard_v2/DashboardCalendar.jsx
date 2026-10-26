import React, { useState, useEffect } from "react";
import DatePicker from "react-datepicker";
import "react-datepicker/dist/react-datepicker.css";
import { CalendarIcon } from 'lucide-react';
import "../../styles/Dashboard/DashboardCalendar.css";

const DashboardCalendar = ({ initialDate, onDateChange }) => {
    const [startDate, setStartDate] = useState(initialDate || new Date());
    const [isPickerOpen, setIsPickerOpen] = useState(false);

    // ✅ 외부 초기 날짜 변경 시 반영
    useEffect(() => {
        if (initialDate) {
            setStartDate(new Date(initialDate));
        }
    }, [initialDate]);

    const updateDate = (date) => {
        setStartDate(date);
        onDateChange({ startDate: date, endDate: date });
    };

    const handleDateChange = (date) => {
        updateDate(date);
        setIsPickerOpen(false);
    };

    const handlePrevDay = () => {
        const prevDate = new Date(startDate);
        prevDate.setDate(prevDate.getDate() - 1);
        updateDate(prevDate);
    };

    const handleNextDay = () => {
        const nextDate = new Date(startDate);
        nextDate.setDate(nextDate.getDate() + 1);
        updateDate(nextDate);
    };

    const getMonthAbbreviation = (date) =>
        date.toLocaleString('en-US', { month: 'short' }).toUpperCase();

    return (
        <div className="dashboard-card dashboard-calendar">
            <div className="calendar-header">
                <div className="calendar-header-inline">
                    <span className="calendar-month">{getMonthAbbreviation(startDate)}</span>
                    <button className="calendar-picker-button" onClick={() => setIsPickerOpen(open => !open)}>
                        <CalendarIcon size={20} />
                    </button>
                </div>
                <div className="calendar-controls">
                    <button className="calendar-button" onClick={handlePrevDay}>
                        &lt;
                    </button>
                    <span className="calendar-day">{startDate.getDate()}</span>
                    <button className="calendar-button" onClick={handleNextDay}>
                        &gt;
                    </button>
                </div>
                {isPickerOpen && (
                    <div className="calendar-popover">
                        <DatePicker
                            selected={startDate}
                            onChange={handleDateChange}
                            inline
                            dropdownMode="select"
                        />
                    </div>
                )}
            </div>
        </div>
    );
};

export default DashboardCalendar;
