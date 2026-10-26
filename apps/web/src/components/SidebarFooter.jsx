// src/components/Header.jsx
import React, { useState, useEffect } from 'react';
import { getMyEmailAndRole } from '../services/auth';

const SidebarFooter = () => {
    const [userInfo, setUserInfo] = useState({ email: '', role: '' }); // 사용자 정보 상태

    useEffect(() => {
        // API 호출하여 사용자 정보 가져오기
        const fetchUserInfo = async () => {
            try {
                const response = await getMyEmailAndRole();
                setUserInfo({
                    email: response.data.email,
                    role: response.data.role,
                });
            } catch (error) {
                console.error('Failed to fetch user info:', error);
            }
        };

        fetchUserInfo();
    }, []);

    return (
        <div className="sidebar-footer">
            <span className="user-name">{userInfo.email || 'Loading...'}</span>
        </div>
    );
};

export default SidebarFooter;
