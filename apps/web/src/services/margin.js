import { apiRequest } from '../utils/apiClient';

export const getMargin = async ({ date }) => {
    try {
        const response = await apiRequest(`/margin/getCampaignAllSales?date=${date}`);
        return response;
    } catch (error) {
        throw error;
    }
}
export const getTotalSales = async ({ date }) => {
    try {
        const response = await apiRequest(`/margin/getDailyAdSummary?date=${date}`)
        return response;
    } catch (error) {
        throw error
    }
}
export const getMarginByCampaignId = async ({ startDate, endDate, campaignId }) => {
    try {
        const response = await apiRequest(`/margin/getMargin?startDate=${startDate}&endDate=${endDate}&campaignId=${campaignId}`);
        return response;
    } catch (error) {
        throw error;
    }
}
export const marginUpdatesByPeriod = async (data) => {
    try {
        const response = await apiRequest('/marginforcampaignchangedbyperiod/save', 'PATCH', data);
        return response; // 성공 시 반환
    } catch (error) {
        console.error('데이터 생성 중 오류 발생:', error.message);
        throw error; // 오류를 상위로 전달
    }
};

export const getDailyMarginSummary = async ({ date }) => {
    try {
        const response = await apiRequest(`/margin/getDailyMarginSummary?start=${date}&end=${date}`)
        return response;
    } catch (error) {
        throw error
    }
}
export const getNetProfitAndReturnCost = async ({ startDate, endDate, }) => {
    try {
        const response = await apiRequest(`/margin/getNetProfitAndReturnCost?startDate=${startDate}&endDate=${endDate}`);
        return response;
    } catch (error) {
        throw error
    }
}
export const updateEfficiencyAndAdBudget = async (data) => {
    try {
        const response = await apiRequest(`/margin/updateEfficiencyAndAdBudget`, 'POST', data);
        return response; // 성공 시 반환
    } catch (error) {
        console.error('데이터 생성 중 오류 발생:', error.message);
        throw error; // 오류를 상위로 전달
    }
};
export const createMarginTable = async ({ targetDate, campaignId }) => {
    try {
        const response = await apiRequest(
            `/margin/createMarginTable?targetDate=${targetDate}&campaignId=${campaignId}`,
            'POST'
        );
        return response;
    } catch (error) {
        throw error;
    }
};

export const findLatestMarginDateByEmail = async () => {
    try {
        const response = await apiRequest(`/margin/findLatestMarginDateByEmail`);
        return response;
    } catch (error) {
        throw error
    }
}
export const getMarginOverview = async ({ start, end }) => {
    try {
        const response = await apiRequest(`/margin/getMarginOverview?end=${end}&start=${start}`);
        return response;
    } catch (error) {
        throw error
    }
}
export const getMarginOverviewGraph = async ({ start, end }) => {
    try {
        const response = await apiRequest(`/margin/getMarginOverviewGraph?end=${end}&start=${start}`);
        return response;
    } catch (error) {
        throw error
    }
}