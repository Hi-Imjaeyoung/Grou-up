import { apiRequest } from '../utils/apiClient';

// 해당 캠패인의 키워드 조회
export const getKeywords = async ({ start, end, campaignId }) => {
  try {
    const response = await apiRequest(`/keyword/getKeywordsAboutCampaign?start=${start}&end=${end}&campaignId=${campaignId}`);
    // console.log(response); // 응답값을 콘솔에 출력
    return response;
  } catch (error) {
    console.error("Error fetching campaigns:", error); // 에러 핸들링
    throw error; // 에러를 다시 던져서 호출한 곳에서 처리할 수 있게 함
  }
}

// 해당 캠패인의 제와 키워드 조회
export const getExclusionKeywords = async ({ campaignId }) => {
  try {
    const response = await apiRequest(`/exclusionKeyword/getExclusionKeywords?campaignId=${campaignId}`);
    // console.log(response); // 응답값을 콘솔에 출력
    return response;
  } catch (error) {
    console.error("Error fetching campaigns:", error); // 에러 핸들링
    throw error; // 에러를 다시 던져서 호출한 곳에서 처리할 수 있게 함
  }
}

//키워드에서 제외키워드 등록
export const registerExclusionKeywords = async ({ selectedKeywords, campaignId }) => {
  try {
    // 요청할 데이터 객체 생성
    const requestData = {
      campaignId: campaignId, // 캠페인 ID 추가
      exclusionKeyword: selectedKeywords.map(keyword => keyword.keyword)
    };
    // API 요청
    const response = await apiRequest(`/exclusionKeyword/addExclusionKeyword`, 'POST', requestData);
    console.log(response); // 응답값을 콘솔에 출력
    return response;
  } catch (error) {
    console.error("Error fetching campaigns:", error); // 에러 핸들링
    throw error; // 에러를 다시 던져서 호출한 곳에서 처리할 수 있게 함
  }
}

export const removeExclsuionKeywords = async ({ selectedKeywords, campaignId }) => {
  try {
    // 요청할 데이터 객체 생성
    const requestData = {
      campaignId: campaignId, // 캠페인 ID 추가
      exclusionKeyword: selectedKeywords
    };
    // API 요청
    const response = await apiRequest(`/exclusionKeyword/remove`, 'DELETE', requestData);
    // console.log(response); // 응답값을 콘솔에 출력
    return response;
  } catch (error) {
    console.error("Error fetching campaigns:", error); // 에러 핸들링
    throw error; // 에러를 다시 던져서 호출한 곳에서 처리할 수 있게 함
  }
}

// 입찰가 등록
export const registerKeywordBid = async ({ selectedKeywords, campaignId }) => {
  try {
    const requestData = {
      campaignId: campaignId,
      data: selectedKeywords.map(keyword => ({
        keyword: keyword.keyword, // 각 keyword의 keyKeyword
        bid: keyword.bid, // 각 keyword의 bid
        keyInfo: null // keyInfo는 비워둡니다
      }))
    };
    const response = await apiRequest('/bid/adds', 'POST', requestData);
    // console.log(response);
    return response;
  } catch (error) {

  }
}

// 입찰가 조회
export const getBidKeywords = async ({ start, end, campaignId }) => {
  try {
    const response = await apiRequest(`/bid/gets?start=${start}&end=${end}&campaignId=${campaignId}`);
    // console.log(response); // 응답값을 콘솔에 출력
    return response;
  } catch (error) {
    console.error("Error fetching campaigns:", error); // 에러 핸들링
    throw error; // 에러를 다시 던져서 호출한 곳에서 처리할 수 있게 함
  }
}

// 입찰가 수정
export const updateBidKeyword = async ({ selectedKeywords, campaignId }) => {
  try {
    const requestData = {
      campaignId: campaignId,
      data: selectedKeywords.map(keyword => ({
        keyword: keyword.keyword, // 각 keyword의 keyKeyword
        bid: keyword.bid, // 각 keyword의 bid
        keyInfo: null // keyInfo는 비워둡니다
      }))
    };
    const response = await apiRequest(`/bid/update`, "PATCH", requestData);
    console.log(response); // 응답값을 콘솔에 출력
    return response;
  } catch (error) {
    console.error("Error fetching bid:", error); // 에러 핸들링
    throw error; // 에러를 다시 던져서 호출한 곳에서 처리할 수 있게 함
  }
}

//입찰가 삭제
export const deleteBodKeywords = async ({ selectedKeywords, campaignId }) => {
  try {
    const requestData = {
      campaignId: campaignId,
      data: selectedKeywords.map(keyword => ({
        keyword: keyword.keyword, // 각 keyword의 keyKeyword
        bid: keyword.bid, // 각 keyword의 bid
        keyInfo: null // keyInfo는 비워둡니다
      }))
    };
    const response = await apiRequest(`/bid/delete`, "DELETE", requestData);
    // console.log(response); // 응답값을 콘솔에 출력
    return response;
  } catch (error) {
    console.error("Error delete bid:", error); // 에러 핸들링
    throw error; // 에러를 다시 던져서 호출한 곳에서 처리할 수 있게 함
  }
}

export const getCampaignStats = async ({ campaignId, start, end }) => {
  try {
    const response = await apiRequest(`/keyword/getCampaignStat?campaignId=${campaignId}&start=${start}&end=${end}`);
    // console.log(response); // 응답값을 콘솔에 출력
    return response;
  } catch (error) {
    console.error("Error fetching campaigns:", error); // 에러 핸들링
    throw error; // 에러를 다시 던져서 호출한 곳에서 처리할 수 있게 함
  }
}

export const getMemoAboutCampaign = async ({ campaignId, start, end }) => {
  try {
    const response = await apiRequest(`/memo/getMemoByDate?start=${start}&end=${end}&campaignId=${campaignId}`);
    return response;
  } catch (error) {
    console.error("Error fetching campaigns:", error); // 에러 핸들링
    throw error; // 에러를 다시 던져서 호출한 곳에서 처리할 수 있게 함
  }
}