import { apiRequest } from '../utils/apiClient';

export const getHistory = async ({ startDate, endDate }) => {
    try {
        const response = await apiRequest(`/file/getHistory?startDate=${startDate}&endDate=${endDate}`);
        return response;
    } catch (error) {
        console.error("Error fetching campaigns:", error); // 에러 핸들링
        throw error; // 에러를 다시 던져서 호출한 곳에서 처리할 수 있게 함
    }
}
export const deleteNetSalesFile = async ({ id }) => {
    try {
        const response = await apiRequest(`/file/deleteNetSalesFile?id=${id}`, "DELETE");
        return response;
    } catch (error) {
        console.error("Error fetching campaigns:", error); // 에러 핸들링
        throw error; // 에러를 다시 던져서 호출한 곳에서 처리할 수 있게 함
    }
}