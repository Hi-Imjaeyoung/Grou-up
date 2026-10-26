// src/components/SunshineUsageHistory.js
import React from 'react';
import './SunshineUsageHistory.css'; // CSS 파일 임포트

const SunshineUsageHistory = () => {
    // 임시 데이터
    const usageHistoryData = [
        { id: 1, campaignName: '반팔티', startDate: '25.05.29', duration: '~25.05.29', usedSunshine: 325, action: '연장하기' },
        { id: 2, campaignName: '반팔티', startDate: '25.05.29', duration: '~25.05.29', usedSunshine: 325, action: '연장하기' },
        { id: 3, campaignName: '반팔티', startDate: '25.05.29', duration: '~25.05.29', usedSunshine: 325, action: '연장하기' },
        { id: 4, campaignName: '반팔티', startDate: '25.05.29', duration: '~25.05.29', usedSunshine: 325, action: '연장하기' },
        { id: 5, campaignName: '반팔티', startDate: '25.05.29', duration: '~25.05.29', usedSunshine: 325, action: '연장하기' },
        { id: 6, campaignName: '반팔티', startDate: '25.05.29', duration: '~25.05.29', usedSunshine: 325, action: '연장하기' },
        { id: 7, campaignName: '반팔티', startDate: '25.05.29', duration: '~25.05.29', usedSunshine: 325, action: '연장하기' },
    ];

    return (
        <div className="sunshine-usage-history">
            <h2>햇살 이용 내역</h2>
            <table>
                <thead>
                    <tr>
                        <th>캠페인 이름</th>
                        <th>이용 시작 날짜</th>
                        <th>이용 기간</th>
                        <th>사용 햇살 갯수</th>
                        <th>연장하기</th>
                    </tr>
                </thead>
                <tbody>
                    {usageHistoryData.map((item) => (
                        <tr key={item.id}>
                            <td className="campaign-name">{item.campaignName} <span className="external-link-icon">❐</span></td>
                            <td>{item.startDate}</td>
                            <td>{item.duration}</td>
                            <td>{item.usedSunshine}</td>
                            <td><button className="action-button">{item.action}</button></td>
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    );
};

export default SunshineUsageHistory;