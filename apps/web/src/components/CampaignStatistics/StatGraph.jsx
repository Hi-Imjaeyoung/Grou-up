import React, { useEffect, useRef } from 'react';
import { Bar } from 'react-chartjs-2';
import { Chart as ChartJS, CategoryScale, LinearScale, BarElement, Title, Tooltip, Legend, LineElement } from 'chart.js';

ChartJS.register(CategoryScale, LinearScale, BarElement, Title, Tooltip, Legend, LineElement);

const StatGraph = ({ search, nonSearch, memoData, startDate, endDate }) => {
    const chartRef = useRef(null);
    useEffect(() => {
        console.log("memos", memoData)

        const chart = chartRef.current;
        if (!chart) return;

        const handleMouseMove = (event) => {
            const canvas = chartRef.current?.canvas;
            if (!canvas) {
                console.warn("Canvas not found");
                return;
            }

            const rect = canvas.getBoundingClientRect();  // ⭐ 매번 새로 계산
            const mouseX = event.clientX - rect.left;
            const mouseY = event.clientY - rect.top;

            const xAxis = chart.scales.x;
            const xAxisArea = {
                top: chart.chartArea.bottom + 5,
                bottom: chart.height,
                left: chart.chartArea.left,
                right: chart.chartArea.right
            };

            if (
                mouseY >= xAxisArea.top &&
                mouseY <= xAxisArea.bottom &&
                mouseX >= xAxisArea.left &&
                mouseX <= xAxisArea.right
            ) {
                let minDist = Infinity;
                let closestLabel = null;

                for (let i = 0; i < chart.data.labels.length; i++) {
                    const labelX = xAxis.getPixelForValue(i);
                    const dist = Math.abs(mouseX - labelX);
                    if (dist < minDist) {
                        minDist = dist;
                        closestLabel = chart.data.labels[i];
                    }
                }

                if (closestLabel && memoData?.[closestLabel]) {
                    let tooltipEl = document.getElementById('x-axis-tooltip');
                    if (!tooltipEl) {
                        tooltipEl = document.createElement('div');
                        tooltipEl.id = 'x-axis-tooltip';
                        tooltipEl.style.position = 'absolute';
                        tooltipEl.style.background = 'rgba(0, 0, 0, 0.8)';
                        tooltipEl.style.color = 'white';
                        tooltipEl.style.padding = '8px 12px';
                        tooltipEl.style.borderRadius = '6px';
                        tooltipEl.style.pointerEvents = 'none';
                        tooltipEl.style.whiteSpace = 'pre-line';
                        tooltipEl.style.fontSize = '12px';
                        tooltipEl.style.zIndex = 1000;
                        document.body.appendChild(tooltipEl);
                    }

                    const content = Array.isArray(memoData[closestLabel])
                        ? memoData[closestLabel].map(m => `• ${m}`).join('\n')
                        : memoData[closestLabel];

                    tooltipEl.innerHTML = `<strong>${closestLabel}</strong><br>${content}`;
                    // 스크롤 위치를 고려하여 툴팁 위치 설정
                    tooltipEl.style.left = `${rect.left + mouseX + 10 + window.scrollX}px`;
                    tooltipEl.style.top = `${rect.top + mouseY - 40 + window.scrollY}px`;
                    tooltipEl.style.display = 'block';
                } else {
                    const tooltipEl = document.getElementById('x-axis-tooltip');
                    if (tooltipEl) tooltipEl.style.display = 'none';
                }
            } else {
                const tooltipEl = document.getElementById('x-axis-tooltip');
                if (tooltipEl) tooltipEl.style.display = 'none';
            }
        };

        const canvas = chartRef.current?.canvas;
        if (canvas) canvas.addEventListener('mousemove', handleMouseMove);

        return () => {
            if (canvas) canvas.removeEventListener('mousemove', handleMouseMove);
            const tooltipEl = document.getElementById('x-axis-tooltip');
            if (tooltipEl) tooltipEl.remove();
        };
    }, [memoData]);


    // 날짜를 배열로 생성
    const dateLabels = [];
    const start = new Date(startDate);
    const end = new Date(endDate);

    // 날짜를 추가
    for (let dt = start; dt <= end; dt.setDate(dt.getDate() + 1)) {
        dateLabels.push(dt.toISOString().split('T')[0]); // 'YYYY-MM-DD' 형식으로 변환
    }

    // search와 nonSearch 데이터를 각각의 배열로 변환
    const searchData = dateLabels.map(date => (search[date] ? search[date].adSales : 0));
    const nonSearchData = dateLabels.map(date => (nonSearch[date] ? nonSearch[date].adSales : 0));

    // 광고비 데이터 생성
    const searchAdCostData = dateLabels.map(date => (search[date] ? search[date].adCost : 0));
    const nonSearchAdCostData = dateLabels.map(date => (nonSearch[date] ? nonSearch[date].adCost : 0));
    const adCostData = dateLabels.map((date, index) => searchAdCostData[index] + nonSearchAdCostData[index]);

    // ROAS 데이터 생성
    const roasData = dateLabels.map((date, index) => {
        const totalRevenue = searchData[index] + nonSearchData[index];
        const totalCost = searchAdCostData[index] + nonSearchAdCostData[index];
        return totalCost > 0 ? (totalRevenue / totalCost) * 100 : 0;
    });

    const data = {
        labels: dateLabels,
        datasets: [
            {
                label: '검색',
                data: searchData,
                backgroundColor: 'rgba(54, 162, 235, 0.6)',
                stack: 'Stack 0',
                order: 3,
            },
            {
                label: '비검색',
                data: nonSearchData,
                backgroundColor: 'rgba(255, 159, 64, 0.6)',
                stack: 'Stack 0',
                order: 3,
            },
            {
                label: '광고비',
                data: adCostData,
                borderColor: 'rgb(0, 123, 255)',
                backgroundColor: 'rgba(0, 123, 255, 0.3)',
                type: 'line',
                fill: false,
                order: 1,
            },
            {
                label: 'ROAS',
                data: roasData,
                borderColor: 'rgb(255, 99, 132)',
                backgroundColor: 'rgba(255, 99, 132, 0.3)',
                type: 'line',
                fill: false,
                yAxisID: 'y-roas',
                order: 1,
            },
        ],
    };

    const options = {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
            legend: {
                position: 'top',
            },
            title: {
                display: false,
                text: '광고비 그래프',
            },
        },
        scales: {
            x: {
                title: {
                    display: false,
                    text: 'Date',
                },
                ticks: {
                    color: (context) => {
                        const label = context.tick.label;
                        return memoData && memoData[label] ? 'red' : 'black'; // memoData에 해당 날짜가 있으면 빨간색, 없으면 검은색
                    }
                },
            },
            y: {
                title: {
                    display: true,
                    text: '광고매출',
                },
                stacked: true,
                min: 0,
                max: Math.max(...adCostData, ...searchData, ...nonSearchData) * 1.5,
            },
            'y-roas': {
                title: {
                    display: true,
                    text: 'ROAS (%)',
                },
                position: 'right',
                ticks: {
                    beginAtZero: true,
                },
                grid: {
                    display: false,
                },
            },
        },
    };

    return (
        <div style={{ height: '100%' }}>
            <Bar
                ref={chartRef}
                data={data}
                options={{ ...options, maintainAspectRatio: false }} // ⭐ 여기에 다시 설정되어 있네.
            />
        </div>
    );
};

export default StatGraph;
