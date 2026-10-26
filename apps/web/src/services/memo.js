import { apiRequest } from '../utils/apiClient';


export const addMemo = async ({ campaignId, contents, date }) => {
    try {
        const requestData = {
            campaignId: campaignId, // 캠페인 ID 추가
            date: date,
            contents: contents
        };
        const response = await apiRequest(`/memo/post`, 'POST', requestData);
        console.log(response)
        return response;
    } catch (error) {
        console.error("Error add memo:", error);
        throw error;
    }
}

export const getMemos = async ({ campaignId }) => {
    try {
        const response = await apiRequest(`/memo/getMemoAboutCampaign?campaignId=${campaignId}`)
        return response.data;
    } catch (error) {
        console.error("Error get memo:", error)
        throw error;
    }
}

export const deleteMemo = async ({ id }) => {
    try {
        const response = await apiRequest(`/memo/delete?memoId=${id}`, 'DELETE');
        // console.log(response)
        return response;
    } catch (error) {
        console.error("Error add memo:", error);
        throw error;
    }
}

export const updateMemo = async ({ memoId, contents }) => {
    try {
        const requestData = {
            memoId: memoId, // 캠페인 ID 추가
            contents: contents
        };
        const response = await apiRequest(`/memo/update`, 'PATCH', requestData);
        // console.log(response)
        return response;
    } catch (error) {
        console.error("Error add memo:", error);
        throw error;
    }
}

