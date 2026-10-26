import React, { useState, useMemo, useContext, useRef, useEffect } from 'react';
import "../../styles/memo/Memoview.css";
import { Trash2 } from 'lucide-react';
import { updateMemo } from '../../services/memo'; // 업데이트 API 호출을 위한 함수 import
import { MyContext } from "../MyContext.jsx";

const MemoView = ({ memos, fetchMemos }) => {
    const { deleteMemo, scrollToDate } = useContext(MyContext); // Context에서 scrollToDate 가져오기
    const [editingMemoId, setEditingMemoId] = useState(null); // 현재 수정 중인 메모 ID
    const [editedContent, setEditedContent] = useState(''); // 수정된 내용 상태
    const targetMemoRef = useRef(null); // 스크롤할 memo의 ref

    // memos를 날짜 기준으로 정렬
    const sortedMemos = useMemo(() => {
        if (!memos) return []; // memos가 null 또는 undefined인 경우 빈 배열 반환
        return [...memos].sort((a, b) => new Date(a.date) - new Date(b.date));
    }, [memos]);

    useEffect(() => {
        // 특정 날짜를 갖는 첫 번째 memo를 찾아서 해당 요소로 스크롤
        if (scrollToDate) {
            const targetMemo = sortedMemos.find(memo => memo.date.substring(0, 10) === scrollToDate);
            if (targetMemo) {
                targetMemoRef.current = targetMemo; // ref 업데이트
                const element = document.getElementById(`memo-${targetMemo.id}`); // id를 사용하여 요소 찾기
                if (element) {
                    element.scrollIntoView({ behavior: 'smooth', block: 'start' });
                }
            }
        }
    }, [sortedMemos, scrollToDate]); // sortedMemos 또는 scrollToDate가 변경될 때마다 실행

    const handleDelete = async (id, date, contents) => {
        const confirmDelete = window.confirm('삭제된 메모는 영영 떠나갑니다. \n정말 삭제할까요?');
        if (confirmDelete) {
            await deleteMemo({ id, date, contents });
            alert('메모가 삭제 되었습니다!');
            fetchMemos(); // 수정 후 메모 목록 갱신
        } else {
            alert('메모가 남아있기로 했습니다');
        }
    };

    const handleContentChange = (memo) => {
        setEditingMemoId(memo.id); // 수정 중인 메모 ID 설정
        // HTML 태그 제거 후 일반 텍스트만 editedContent에 저장
        setEditedContent(memo.contents.replace(/<br\s*\/?>/g, '\n'));
    };

    const handleBlur = async (memo) => {
        if (editedContent.trim() !== memo.contents) {
            const confirmUpdate = window.confirm('수정된 내용을 저장하시겠습니까?');
            if (confirmUpdate) {
                try {
                    // 서버에 저장하기 전에 다시 HTML 태그 추가
                    const contentToSave = editedContent.replace(/\n/g, '<br>');
                    await updateMemo({
                        memoId: memo.id,
                        contents: contentToSave,
                    });
                    alert('메모가 수정되었습니다.');
                    fetchMemos(); // 수정 후 메모 목록 갱신
                } catch (error) {
                    console.error('메모 수정 실패:', error);
                    alert('메모 수정에 실패했습니다.');
                }
            } else {
                setEditedContent(memo.contents); // 수정 취소 시 원래 내용으로 복원
            }
        }
        setEditingMemoId(null); // 수정 완료 후 상태 초기화
    };

    return (
        <div className="memo-view">
            <div>
                <table>
                    <tbody>
                        {sortedMemos.map((memo) => (
                            <tr
                                key={memo.id}
                                id={`memo-${memo.id}`} // id 추가
                            >
                                <td className="memo-view-td-date">  {memo.date.substring(5, 10)}</td>
                                <td className="memo-view-td-contents">
                                    {editingMemoId === memo.id ? (
                                        <textarea
                                            value={editedContent}
                                            onChange={(e) => setEditedContent(e.target.value)} // 수정된 내용 업데이트
                                            onBlur={() => handleBlur(memo)} // 포커스 아웃 시 수정 완료
                                            rows={editedContent.split('\n').length} // 줄 수를 수정된 내용에 맞춤
                                            style={{
                                                width: '100%',
                                                border: 'none',
                                                outline: 'none',
                                                resize: 'none', // 크기 조절 비활성화
                                            }}
                                        />
                                    ) : (
                                        <span onClick={() => handleContentChange(memo)} style={{ whiteSpace: 'pre-wrap' }}>
                                            {memo.contents.replace(/<br\s*\/?>/g, '\n')} {/* HTML의 <br>을 줄바꿈으로 변환 */}
                                        </span>
                                    )}
                                </td>
                                <td className="memo-view-td-delete-button">
                                    <button onClick={() => handleDelete(memo.id, memo.date, memo.contents)} style={{ border: 'none', background: 'none', cursor: 'pointer' }}>
                                        <Trash2 size={16} />
                                    </button>
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>
        </div>
    );
};

export default MemoView;
