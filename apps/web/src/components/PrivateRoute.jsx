import React from "react";
import { Navigate } from "react-router-dom";
import { getToken } from "../utils/tokenManager";

const PrivateRoute = ({ children }) => {
    // JWT 토큰을 localStorage에서 가져오기
    const token = getToken()
    if (!token) {
        // 토큰이 없으면 로그인 페이지로 리다이렉트
        return <Navigate to="/" replace />;
    }

    // 인증된 경우 자식 컴포넌트를 렌더링
    return children;
};

export default PrivateRoute;
