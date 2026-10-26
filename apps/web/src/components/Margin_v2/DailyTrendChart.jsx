
import React, { useMemo } from 'react';
import {
    ResponsiveContainer, ComposedChart, Bar, Line, XAxis, YAxis, CartesianGrid, Tooltip
} from 'recharts';

const DailyTrendChart = ({ chartApiData, title, formatCurrency }) => {
    // ✅ API 데이터를 차트 형식에 맞게 간단히 변환합니다. 불필요한 로직은 모두 제거되었습니다.
    const chartData = useMemo(() => {
        if (!chartApiData || chartApiData.length === 0) {
            return [];
        }
        // API의 데이터 키를 차트의 데이터 키로 이름만 변경해 줍니다.
        return chartApiData.map(item => ({
            date: item.marDate,
            revenue: item.marSales,
            netProfit: item.marNetProfit
        }));
    }, [chartApiData]); // API 데이터가 바뀔 때만 실행됩니다.

    return (
        <section className="bg-white rounded-2xl shadow-lg p-4 flex-grow flex flex-col">
            <div className="flex items-center justify-between mb-2">
                <h3 className="text-lg font-bold text-gray-700">{title}</h3>
                <div className="flex items-center gap-4 text-xs font-medium">
                    <div className="flex items-center gap-2"><div className="w-2.5 h-2.5 bg-[#57B9AA] rounded-sm"></div><span>매출</span></div>
                    <div className="flex items-center gap-2"><div className="w-3 h-1 bg-[#5E87E3] rounded-full"></div><span>순이익</span></div>
                </div>
            </div>
            <div className="w-full flex-grow">
                <ResponsiveContainer width="100%" height="100%">
                    {/* ✅ 가공된 chartData를 사용합니다. */}
                    <ComposedChart data={chartData} margin={{ top: 5, right: 20, left: -15, bottom: 5 }}>
                        <CartesianGrid strokeDasharray="3 3" vertical={false} />
                        <XAxis dataKey="date" tick={{ fontSize: 10 }} />
                        <YAxis tickFormatter={(value) => `${value / 1000}k`} tick={{ fontSize: 10 }} />
                        <Tooltip
                            formatter={(value, name) => [formatCurrency(value), name === 'revenue' ? '매출' : '순이익']}
                            labelFormatter={(label) => `날짜: ${label}`}
                            contentStyle={{ borderRadius: '0.5rem', boxShadow: '0 4px 6px rgba(0,0,0,0.1)' }}
                        />
                        <Bar dataKey="revenue" fill="#57B9AA" radius={[4, 4, 0, 0]} />
                        <Line type="monotone" dataKey="netProfit" stroke="#5E87E3" strokeWidth={3} dot={false} activeDot={{ r: 5 }} />
                    </ComposedChart>
                </ResponsiveContainer>
            </div>
        </section>
    );
};

export default DailyTrendChart;