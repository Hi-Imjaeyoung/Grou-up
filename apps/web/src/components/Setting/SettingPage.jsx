import React, { useState } from "react"; // useState 임포트
import "./SettingPage.css";
import SunBuybtns from "./SunBuyBtns.jsx"
import SunShineLog from "./SunShineLog.jsx"

const SettingPage = ({ }) => {
    // 현재 활성화된 탭을 관리하는 state. 초기값은 'mySunshine' (내 햇살)
    const [activeTab, setActiveTab] = useState('mySunshine');

    // 탭 버튼 클릭 시 호출될 핸들러 함수
    const handleTabClick = (tabKey) => {
        setActiveTab(tabKey);
        // 실제 애플리케이션에서는 이 state 값에 따라 아래 영역에
        // 해당 탭의 내용을 가진 다른 컴포넌트를 렌더링하게 됩니다.
        console.log(`Active tab changed to: ${tabKey}`); // 확인용 로그
    };

    return (
        <div className="SettingPage">
            <h2 className="SettingPage_header">설정</h2>
            <div className="SettingTabContainer">
                {/* 내 햇살 버튼 */}
                <button
                    // 현재 activeTab이 'mySunshine'이면 'active-tab' 클래스 추가
                    className={`SettingTabButton ${activeTab === 'mySunshine' ? 'active-tab' : ''}`}
                    onClick={() => handleTabClick('mySunshine')} // 클릭 시 state 업데이트
                >
                    <span className="SettingTabButton_label">내 햇살</span> {/* 내부 클래스명 일관성 유지 */}
                    <div className="SettingTabButton_content"> {/* 내부 클래스명 일관성 유지 */}
                        <div className="SettingTabButton_cnt">300</div> {/* 내부 클래스명 일관성 유지 */}
                        <div className="SettingTabButton_string1">현재 보유 햇살</div> {/* 내부 클래스명 일관성 유지 */}
                    </div>
                </button>

                {/* 햇살 이용 내역 버튼 */}
                <button
                    className={`SettingTabButton ${activeTab === 'sunshineLog' ? 'active-tab' : ''}`}
                    onClick={() => handleTabClick('sunshineLog')}
                >
                    <span className="SettingTabButton_label">햇살이용내역</span>
                    <div className="SettingTabButton_content">
                        {/* 실제 내용에 맞게 아래 값들은 변경 필요 */}
                        <div className="SettingTabButton_cnt">254</div> {/* 예시: 이미지의 숫자 */}
                        <div className="SettingTabButton_string1">이번 달 사용 햇살</div> {/* 예시: 이미지의 텍스트 */}
                    </div>
                </button>

                {/* 추천인 버튼 */}
                <button
                    className={`SettingTabButton ${activeTab === 'recommandId' ? 'active-tab' : ''}`}
                    onClick={() => handleTabClick('recommandId')}
                >
                    <span className="SettingTabButton_label">내 추천인 ID</span> {/* 예시: 이미지의 라벨 텍스트 */}
                    <div className="SettingTabButton_content">
                        {/* 실제 내용에 맞게 아래 값들은 변경 필요 */}
                        <div className="SettingTabButton_cnt">GROUPUP123</div> {/* 예시: 이미지의 ID */}
                        <div className="SettingTabButton_string1">1명 초대하면 1햇살 지급</div>
                    </div>
                </button>

                {/* 나를 추천한 사람 버튼 */}
                <button
                    className={`SettingTabButton ${activeTab === 'recommandCounter' ? 'active-tab' : ''}`}
                    onClick={() => handleTabClick('recommandCounter')}
                >
                    <span className="SettingTabButton_label">나를 추천한 사람</span>
                    <div className="SettingTabButton_content">
                        {/* 실제 내용에 맞게 아래 값들은 변경 필요 */}
                        <div className="SettingTabButton_cnt">52</div> {/* 예시: 이미지의 숫자 */}
                        <div className="SettingTabButton_string1">1명 당 1햇살로 변경</div> {/* 예시: 이미지의 텍스트 */}
                    </div>
                </button>
            </div>

            {/* 여기에 activeTab 상태에 따라 다른 컴포넌트를 렌더링하는 로직 추가 */}
            {activeTab === 'mySunshine' && <SunBuybtns />}
            {activeTab === 'sunshineLog' && <SunShineLog />}
            {/* 예: {activeTab === 'sunshineLog' && <SunshineLogComponent />} */}
            {/* ... */}

        </div>
    );
}

export default SettingPage;