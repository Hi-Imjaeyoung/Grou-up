import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    vus: 50, // 50명 동시 접속 (Virtual Users)
    duration: '10s', // 10초 동안 미친 듯이 파상 공세
};

// 테스트용 엑셀 파일 로드 (네 로컬에 있는 22KB짜리 파일 경로)
const excelFile = open('./test_excel.xlsx', 'b');

export default function () {
    const formattedVU = __VU.toString().padStart(2, '0');
    const token = `TestToken : GUTokenuser${formattedVU}`; // ex: testToken01, testToken50
    const url = 'http://localhost:8080/api/marginforcam/upload'; // 네 업로드 API 주소

    const data = {
        file: http.file(excelFile, 'test_excel.xlsx', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'),
    };

    // 만약 JWT 인증이 필요하면 헤더에 추가
    const params = {
        headers: {
            'Authorization': `${token}`,
        },
    };

    const res = http.post(url, data, params);

    check(res, {
        'is status 200': (r) => r.status === 200,
    });

    sleep(1); // 유저당 1초 쉬고 다시 요청
}