import { getToken } from './tokenManager';
import { API_URL } from '../config/api';

// ⭐️ 하나의 유연한 공통 API 요청 함수
export const apiRequest = async (endpoint, method = 'GET', body = null, options = {}) => {
  try {
    const token = getToken();
    const headers = {}; // 기본 헤더는 비워두자

    // [1] 요청 body의 타입에 따라 Content-Type을 동적으로 설정
    if (body) {
      if (body instanceof FormData) {
        // body가 FormData일 경우, Content-Type을 브라우저가 자동으로 설정하게 둬야 해.
        // (multipart/form-data 헤더와 boundary가 자동으로 생성됨)
      } else {
        headers['Content-Type'] = 'application/json';
      }
    }

    if (token) {
      headers.Authorization = `Bearer ${token}`;
    }

    const response = await fetch(`${API_URL}${endpoint}`, {
      method,
      headers,
      // [2] body가 FormData가 아닐 경우에만 JSON.stringify를 사용
      body: body && !(body instanceof FormData) ? JSON.stringify(body) : body,
    });

    // [3] 에러 처리를 먼저 하자
    if (!response.ok) {
      // 서버가 보낸 에러 메시지는 JSON이 아니라 일반 텍스트(String)일 수 있어.
      // .text()로 응답 본문을 안전하게 읽어온다!
      const errorText = await response.text();
      // 읽어온 텍스트로 에러를 던져야 컴포넌트의 catch에서 받을 수 있어.
      throw new Error(errorText || '서버에서 에러가 발생했습니다.');
    }

    // [4] 옵션의 responseType에 따라 응답을 다르게 처리!
    const responseType = options.responseType || 'json'; // 기본값은 'json'

    if (responseType === 'blob') {
      return await response.blob(); // blob으로 처리
    }

    // response.body가 비어있는 경우를 대비
    const text = await response.text();
    if (!text) {
      return null; // 혹은 {}, [] 등 API 스펙에 맞게
    }

    return JSON.parse(text); // 기본적으로 json으로 처리

  } catch (error) {
    // ✨ 이 catch 블록은 이제 진짜 '네트워크 오류'나 위에서 던진 에러를 잡는 역할만 해.
    console.error('apiRequest 최종 에러:', error.message);
    // error.message에는 위에서 던진 "서버 에러 메시지" 또는 "네트워크 오류"가 담겨있어.
    throw error; // 받은 에러를 그대로 다시 던져서 컴포넌트에서 처리할 수 있게 한다
  }
};