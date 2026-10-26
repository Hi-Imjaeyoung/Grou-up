import React, { useState } from 'react';
import '../../styles/memo/MemoButton.css';
import { SquarePen, PenOff } from 'lucide-react'; // Lucide 아이콘 import

const MemoButton = ({ onClick }) => {
    const [isExpanded, setIsExpanded] = useState(false); // 아이콘 상태 관리

    const handleClick = () => {
        setIsExpanded((prev) => !prev); // 클릭 시 상태 토글
        onClick(); // 부모 컴포넌트의 onClick 호출
    };

    return (
        <div className="memo-button" onClick={handleClick}>
            {isExpanded ? (
                <PenOff className="icon" /> // 아이콘이 X로 변경
            ) : (
                <SquarePen className="icon" /> // 기본 아이콘
            )}
        </div>
    );
};

export default MemoButton;
