import React, { useState } from "react";
import "../styles/Mainform.css"; // 스타일 파일 추가
import KeywordComponent from "../components/KeywordComponent"; // 추가
import CampaignOptionDetailsComponent from "../components/CampaignOptionDetailsComponent";
import ExclusionKeywordComponent from '../components/ExclusionKeywordComponent';
import Totalsearchbar from "./Totalsearchbar";
import { useParams, useLocation } from "react-router-dom";

const CampaignDetail = () => {
  const { campaignId } = useParams(); // URL 파라미터에서 campaignId를 가져옴
  const location = useLocation(); // 현재 위치 정보 가져오기

  // 쿼리 파라미터에서 title 추출하기
  const queryParams = new URLSearchParams(location.search);
  const title = queryParams.get("title");

  const getYesterday = () => {
    const yesterday = new Date();
    yesterday.setDate(yesterday.getDate() - 1); // 하루 전 날짜로 설정
    return yesterday.toISOString().split("T")[0]; // YYYY-MM-DD 형식 반환
  };

  // 상태 추가: 어떤 KeywordComponent를 보여줄지 결정
  const [activeComponent, setActiveComponent] = useState(null);
  const [startDate, setStartDate] = useState(getYesterday()); // 시작 날짜 상태
  const [endDate, setEndDate] = useState(getYesterday()); // 종료 날짜 상태

  // 버튼 클릭 시 호출될 함수
  const handleComponentChange = (component) => {
    setActiveComponent(component);
  };

  // 검색 버튼 클릭 시 호출될 함수
  const handleSearch = (start, end) => {
    setStartDate(start);
    setEndDate(end);
    console.log("Search triggered with dates:", start, end);
  };

  return (
    <div className="main-content">
      <div className="min-h-screen">
        <main className="container mx-auto p-6">
          <Totalsearchbar
            onComponentChange={handleComponentChange}
            onSearch={handleSearch} // 검색 함수 전달
            title={title}
          />
          <div className="mt-8">
            {activeComponent === "Keywordcomponent" && (
              <KeywordComponent
                campaignId={campaignId}
                startDate={startDate}
                endDate={endDate}
              />
            )}
            {activeComponent === 'ExclusionKeywordComponent' && <ExclusionKeywordComponent campaignId={campaignId} />}
            {activeComponent === "CampaignOptionDetailsComponent" && (
              <CampaignOptionDetailsComponent
                campaignId={campaignId}
                startDate={startDate}
                endDate={endDate}
              />
            )}
          </div>
        </main>
      </div>
    </div>
  );
};

export default CampaignDetail;
