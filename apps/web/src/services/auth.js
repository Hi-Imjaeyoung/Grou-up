import { setToken } from '../utils/tokenManager';
import { apiRequest } from '../utils/apiClient';

// 로그인 요청
export const login = async ({ email, password }) => {
  const data = await apiRequest('/members/login', 'POST', { email, password });

  // accessToken 저장
  setToken(data.data.accessToken);
  return data; // 필요 시 추가 데이터 반환
};

// 회원가입 요청
export const signup = async ({ email, password, name }) => {
  return apiRequest("/members/signup", "POST", { email, password, name });
};

// role + nickname 요청
export const getMyName = async () => {
  const data = await apiRequest('/members/getMyName', "GET")
  console.log(data)

  return data;
}

