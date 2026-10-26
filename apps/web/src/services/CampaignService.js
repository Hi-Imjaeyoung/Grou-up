import { apiRequest } from '../utils/apiClient';

// 캠패인 이름 조회
export const getMyCampaignsAnalysis = async ({ start, end }) => {
    try {
        const response = await apiRequest(`/campaign/totalAnalysisData?start=${start}&end=${end}`);
        // console.log(response.data); // 응답값을 콘솔에 출력
        return response.data;
    } catch (error) {
        console.error("Error fetching campaigns:", error); // 에러 핸들링
        throw error; // 에러를 다시 던져서 호출한 곳에서 처리할 수 있게 함
    }
}
// 캠패인 이름 조회
export const getDailyMarginSummary = async ({ start, end }) => {
    try {
        const response = await apiRequest(`/margin/getDailyMarginSummary?start=${start}&end=${end}`);
        console.log(response.data); // 응답값을 콘솔에 출력
        return response.data;
    } catch (error) {
        console.error("Error fetching getDailyMarginSummary:", error); // 에러 핸들링
        throw error; // 에러를 다시 던져서 호출한 곳에서 처리할 수 있게 함
    }
}
