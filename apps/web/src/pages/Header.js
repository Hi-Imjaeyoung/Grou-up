import React from 'react';
import '../styles/Header.css'; // 스타일 파일을 임포트
import { useNavigate } from 'react-router-dom'; // useNavigate 임포트

const Header = () => {
    const navigate = useNavigate();

    const handleTitleClick = () => {
        navigate('/main');
    };

    return (
        <div className="header">
            <h1 className="header-title" onClick={handleTitleClick}>
                GrouUP
            </h1>
            <div className="user-info">
                <div className="user-details">
                    <span className="user-name">Moni Roy</span>
                    <span className="user-role">Admin</span>
                </div>
                <button className="dropdown-button">▼</button>
            </div>
        </div>
    );
};

export default Header;