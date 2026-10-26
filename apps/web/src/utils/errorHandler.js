// 에러 메시지 매핑
export const errorMessages = {
    "already member exist": "이미 등록된 이메일입니다. 다른 이메일을 사용해주세요.",
    "account cannot be found": "이메일 또는 비밀번호가 잘못되었습니다.",
    "server error": "서버에서 문제가 발생했습니다. 잠시 후 다시 시도해주세요.",
    "email is essential" : "이메일을 작성해주세요",
    "Invalid password format" : "특수문자, 소문자 , 대문자 , 숫자중 3가지를 포함, 8글자 이상 20글자 이하로 비밀번호를 작성해주세요",
    "name is essential" : "이름을 작성해주세요",
    "This account is registered with Kakao login." : "카카오 계정이 존재합니다.",

    default: "알 수 없는 오류가 발생했습니다. 관리자에게 문의해주세요.",

  };
  
  // 공통 에러 처리 함수
  export const handleError = (error) => {
    const message = errorMessages[error.message] || errorMessages.default;
    alert(message); 
  };
  