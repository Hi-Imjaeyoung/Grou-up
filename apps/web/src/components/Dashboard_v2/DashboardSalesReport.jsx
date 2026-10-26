import React, { useState, useMemo } from 'react';
import { BarChart4, BookText } from 'lucide-react';
import {
    Chart as ChartJS,
    CategoryScale,
    LinearScale,
    PointElement,
    LineElement,
    Tooltip,
    Legend
} from 'chart.js';
import { Line } from 'react-chartjs-2';
import '../../styles/Dashboard/DashboardSalesReport.css';

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, Tooltip, Legend);

const DashboardSalesReport = ({ tableData }) => {
    const [view, setView] = useState('chart'); // chart or table

    const chartData = useMemo(() => {
        const dailyMap = new Map();

        tableData.forEach(group => {
            group.data.forEach(item => {
                const date = item.marDate;
                const prev = dailyMap.get(date) || { marSales: 0, marAdCost: 0 };
                prev.marSales += item.marSales || 0;
                prev.marAdCost += item.marAdCost || 0;
                dailyMap.set(date, prev);
            });
        });

        return Array.from(dailyMap.entries()).map(([date, val]) => ({
            name: date,
            총매출: val.marSales,
            총광고비: val.marAdCost,
            ROAS: val.marAdCost ? Math.round((val.marSales / val.marAdCost) * 100) : 0,
        })).sort((a, b) => new Date(a.name) - new Date(b.name));
    }, [tableData]);

    const chartJsData = {
        labels: chartData.map(d => d.name),
        datasets: [
            {
                label: '총매출',
                data: chartData.map(d => d.총매출),
                borderColor: '#FF6384',
                backgroundColor: '#FF6384',
                yAxisID: 'y1',
                tension: 0.4
            },
            {
                label: '총광고비',
                data: chartData.map(d => d.총광고비),
                borderColor: '#36A2EB',
                backgroundColor: '#36A2EB',
                yAxisID: 'y1',
                tension: 0.4
            },
            {
                label: 'ROAS',
                data: chartData.map(d => d.ROAS),
                borderColor: '#000000',
                backgroundColor: '#000000',
                yAxisID: 'y2',
                tension: 0.4
            }
        ]
    };

    const chartJsOptions = {
        responsive: true,
        maintainAspectRatio: false,
        scales: {
            y1: {
                type: 'linear',
                position: 'left',
                ticks: {
                    callback: value => value.toLocaleString()
                }
            },
            y2: {
                type: 'linear',
                position: 'right',
                grid: {
                    drawOnChartArea: false
                },
                ticks: {
                    callback: value => `${value}%`
                }
            }
        },
        plugins: {
            tooltip: {
                callbacks: {
                    label: function (context) {
                        const label = context.dataset.label || '';
                        const value = context.parsed.y;
                        return `${label}: ${value.toLocaleString()}`;
                    }
                }
            }
        }
    };

    return (
        <div className="dashboard-card dashboard-sales-report">
            <div className="card-label-row">
                <div className="card-label">종합 보고서</div>
                <div className="chart-toggle-switch">
                    <button
                        className={`chart-toggle-button ${view === 'chart' ? 'active' : ''}`}
                        onClick={() => setView('chart')}
                    >
                        <BarChart4 size={15} />
                    </button>
                    <button
                        className={`chart-toggle-button ${view === 'table' ? 'active' : ''}`}
                        onClick={() => setView('table')}
                    >
                        <BookText size={15} />
                    </button>
                </div>
            </div>
            {view === 'chart' ? (
                <div className="chart-container" style={{ marginBottom: '24px' }}>
                    <Line data={chartJsData} options={chartJsOptions} height={300} />
                </div>
            ) : (
                <div className="sales-table-wrapper">
                    <table className="sales-table">
                        <thead>
                            <tr>
                                <th>날짜</th>
                                <th>총 매출</th>
                                <th>총 광고비</th>
                                <th>ROAS</th>
                            </tr>
                        </thead>
                        <tbody>
                            {chartData.map((row, i) => (
                                <tr key={i}>
                                    <td>{row.name}</td>
                                    <td>{row.총매출.toLocaleString()}</td>
                                    <td>{row.총광고비.toLocaleString()}</td>
                                    <td>{row.ROAS.toFixed(0)}%</td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            )}
        </div>
    );
};

export default DashboardSalesReport;
