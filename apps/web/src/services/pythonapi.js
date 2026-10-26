import axios from 'axios';
import { getToken } from '../utils/tokenManager';  // 토큰 가져오기 함수 사용
import { PYTHON_URL } from '../config/api';
export const uploadFile1 = (file, onUploadProgress) => {
  const formData = new FormData();
  formData.append('file', file);

  // 토큰 가져오기
  const token = getToken();

  return axios.post(`${PYTHON_URL}/django/upload_excel/`, formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
      'Authorization': `Bearer ${token}`  // Authorization 헤더에 토큰 추가
    },
    onUploadProgress: onUploadProgress,  // 진행률 추적 추가
  });
};

export const uploadFile2 = (file, onUploadProgress) => {
  const formData = new FormData();
  formData.append('file', file);

  // 토큰 가져오기
  const token = getToken();

  return axios.post('http://localhost:8000/upload_category/', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
      'Authorization': `Bearer ${token}`  // Authorization 헤더에 토큰 추가
    },
    onUploadProgress: onUploadProgress,  // 진행률 추적 추가
  });
};

export const uploadFile3 = (file, date, onUploadProgress) => {
  const formData = new FormData();

  const renamed = `${date}_${file.name}`;
  formData.append('file', file, renamed);

  // 토큰 가져오기
  const token = getToken();

  return axios.post(`${PYTHON_URL}/django/upload_margin/`, formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
      'Authorization': `Bearer ${token}`  // Authorization 헤더에 토큰 추가
    },
    onUploadProgress: onUploadProgress,  // 진행률 추적 추가
  });
};

const getPresignedUrl = (fileName) => {
  const token = getToken();
  return axios.get(`${PYTHON_URL}/django/generate_s3_presigned_url/`, {
    params: { fileName },
    headers: { 'Authorization': `Bearer ${token}` },
  });
};

/**
 * 2단계: 발급받은 Presigned URL로 S3에 파일을 직접 업로드하는 함수
 */
const uploadToS3 = (presignedUrl, file, onUploadProgress) => {
  // Django에서 생성한 Content-Type과 정확히 매칭
  let contentType = 'application/octet-stream';

  if (file.name.toLowerCase().endsWith('.xlsx') || file.name.toLowerCase().endsWith('.xls')) {
    contentType = 'application/vnd.ms-excel';
  } else if (file.name.toLowerCase().endsWith('.csv')) {
    contentType = 'text/csv';
  }

  return axios.put(presignedUrl, file, {
    headers: {
      'Content-Type': contentType, // Django와 동일한 Content-Type 사용
    },
    onUploadProgress
  });
};
/**
 * 3단계: S3에 넣은거 빼서 데이터 넣는 함수
 */
const processS3File = (s3Key, originalFileName) => {
  const token = getToken();
  return axios.post(`${PYTHON_URL}/django/upload_excel/`, {
    key: s3Key,
    fileName: originalFileName
  }, {
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    },
  });
};
/**
 * 전체 흐름
 */
export const uploadFileWithPresignedUrl = async (file, onUploadProgress) => {
  try {
    // 1단계: Presigned URL 받아오기
    const urlResponse = await getPresignedUrl(file.name);
    const { presignedUrl, key } = urlResponse.data;

    if (!presignedUrl || !key) {
      throw new Error("Presigned URL을 받아오지 못했습니다.");
    }

    // 2단계: S3로 파일 업로드
    const s3Response = await uploadToS3(presignedUrl, file, onUploadProgress);

    if (s3Response.status !== 200) {
      throw new Error("S3 업로드 실패");
    }


    // 3단계: 업로드된 파일 처리 요청
    const processResponse = await processS3File(key, file.name);

    return processResponse;

  } catch (error) {
    console.error("파일 업로드 및 처리 과정에서 에러 발생:", error);
    throw error;
  }
};