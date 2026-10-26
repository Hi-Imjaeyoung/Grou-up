import React, { useState, useEffect, useCallback } from 'react';
import '../../styles/DateRangeCalendar.css';

// 날짜 객체를 'YYYY-MM-DD' 형식의 문자열로 변환하는 헬퍼 함수
// 이는 시간대 문제를 회피하고 로컬 날짜를 정확하게 표현합니다.
const formatDateToYYYYMMDD = (date) => {
    const year = date.getFullYear();
    const month = (date.getMonth() + 1).toString().padStart(2, '0'); // 월은 0부터 시작하므로 +1
    const day = date.getDate().toString().padStart(2, '0');
    return `${year}-${month}-${day}`;
};

const DateRangeCalendar = ({ onDateRangeChange, initialStartDate, initialEndDate, onClose }) => {
    // 현재 화면 기준 날짜(첫 달의 1일)
    const [currentDate, setCurrentDate] = useState(new Date());
    const [startDate, setStartDate] = useState(initialStartDate ? new Date(initialStartDate) : null);
    const [endDate, setEndDate] = useState(initialEndDate ? new Date(initialEndDate) : null);
    const [isSelectingEnd, setIsSelectingEnd] = useState(false); // 시작일 선택 후 끝일 선택 중인지 여부

    const today = new Date();
    today.setHours(0, 0, 0, 0); // 오늘 날짜의 시/분/초/밀리초를 0으로 통일하여 비교 용이하게 함

    // 퀵 옵션 정의 (이 부분은 기존과 동일)
    const quickOptions = [
        { label: '어제', value: 'yesterday' },
        { label: '최근 7일', value: 'last7days' },
        { label: '이번달', value: 'thisMonth' },
        { label: '최근 30일', value: 'last30days' },
        { label: '지난 주', value: 'lastWeek' },
        { label: '지난 달', value: 'lastMonth' },
    ];

    // 부모에게 날짜 범위 변경 알려주기 (useCallback을 사용하여 최적화)
    const notifyDateRangeChange = useCallback(() => {
        if (startDate && endDate && onDateRangeChange) {
            // 시간대 문제 해결을 위해 formatDateToYYYYMMDD 헬퍼 함수 사용
            onDateRangeChange({
                startDate: formatDateToYYYYMMDD(startDate),
                endDate: formatDateToYYYYMMDD(endDate),
            });
        }
    }, [startDate, endDate, onDateRangeChange]);

    useEffect(() => {
        notifyDateRangeChange();
    }, [startDate, endDate, notifyDateRangeChange]);


    // 한 달치 달력 데이터 생성 (6주×7일) - 기존과 동일
    const generateCalendar = date => {
        const year = date.getFullYear();
        const month = date.getMonth();
        const first = new Date(year, month, 1);
        const start = new Date(first);
        start.setDate(start.getDate() - first.getDay());

        const weeks = [];
        const cur = new Date(start);
        for (let w = 0; w < 6; w++) {
            const week = [];
            for (let d = 0; d < 7; d++) {
                week.push(new Date(cur));
                cur.setDate(cur.getDate() + 1);
            }
            weeks.push(week);
        }
        return weeks;
    };

    // 날짜 클릭 처리 - 핵심 수정 부분 (이전과 동일)
    const handleDateClick = date => {
        const clicked = new Date(date);
        clicked.setHours(0, 0, 0, 0); // 클릭된 날짜의 시/분/초/밀리초를 0으로 통일

        // 미래 날짜 선택 방지 (오늘 날짜까지만 선택 가능)
        // 현재 날짜를 기준으로 오늘 이후의 날짜는 선택할 수 없음.
        if (clicked > today) {
            console.log("미래 날짜는 선택할 수 없습니다.");
            return;
        }

        if (!startDate || (startDate && endDate)) {
            // 1. 시작일이 없거나, 시작일과 끝일이 모두 선택된 상태:
            //    새로운 시작일로 설정하고 끝일 선택 모드로 전환
            setStartDate(clicked);
            setEndDate(null);
            setIsSelectingEnd(true);
        } else if (startDate && !endDate) {
            // 2. 시작일은 있고 끝일이 없는 상태 (끝일 선택 중):
            let selectedEnd = clicked;

            // 2-1) 시작일보다 이전 날짜를 클릭한 경우:
            //      새로운 시작일로 재설정하고 끝일 선택 모드로 유지
            if (selectedEnd < startDate) {
                setStartDate(selectedEnd);
                setEndDate(null); // 끝일 초기화
                setIsSelectingEnd(true); // 끝일 선택 모드 유지
                return;
            }

            // 2-2) 시작일로부터 최대 30일 범위(시작일 포함) 제한
            const maxAllowedEndDate = new Date(startDate);
            maxAllowedEndDate.setDate(maxAllowedEndDate.getDate() + 29); // 시작일 포함 30일

            if (selectedEnd > maxAllowedEndDate) {
                // 선택된 끝일이 허용 범위를 초과하면, 최대 허용 끝일로 설정 (startDate는 변경하지 않음)
                selectedEnd = maxAllowedEndDate;
            }

            // 2-3) 선택된 끝일이 오늘을 초과하는 경우 (미래 날짜 선택 방지)
            if (selectedEnd > today) {
                selectedEnd = today;
            }

            // 모든 조건 만족 시 끝일 설정 및 선택 완료
            setEndDate(selectedEnd);
            setIsSelectingEnd(false);
        }
    };


    // 범위·시작·끝 날짜 검사 - 기존과 동일
    const isInRange = date => startDate && endDate && date >= startDate && date <= endDate;
    const isStartDate = date => startDate && date.toDateString() === startDate.toDateString();
    const isEndDate = date => endDate && date.toDateString() === endDate.toDateString();

    // 퀵 옵션 클릭 - 기존과 동일하게 작동하도록 조정 (오늘 날짜 설정도 동일하게 0시 0분 0초로 통일)
    const handleQuickSelect = option => {
        const tempToday = new Date();
        tempToday.setHours(0, 0, 0, 0); // 오늘 날짜 0시 0분 0초로 설정

        let start, end;
        switch (option) {
            case 'yesterday':
                start = new Date(tempToday); start.setDate(start.getDate() - 1);
                end = new Date(start);
                break;
            case 'last7days':
                start = new Date(tempToday); start.setDate(tempToday.getDate() - 6);
                end = new Date(tempToday);
                break;
            case 'thisMonth':
                start = new Date(tempToday.getFullYear(), tempToday.getMonth(), 1);
                end = new Date(tempToday);
                break;
            case 'last30days':
                start = new Date(tempToday); start.setDate(tempToday.getDate() - 29);
                end = new Date(tempToday);
                break;
            case 'lastWeek':
                start = new Date(tempToday); start.setDate(tempToday.getDate() - tempToday.getDay() - 7);
                end = new Date(start); end.setDate(end.getDate() + 6);
                break;
            case 'lastMonth':
                start = new Date(tempToday.getFullYear(), tempToday.getMonth() - 1, 1);
                end = new Date(tempToday.getFullYear(), tempToday.getMonth(), 0);
                break;
            default:
                return;
        }
        setStartDate(start);
        setEndDate(end);
        setIsSelectingEnd(false); // 퀵 옵션 선택은 항상 선택 완료 상태
    };

    // 전역(두 달치) 내비게이션
    const prevRange = () => {
        const newDate = new Date(currentDate.getFullYear(), currentDate.getMonth() - 1, 1);
        setCurrentDate(newDate);
    };
    const nextRange = () => {
        const newDate = new Date(currentDate.getFullYear(), currentDate.getMonth() + 1, 1);
        setCurrentDate(newDate);
    };

    // 보여줄 두 달
    const calendars = [
        new Date(currentDate.getFullYear(), currentDate.getMonth(), 1),
        new Date(currentDate.getFullYear(), currentDate.getMonth() + 1, 1),
    ];

    const monthNames = ['1월', '2월', '3월', '4월', '5월', '6월', '7월', '8월', '9월', '10월', '11월', '12월'];
    const dayNames = ['일', '월', '화', '수', '목', '금', '토'];

    return (
        <div className="date-range-calendar">
            {/* 두 달 달력 */}
            <div className="calendar-container">
                {calendars.map((viewDate, idx) => {
                    const weeks = generateCalendar(viewDate);
                    return (
                        <div className="calendar-content" key={idx}>
                            <h4 className="month-year">
                                {/* 이전 달 이동 버튼: 이제 양쪽 달력 모두에서 이전 달로 이동 가능 */}
                                <button
                                    onClick={prevRange}
                                    className="nav-button"
                                // disabled 제거 (이제 항상 활성화)
                                >
                                    ‹
                                </button>
                                {viewDate.getFullYear()}년 {monthNames[viewDate.getMonth()]}
                                {/* 다음 달 이동 버튼: 이제 양쪽 달력 모두에서 다음 달로 이동 가능 */}
                                <button
                                    onClick={nextRange}
                                    className="nav-button"
                                // disabled 제거 (이제 항상 활성화)
                                >
                                    ›
                                </button>
                            </h4>
                            <div className="calendar-grid">
                                <div className="weekdays">
                                    {dayNames.map(d => (
                                        <div key={d} className={`weekday ${d === '일' ? 'sunday' : ''}`}>{d}</div>
                                    ))}
                                </div>
                                <div className="dates">
                                    {weeks.map((week, wi) => (
                                        <div className="week" key={wi}>
                                            {week.map((day, di) => {
                                                const sameMonth = day.getMonth() === viewDate.getMonth();
                                                const isToday = day.toDateString() === today.toDateString();

                                                // 오늘을 넘어가는 날짜는 비활성화
                                                const isFutureDate = day > today;

                                                const classes = [
                                                    'date-cell',
                                                    !sameMonth && 'other-month',
                                                    isToday && 'today',
                                                    isStartDate(day) && 'start-date',
                                                    isEndDate(day) && 'end-date',
                                                    // 시작일 선택 중이고, 현재 날짜가 시작일 이후, 최대 허용 종료일 이전일 경우에도 in-range 클래스 적용
                                                    // 이는 사용자에게 선택 가능한 범위를 시각적으로 보여주는 데 도움이 됩니다.
                                                    isSelectingEnd && startDate && day >= startDate && day <= new Date(startDate.getFullYear(), startDate.getMonth(), startDate.getDate() + 29) && 'in-range-preview',
                                                    isInRange(day) && 'in-range', // 최종 선택된 범위
                                                    day.getDay() === 0 && 'sunday'
                                                ].filter(Boolean).join(' ');

                                                return (
                                                    <button
                                                        key={di}
                                                        className={classes}
                                                        onClick={() => handleDateClick(day)}
                                                        disabled={isFutureDate} // 미래 날짜는 선택 불가
                                                    >
                                                        {day.getDate()}
                                                    </button>
                                                );
                                            })}
                                        </div>
                                    ))}
                                </div>
                            </div>
                        </div>
                    );
                })}
            </div>

            {/* 퀵 옵션 */}
            <div className="quick-options">
                {quickOptions.map(opt => (
                    <button
                        key={opt.value}
                        className="quick-option-button"
                        onClick={() => handleQuickSelect(opt.value)}
                    >
                        {opt.label}
                    </button>
                ))}
            </div>

            {/* 액션 버튼 */}
            <div className="action-buttons">
                <button className="cancel-button" onClick={() => {
                    setStartDate(null);
                    setEndDate(null);
                    setIsSelectingEnd(false); // 취소 시 선택 모드도 초기화
                }}>취소</button>
                <button className="cancel-button" onClick={() => {
                    onClose && onClose();
                }}>닫기 </button>
            </div>
        </div>
    );
};

export default DateRangeCalendar;
