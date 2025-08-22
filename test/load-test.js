// load-test.js

import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    // VU 목표치를 60에서 30 정도로 낮춰서 테스트를 시작해보자.
    stages: [
        { duration: '15s', target: 30 },
        { duration: '15s', target: 0 },
    ],
    thresholds: {
        'http_req_failed': ['rate<0.01'],
        'http_req_duration': ['p(95)<1000'],
    },
};

export default function () {
    const randomString = Math.random().toString(36).substring(2, 15);
    const url = `http://localhost:8080/api/some-non-existent-path/${randomString}`;

    // ✨ params 객체 자체를 없애고 헤더 없이 요청을 보낸다.
    const res = http.get(url);

    check(res, { 'is status 401 or 404': (r) => r.status === 401 || r.status === 404 });
}


