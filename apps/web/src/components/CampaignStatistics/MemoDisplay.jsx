import React from 'react';
import '../../styles/campaignStats/MemoDisplay.css';


const MemoDisplay = ({ memoData }) => {
    if (!memoData || Object.keys(memoData).length === 0) {
        return (
            <div className="memo-container">
                <p className="no-memo-text">  해당 기간에 작성된 메모가 없습니다.</p>
            </div>
        );
    }

    // 1. 데이터 가공 로직에서 author 부분을 제거
    // 1. 데이터를 평평하게 가공하는 부분
    const flatMemoList = Object.entries(memoData).flatMap(([date, memos]) => {
        return memos.map((memoContent, index) => ({
            id: `${date}-${index}`,
            date: date,
            content: memoContent,
        }));
    });
    const sortedMemoList = flatMemoList.sort((a, b) => a.date.localeCompare(b.date));

    // 2. 렌더링 부분도 author 관련 코드를 모두 제거
    return (
        <div className="memo-container">
            <table className="memo-table">
                <thead>
                    <tr>
                        <th>날짜</th>
                        <th>내용</th>
                    </tr>
                </thead>
                <tbody>
                    {sortedMemoList.map(memo => (
                        <tr key={memo.id}>
                            <td>{memo.date}</td>
                            <td>
                                <p
                                    className="memo-content2"
                                    dangerouslySetInnerHTML={{ __html: memo.content }}
                                />
                            </td>
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    );
};

export default MemoDisplay;