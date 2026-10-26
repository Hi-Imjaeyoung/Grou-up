import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    vus: 10,           // 처음부터 10명이 접속
    duration: '10s',   // 10초 동안 유지
    thresholds: {
        http_req_failed: ['rate<0.01'],
        http_req_duration: ['p(95)<500'],
    },
};

export default function () {
    const commandUrl = `http://localhost:8080/api/command/campaign/delete/data`;
    const email = 'testUser2@test.com'; // 테스트할 타겟 유저
    const year = 2026;

    const deletePayload = JSON.stringify({
        email: email,
        start: `${year}-01-01`,
        end: `${year}-01-31`, // 1월 한 달치 삭제 요청!
        campaignIds: [11]      // 더미 캠페인 ID
    });
    const params = { headers: { 'Content-Type': 'application/json' } };
    let deleteRes = http.del(commandUrl, deletePayload, params);
    check(deleteRes, { 'Delete Status 200': (r) => r.status === 200 });
    sleep(1)

}