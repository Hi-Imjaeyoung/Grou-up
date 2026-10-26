import { apiRequest } from '../utils/apiClient';

// 캠패인 이름 조회
export const getMyCampaigns = async () => {
  try {
    const response = await apiRequest("/campaign/getMyCampaigns");
    // console.log(response); // 응답값을 콘솔에 출력
    return response;
  } catch (error) {
    console.error("Error fetching campaigns:", error); // 에러 핸들링
    throw error; // 에러를 다시 던져서 호출한 곳에서 처리할 수 있게 함
  }
}

export const deleteCampaignAll = async (campaignIds) => { // 파라미터를 객체로 받지 않음
  try {
    const response = await apiRequest("/campaign/deleteCampaign", "DELETE", campaignIds);
    return response;
  } catch (error) {
    console.error("캠페인 삭제 API 호출 중 오류:", error); // 오류 로깅 추가
    throw error; // 오류를 다시 던져서 상위 컴포넌트에서 처리하도록 함
  }
};

export const deleteCampaignData = async (params) => { // 파라미터를 객체로 받음
  try {
    const body = {
      campaignIds: params.campaignIds,
      start: params.start,
      end: params.end
    }
    console.log(body);
    const response = await apiRequest("/campaign/deleteCampaignData", "DELETE", body);
    console.log(response);
    return response;
  } catch (error) {
    console.error("캠페인 삭제 API 호출 중 오류:", error); // 오류 로깅 추가
    throw error; // 오류를 다시 던져서 상위 컴포넌트에서 처리하도록 함
  }
};