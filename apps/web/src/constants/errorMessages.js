// 에러 메시지 매핑
export const errorMessages = {
    "already member exist": "이미 등록된 이메일입니다. 다른 이메일을 사용해주세요.",
    "account cannot be found": "이메일 또는 비밀번호가 잘못되었습니다.",
    "server error": "서버에서 문제가 발생했습니다. 잠시 후 다시 시도해주세요.",
    default: "알 수 없는 오류가 발생했습니다. 관리자에게 문의해주세요.",
  };
  
  // 공통 에러 처리 함수
  export const handleError = (error) => {
    const message = errorMessages[error.message] || errorMessages.default; // 에러 메시지 매핑
    alert(message); // 사용자 친화적인 메시지 표시
  };