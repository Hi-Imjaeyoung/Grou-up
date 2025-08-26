// load-test.js

import http from 'k6/http';
import { check, sleep } from 'k6';

const users = [
    { vu_id: 1, token: 'TestToken : GUTokenuser01' },
    { vu_id: 2, token: 'TestToken : GUTokenuser02' },
    { vu_id: 3, token: 'TestToken : GUTokenuser03' },
    { vu_id: 4, token: 'TestToken : GUTokenuser04' },
    { vu_id: 5, token: 'TestToken : GUTokenuser05' },
];

// export const options = {
//     // VU 목표치를 60에서 30 정도로 낮춰서 테스트를 시작해보자.
//     stages: [
//         { duration: '15s', target: 30 },
//         { duration: '15s', target: 0 },
//     ],
//     thresholds: {
//         'http_req_failed': ['rate<0.01'],
//         'http_req_duration': ['p(95)<1000'],
//     },
// };

export const options = {
    // 5명의 유저로 테스트할 거니까, VU를 5로 설정
    vus: 5,
    duration: '30s',
};


export default function () {
    const currentUser = users[(__VU - 1) % users.length];
    // console.log(`Executing test for VU ${__VU} (${currentUser.name})`);
    const url = `http://localhost:8080/api/campaign/getMyCampaigns`;

    const params = {
        headers: {
            'Authorization': `${currentUser.token}`,
            'Content-Type': 'application/json',
        },
    };

    // ✨ params 객체 자체를 없애고 헤더 없이 요청을 보낸다.
    const res = http.get(url, params);

    check(res, {
        'is status 200': (r) => r.status === 200,
    })
    sleep(1);
}


