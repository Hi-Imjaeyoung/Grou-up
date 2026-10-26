import React from 'react';
import { Bar } from 'react-chartjs-2';
import { Chart as ChartJS, CategoryScale, LinearScale, BarElement, Title, Tooltip, Legend, LineElement } from 'chart.js';

// Chart.js 등록
ChartJS.register(CategoryScale, LinearScale, BarElement, Title, Tooltip, Legend, LineElement);

const StatGraph3 = ({ search, nonSearch, startDate, endDate }) => {
    // 날짜 배열 생성
    const dateLabels = [];
    const start = new Date(startDate);
    const end = new Date(endDate);

    // 날짜 추가
    for (let dt = start; dt <= end; dt.setDate(dt.getDate() + 1)) {
        dateLabels.push(dt.toISOString().split('T')[0]); // 'YYYY-MM-DD' 형식으로 변환
    }

    // search와 nonSearch 데이터를 각각의 배열로 변환
    const searchClicks = dateLabels.map(date => (search[date] ? search[date].clicks : 0));
    const searchSales = dateLabels.map(date => (search[date] ? search[date].totalSales : 0));
    const nonSearchClicks = dateLabels.map(date => (nonSearch[date] ? nonSearch[date].clicks : 0));
    const nonSearchSales = dateLabels.map(date => (nonSearch[date] ? nonSearch[date].totalSales : 0));
    const searchimpressionss = dateLabels.map(date => (search[date] ? search[date].impressions : 0));
    const nonSearchimpressionss = dateLabels.map(date => (nonSearch[date] ? nonSearch[date].impressions : 0));

    // CVR 데이터 생성
    const searchCvrData = dateLabels.map((date, index) => {
        const clicks = searchClicks[index];
        const sales = searchSales[index];
        return clicks > 0 ? (sales / clicks) * 100 : 0; // 퍼센트로 표현
    });
    const nonSearchCvrData = dateLabels.map((date, index) => {
        const clicks = nonSearchClicks[index];
        const sales = nonSearchSales[index];
        return clicks > 0 ? (sales / clicks) * 100 : 0; // 퍼센트로 표현
    });

    // 클릭률 데이터 생성
    const searchClickRate = dateLabels.map((date, index) => {
        const clicks = searchClicks[index];
        const impressionss = searchimpressionss[index];
        return impressionss > 0 ? (clicks / impressionss) * 100 : 0; // 퍼센트로 표현
    });
    const nonSearchClickRate = dateLabels.map((date, index) => {
        const clicks = nonSearchClicks[index];
        const impressionss = nonSearchimpressionss[index];
        return impressionss > 0 ? (clicks / impressionss) * 100 : 0; // 퍼센트로 표현
    });

    const data = {
        labels: dateLabels,
        datasets: [
            {
                label: '검색 전환율',
                data: searchCvrData,
                borderColor: '#d3264f', // 전환율 선 색상 (빨강)
                backgroundColor: 'rgba(255, 99, 132, 0.3)', // 전환율 색상 (옅은 빨강)
                type: 'line', // 선 그래프로 표시
                fill: false, // 선 그래프 안 채우기
                yAxisID: 'y', // 첫 번째 Y축
                order: 1,
            },
            {
                label: '비검색 전환율',
                data: nonSearchCvrData,
                borderColor: '#fdb665', // 비검색 전환율 선 색상 (짙은 파랑)
                backgroundColor: 'rgba(255, 165, 0, 0.3)', // 비검색 전환율 색상 (옅은 파랑)
                type: 'line', // 선 그래프로 표시
                fill: false,
                yAxisID: 'y', // 첫 번째 Y축
                order: 1,
            },
            {
                label: '검색 클릭률',
                data: searchClickRate,
                backgroundColor: '#8bc8b0', // 클릭률 색상 (옅은 초록)
                type: 'bar', // 선 그래프로 표시
                fill: false,
                yAxisID: 'y1', // 두 번째 Y축
                order: 2,
            },
            {
                label: '비검색 클릭률',
                data: nonSearchClickRate,
                backgroundColor: '#5c62b8', // 클릭률 색상 (옅은 주황)
                type: 'bar', // 선 그래프로 표시
                fill: false,
                yAxisID: 'y1', // 두 번째 Y축
                order: 2,
            },
        ],
    };

    // 옵션 설정
    const options = {
        responsive: true,
        maintainAspectRatio: false, // 비율 유지하지 않음
        plugins: {
            legend: {
                position: 'top',
            },
            title: {
                display: false,
                text: '전환율 및 클릭률 그래프',
            },
        },
        scales: {
            x: {
                title: {
                    display: false,
                    text: 'Date',
                },
            },
            y: {
                title: {
                    display: true,
                    text: '전환율 (%)',
                },
                ticks: {
                    beginAtZero: true,
                    max: 100, // CVR 최대값 설정
                },
                grid: {
                    display: false, // Y축의 그리드 선을 보이지 않게 설정
                },
            },
            y1: {
                position: 'right', // 두 번째 Y축 위치
                title: {
                    display: true,
                    text: '클릭률 (%)',
                },
                ticks: {
                    beginAtZero: true,
                    max: 100, // 클릭률 최대값 설정
                },
                grid: {
                    display: false, // 두 번째 Y축의 그리드 선을 보이지 않게 설정
                },
            },
        },
    };

    return (
        <div style={{ height: '100%' }}>
            <Bar data={data} options={{ ...options, maintainAspectRatio: false }} />
        </div>
    );
};

export default StatGraph3;
