import { apiRequest } from '../utils/apiClient';

export const getMyTotalSales = async ({ date }) => {
    try {
        const response = await apiRequest(`/net/getMyTotalSales?start=${date}&end=${date}`);
        return response.data;
    } catch (error) {
        console.error("Error fetching getMyTotalSales:", error); // 에러 핸들링
        throw error; // 에러를 다시 던져서 호출한 곳에서 처리할 수 있게 함
    }
}