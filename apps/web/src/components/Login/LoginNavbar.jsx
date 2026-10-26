import React from "react";
import logo from "../../images/grouup_logo.png"; // 로고 이미지 경로
import '../../styles/Login/LoginNavbar.css'; // 새로운 CSS 파일 import

const LoginNavbar = () => {
    return (
        <nav className="navbar">
            <div className="navbar-container">
                <div className="flex items-center">
                    <img src={logo} alt="Grouup Logo" className="logo" />
                </div>
                <div className="flex space-x-2">
                    {/* <a href="#features" className="button">기능 소개</a>
                    <a href="#pricing" className="button">요금 정책</a>
                    <a href="#contact" className="button">문의</a>
                    <a href="#login" className="button">로그인</a> */}
                </div>
            </div>
        </nav>
    );
};

export default LoginNavbar;
