import { React, useEffect } from "react";
import DashboardGridV2 from "./Dashboard_v2/DashboardGridV2";
import "../styles/Mainform.css"; // 메인 폼 전용 스타일

const MainForm = () => {
  useEffect(() => {
    const meta = document.createElement('meta');
    meta.name = 'viewport';
    meta.content = 'width=device-width, initial-scale=0.3';
    document.head.appendChild(meta)
  }, []); // 빈 배열([])을 넣으면 컴포넌트가 처음 렌더링될 때만 실행됨
  return (
    <div className="form-main-content">
      <DashboardGridV2 />
    </div>
  );
};
export default MainForm;
