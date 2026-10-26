// src/components/SunshineChargeHistory.js
import React from 'react';
import './SunshineChargeHistory.css'; // CSS 파일 임포트

const SunshineChargeHistory = () => {
    // 임시 데이터 (나중에 API나 props로 받을 수 있습니다)
    const chargeHistoryData = [
        { id: 1, count: '햇살 100개', amount: '200,000', date: '25.05.29', paymentAction: '내역보기' },
        { id: 2, count: '햇살 100개', amount: '200,000', date: '25.05.29', paymentAction: '내역보기' },
        { id: 3, count: '햇살 100개', amount: '200,000', date: '25.05.29', paymentAction: '내역보기' },
        { id: 4, count: '햇살 100개', amount: '200,000', date: '25.05.29', paymentAction: '내역보기' },
        { id: 5, count: '햇살 100개', amount: '200,000', date: '25.05.29', paymentAction: '내역보기' },
        { id: 6, count: '햇살 100개', amount: '200,000', date: '25.05.29', paymentAction: '내역보기' },
        { id: 7, count: '햇살 100개', amount: '200,000', date: '25.05.29', paymentAction: '내역보기' },
        { id: 7, count: '햇살 100개', amount: '200,000', date: '25.05.29', paymentAction: '내역보기' },
        { id: 7, count: '햇살 100개', amount: '200,000', date: '25.05.29', paymentAction: '내역보기' },
        { id: 7, count: '햇살 100개', amount: '200,000', date: '25.05.29', paymentAction: '내역보기' },
        { id: 7, count: '햇살 100개', amount: '200,000', date: '25.05.29', paymentAction: '내역보기' },

    ];

    return (
        <div className="sunshine-charge-history">
            <h2>햇살 충전 내역</h2>
            <table>
                <thead>
                    <tr>
                        <th>충전 갯수</th>
                        <th>충전 금액</th>
                        <th>충전 날짜</th>
                        <th>결제 내역</th>
                    </tr>
                </thead>
                <tbody>
                    {chargeHistoryData.map((item) => (
                        <tr key={item.id}>
                            <td>{item.count}</td>
                            <td>{item.amount}</td>
                            <td>{item.date}</td>
                            <td><button className="action-button">{item.paymentAction}</button></td>
                        </tr>
                    ))}
                </tbody>
            </table >
        </div>
    );
};

export default SunshineChargeHistory;