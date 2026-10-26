import React from "react";
import DatePicker from "react-datepicker";
import { ArrowRight, ArrowLeft } from "lucide-react";

const DateControls = ({ date, onPrevDay, onNextDay, onDateChange }) => {
    return (
        <div className="date-controls">
            <button className="date-button" onClick={onPrevDay}>
                <ArrowLeft />
            </button>
            <DatePicker
                selected={date}
                onChange={onDateChange}
                dateFormat="yyyy-MM-dd"
                className="date-picker"
            />
            <button className="date-button" onClick={onNextDay}>
                <ArrowRight />
            </button>
        </div>
    );
};

export default DateControls;