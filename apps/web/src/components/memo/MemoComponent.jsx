import React, { useEffect, useState } from 'react';
import MemoView from './MemoView'; // MemoView import
import MemoCreate from './MemoCreate'; // MemoCreate import
import { useLocation } from 'react-router-dom'; // useLocation import
import { getMemos } from '../../services/memo'; // getMemos import
import "../../styles/memo/MemoComponent.css"; // 필요한 스타일 import

const MemoComponent = () => {
    const [memos, setMemos] = useState([]); // 메모 상태
    const [error, setError] = useState(null); // 에러 상태 추가
    const location = useLocation(); // 현재 위치 정보 가져오기
    const campaignId = location.pathname.split('/')[2]; // URL 경로에서 campaignId 추출

    const fetchMemos = async () => {
        try {
            // console.log(campaignId)
            const response = await getMemos({ campaignId });
            setMemos(response || []);
        } catch (error) {
            console.error('메모 불러오기 실패:', error);
            setError('메모 불러오기를 실패했습니다.'); // 에러 메시지 설정
        }
    };

    useEffect(() => {
        fetchMemos(); // 컴포넌트 마운트 시 메모 가져오기
    }, [campaignId]); // campaignId가 변경될 때마다 호출

    const handleAddMemo = async (newMemo) => {
        // 메모 추가 후 목록 갱신
        await fetchMemos();
    };

    return (
        <div className="memo-component">
            <h2 className='memo-componet-header'>메 모</h2>
            {error && <div className="error-message">{error}</div>} {/* 에러 메시지 표시 */}
            <MemoView
                memos={memos}
                fetchMemos={fetchMemos} /> {/* 메모 조회 컴포넌트 */}
            <MemoCreate onAddMemo={handleAddMemo} /> {/* 메모 작성 컴포넌트 */}
        </div>
    );
}

export default MemoComponent;
