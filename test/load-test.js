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
//     stages: [
//         // 1. 5초 동안 유저를 0명에서 100명까지 서서히 늘린다. (Ramp-up)
//         //    (서버에 갑작스러운 부하를 주지 않기 위한 예열 단계)
//         { duration: '5s', target: 100 },
//         // 2. 100명의 유저를 10초 동안 유지한다. (요구사항 핵심)
//         { duration: '10s', target: 100 },
//         // 3. 5초 동안 유저를 100명에서 0명으로 서서히 줄인다. (Ramp-down)
//         { duration: '5s', target: 0 },
//     ],
//     thresholds: {
//         'http_req_failed': ['rate<0.01'],
//         'http_req_duration': ['p(95)<10000'],
//     },
// };

export const options = {
    stages: [
        // 1. 30초간 평상시 상태 (유저 1~2명)
        { duration: '30s', target: 2 },

        // 2. 스파이크! (5명으로 급증)
        { duration: '5s', target: 10 }, // 👈 여기를 5로 수정!

        // 3. 최고조 5명 유지
        { duration: '10s', target: 10 }, // 👈 여기도 5로 수정!

        // 4. 복구
        { duration: '5s', target: 2 },
        { duration: '10s', target: 2 },
    ],
    thresholds: {
        'http_req_duration': ['p(95)<1000'],
    },
};

export default function () {
    // 100명의 VU가 5개의 토큰을 순환하며 사용하게 된다. (이 로직은 그대로 두면 돼!)
    const currentUser = users[(__VU - 1) % users.length];
    const url = `http://localhost:8080/api/marginforcam/downloadExcel`;

    const params = {
        headers: {
            'Authorization': `${currentUser.token}`,
            'Content-Type': 'application/json',
        },
    };

    const res = http.get(url, params);

    check(res, {
        'is status 200': (r) => r.status === 200,
    })
    sleep(1);
}