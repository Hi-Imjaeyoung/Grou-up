import React from 'react';
import '../styles/KeywordOptionModal.css'; // 스타일 파일

const KeywordOptionModal = ({ startDate, endDate, onClose, salesOptions, children }) => {
    // salesOptions 객체를 배열로 변환
    const salesArray = Object.entries(salesOptions).map(([name, quantity]) => ({
        name,
        quantity
    }));

    return (
        <div className="modal-overlay">
            <div className="modal-content">
                <button className="modal-close" onClick={onClose}>
                    &times; {/* 닫기 버튼 */}
                </button>
                {children}
                <div style={{ marginBottom: '10px' }}>
                    <h5 style={{ fontSize: '15px' }}>
                        조회 날짜: {startDate} ~ {endDate}
                    </h5>
                </div>
                <div>
                    {salesArray.length === 0 ? (
                        <p>판매 정보가 없습니다.</p> // 데이터가 없을 경우 메시지 표시
                    ) : (
                        <table>
                            <thead>
                                <tr>
                                    <th>옵션 이름</th>
                                    <th>판매량</th>
                                </tr>
                            </thead>
                            <tbody>
                                {salesArray.map((option, index) => (
                                    <tr key={index}>
                                        <td>{option.name}</td>
                                        <td>{option.quantity}</td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    )}
                </div>
            </div>
        </div>
    );
};

export default KeywordOptionModal;
