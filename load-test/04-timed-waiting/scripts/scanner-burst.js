// ③ TIMED_WAITING 재검증 (2026-10-27): 스캐너 요청 버스트.
// 2025-09-13 운영 로그에 남은 스캐너 요청(GET /frontend/.env, /conf.php.swp)을 인증 없이 동시 300명이 15초간
// 쉬지 않고 보낸다(모두 401). 동시 처리 중인 요청이 Tomcat 워커 수 상한(기본 200)을 넘도록 하는 것이 목적이다.
// 부하가 끝난 뒤의 유휴 구간 관측은 rerun/run.sh 가 맡는다.
import http from 'k6/http';
import { check } from 'k6';

const PATHS = ['/frontend/.env', '/conf.php.swp'];

export const options = {
    scenarios: {
        warm_up: {
            executor: 'constant-vus', vus: 5, duration: '5s', gracefulStop: '1s',
            tags: { phase: 'warmup' },
        },
        burst: {
            executor: 'constant-vus', vus: 300, duration: '15s', startTime: '6s',
            tags: { phase: 'burst' },
        },
    },
    // 응답이 401 이어도 실패로 세지 않는다(스캐너 요청은 원래 거절되는 요청)
    thresholds: {
        'http_req_duration{phase:burst}': ['p(95)<60000'],
    },
};

http.setResponseCallback(http.expectedStatuses(401, 403, 404));

export default function () {
    const res = http.get(`http://localhost:8080${PATHS[__ITER % PATHS.length]}`);
    check(res, { 'rejected (401/403/404)': (r) => [401, 403, 404].includes(r.status) });
}
