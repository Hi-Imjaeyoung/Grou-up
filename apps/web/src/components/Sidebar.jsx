import React, { useState, useEffect } from 'react';
import { getMyName } from '../services/auth';
import {
  Upload,
  Calculator,
  Home,
  Settings,
  LogOut,
  Folder,
  BookOpen,
  MessageCircle
} from 'lucide-react';
import logo from "../images/Logo.png";
import '../styles/Sidebar.css';
import { useNavigate, useLocation } from 'react-router-dom';
import { removeToken } from '../utils/tokenManager';

const MenuItem = ({ item, activePath, onSelect, currentPath = [], level = 0, location }) => {
  const hasChildren = Array.isArray(item.children) && item.children.length > 0;
  const itemPath = [...currentPath, item.title];
  const navigate = useNavigate();

  const isActive = item.campaignId
    ? location.pathname === `/campaigns/${item.campaignId}`
    : activePath.join('/').startsWith(itemPath.join('/'));

  const [isOpen, setIsOpen] = useState(false);

  useEffect(() => {
    if (hasChildren && isActive) {
      setIsOpen(true);
    }
  }, [hasChildren, isActive]);

  // ✅ handleClick 함수 수정
  const handleClick = () => {
    // item에 path가 지정되어 있으면 해당 경로로 이동
    if (item.path) {
      navigate(item.path);
    }
    onSelect(itemPath);

    if (hasChildren) {
      setIsOpen(prev => !prev);
    }

    if (item.path === '/logout') {
      removeToken();
      return navigate('/');
    }

    if (item.path) {
      return navigate(item.path);
    }

    if (item.campaignId) {
      return navigate(`/campaigns/${item.campaignId}?title=${encodeURIComponent(item.title)}`);
    }
  };

  return (
    <div className={`menu-item level-${level}`}>
      <div
        className={`menu-item-content${isActive ? ' active' : ''}`}
        style={{
          paddingLeft: hasChildren ? '8px' : '8px',
          marginTop: ['대시보드'].includes(item.title)
            ? '25px'
            : ['광고 캠페인 분석', '마진 계산기', '설정'].includes(item.title)
              ? '0px'
              : undefined,
        }}
        onClick={handleClick}
      >
        <span className="menu-item-icon">{item.icon || (hasChildren ? <Folder size={16} /> : null)}</span>
        <span className="menu-item-text" title={item.title}>
          {item.title}
        </span>
      </div>

      {hasChildren && isOpen && (
        <div className="submenu-container">
          {item.children.map((child, idx) => (
            <MenuItem
              key={idx}
              item={child}
              activePath={activePath}
              onSelect={onSelect}
              currentPath={itemPath}
              level={level + 1}
              location={location}
            />
          ))}
        </div>
      )}
    </div>
  );
};

// 외부 링크를 위한 새로운 컴포넌트
const ExternalLinkItem = ({ title, icon, url }) => {
  const handleClick = () => {
    window.open(url, '_blank', 'noopener,noreferrer');
  };

  return (
    <div className="menu-item level-0">
      <div
        className="menu-item-content"
        onClick={handleClick}
      >
        <span className="menu-item-icon">{icon}</span>
        <span className="menu-item-text" title={title}>
          {title}
        </span>
      </div>
    </div>
  );
};

const Sidebar = ({ campaigns }) => {
  const [activePath, setActivePath] = useState([]);
  const [userInfo, setUserInfo] = useState({ name: '' });
  const location = useLocation();

  useEffect(() => {
    (async () => {
      try {
        const res = await getMyName();
        setUserInfo({ name: res.data.name });
      } catch (err) {
        console.error('Failed to fetch user info:', err);
      }
    })();
  }, []);

  useEffect(() => {
    const path = location.pathname;

    // ✅ 광고 캠페인 분석 페이지 경로 추가
    if (path.startsWith('/campaigns/analysis')) {
      setActivePath(['광고 캠페인 분석']);
    } else if (path.startsWith('/campaigns/')) {
      const title = new URLSearchParams(location.search).get('title') || '';
      setActivePath(['광고 캠페인 분석', title]);
    } else if (path.startsWith('/margin-calculator/formula')) {
      setActivePath(['마진 계산기', '계산식 입력']);
    } else if (path.startsWith('/margin-calculator/result')) {
      setActivePath(['마진 계산기', '마진 계산']);
    } else if (path.startsWith('/margin-overview')) {
      setActivePath(['마진 계산기']);
    } else if (path === '/main') {
      setActivePath(['대시보드']);
    } else if (path === '/upload') {
      setActivePath(['액셀 업로드']);
    } else if (path === '/settings') {
      setActivePath(['설정']);
    } else {
      setActivePath([]);
    }
  }, [location]);

  const menuGroups = [
    [
      { title: "대시보드", icon: <Home size={16} />, path: "/main" },
      {
        title: "액셀 업로드",
        icon: <Upload size={16} />,
        path: "/upload",
        children: [
          { title: "데이터 삭제", path: "/delete-data" }
        ]
      }
    ],
    [
      {
        title: "광고 캠페인 분석",
        icon: <Folder size={13} />,
        path: "/campaigns/analysis",
        children: campaigns.map(c => ({
          title: c.title,
          campaignId: c.campaignId
        }))
      }
    ],
    [
      {
        title: "마진 계산기",
        icon: <Calculator size={16} />,
        path: "/margin-overview",
        children: [
          { title: "계산식 입력", path: "/margin-calculator/formula" },
          { title: "마진 계산", path: "/margin-calculator/result" }
        ]
      }
    ],
    [
      { title: "로그아웃", icon: <LogOut size={16} />, path: "/logout" }
    ]
  ];

  return (
    <aside className="sidebar">
      <nav className="sidebar-nav">
        <div className="sidebar-header">
          <div className="sidebar-logo-box">
            <img src={logo} alt="Logo" />
          </div>
          <div className="sidebar-username">{userInfo.name || '다시 로그인 해주세요'}</div>
        </div>
        <div className="menu-container">
          {menuGroups.map((group, groupIdx) => (
            <React.Fragment key={groupIdx}>
              {group.map((item, i) => (
                <MenuItem
                  key={`${groupIdx}-${i}`}
                  item={item}
                  activePath={activePath}
                  onSelect={setActivePath}
                  location={location}
                />
              ))}
              {groupIdx < menuGroups.length - 1 && <hr className="menu-divider" />}
            </React.Fragment>
          ))}
          {/* 추가된 헬프 링크들 */}
          <hr className="menu-divider" />
          <ExternalLinkItem
            title="사용자설명서"
            icon={<BookOpen size={16} />}
            url="https://flower-aunt-7b7.notion.site/1a1a559505a9804e85aee1b98355a5a2?pvs=74"
          />
          <ExternalLinkItem
            title="오픈채팅"
            icon={<MessageCircle size={16} />}
            url="https://open.kakao.com/o/gR9R7s4g"
          />
        </div>
      </nav>
    </aside>
  );
};

export default Sidebar;