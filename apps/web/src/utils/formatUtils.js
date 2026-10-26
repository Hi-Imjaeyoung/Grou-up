export const formatNumber = (number) => {
    return new Intl.NumberFormat('ko-KR').format(number); // 한국어 형식으로 포맷
};

export const formatInteger = (number) => {
    const intNumber = Math.trunc(number); // 소수점 이하 버리기
    return new Intl.NumberFormat('ko-KR').format(intNumber);
};