import { apiRequest } from '../utils/apiClient';
import axios from 'axios';

// 해당 캠패인의 전체옵션 조회
export const getMyExecutionData = async ({ campaignId }) => {
    try {
        const response = await apiRequest(`/execution/getMyExecutionData?campaignId=${campaignId}`);
        console.log(response)
        return response;
    } catch (error) {
        console.error("Error fetching campaigns:", error);
        throw error;
    }
}
export const updateMyExecutionData = async (data) => {
    try {
        const response = await apiRequest('/execution/update', 'PATCH', data);
        console.log(response)
        return response; // 성공 시 반환
    } catch (error) {
        console.error('데이터 생성 중 오류 발생:', error.message);
        throw error; // 오류를 상위로 전달
    }
};