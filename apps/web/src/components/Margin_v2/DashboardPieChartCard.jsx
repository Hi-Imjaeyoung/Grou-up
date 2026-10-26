import React from 'react';
import { PieChart, Pie, Cell, Tooltip, ResponsiveContainer } from 'recharts';

// 파이 차트 라벨을 그리기 위한 헬퍼 함수
const RADIAN = Math.PI / 180;
const renderCustomizedLabel = ({ cx, cy, midAngle, innerRadius, outerRadius, percent }) => {
    if (percent < 0.04) return null; // 4% 미만 조각은 라벨 숨김
    const radius = innerRadius + (outerRadius - innerRadius) * 0.5;
    const x = cx + radius * Math.cos(-midAngle * RADIAN);
    const y = cy + radius * Math.sin(-midAngle * RADIAN);
    return (
        <text x={x} y={y} fill="#374151" textAnchor="middle" dominantBaseline="central" fontSize={12} fontWeight="bold">
            {`${(percent * 100).toFixed(0)}%`}
        </text>
    );
};

const DashboardPieChartCard = ({ title, data, totalValue, onHover, formatCurrency, COLORS, PieTooltip }) => {
    const totalLabel = title === '광고 매출' ? '총 매출' : '총 순이익';

    return (
        <div className="lg:col-span-4 bg-white rounded-2xl shadow-lg p-4 flex flex-col">
            <h3 className="text-lg font-bold text-gray-700 mb-2">{title}</h3>
            <div className="flex-grow w-full h-56 md:h-64 relative" onMouseLeave={() => onHover(null)}>
                <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 text-center pointer-events-none">
                    <span className="text-xs text-gray-500">{totalLabel}</span>
                    <p className="text-xl font-bold text-gray-800">{formatCurrency(totalValue)}</p>
                </div>
                <ResponsiveContainer width="100%" height="100%">
                    <PieChart>
                        <Pie
                            data={data}
                            cx="50%"
                            cy="50%"
                            innerRadius="65%"
                            outerRadius="100%"
                            dataKey="value"
                            paddingAngle={2}
                            onMouseEnter={(d) => onHover(d.name)}
                            labelLine={false}
                            label={renderCustomizedLabel}
                        >
                            {data.map((entry, index) => (<Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />))}
                        </Pie>
                        <Tooltip content={<PieTooltip />} />
                    </PieChart>
                </ResponsiveContainer>
            </div>
            <div className="mt-2 text-xs text-gray-600 grid grid-cols-2 gap-x-4 gap-y-1">
                {data.map((entry, index) => (
                    <div key={`legend-${entry.name}-${index}`} className="flex items-center">
                        <div className="w-2.5 h-2.5 rounded-sm mr-2" style={{ backgroundColor: COLORS[index % COLORS.length] }}></div>
                        <span className="truncate mr-1">{entry.name}:</span>
                        <span className="font-semibold">
                            {totalValue > 0 ? `${((entry.value / totalValue) * 100).toFixed(1)}%` : '0.0%'}
                        </span>
                    </div>
                ))}
            </div>
        </div>
    );
};

export default DashboardPieChartCard;