import http from 'k6/http';
import { check, sleep } from 'k6';
import exec from 'k6/execution';

// 1. 유저 데이터 생성 (파레토 법칙)
const TOTAL_USERS = 500;
const RATIO = { light: 0.80, heavy: 0.15, outlier: 0.05 };

function generateUsers() {
    const users = [];

    const lightCount = TOTAL_USERS * RATIO.light;     // 400명
    const heavyCount = TOTAL_USERS * RATIO.heavy;     // 75명
    const outlierCount = TOTAL_USERS * RATIO.outlier; // 25명

    const lightEnd = lightCount;                        // 1 ~ 400
    const heavyEnd = lightCount + heavyCount;           // 401 ~ 475
    const outlierEnd = TOTAL_USERS;                     // 476 ~ 500

    // 1. 일반 유저 (1 ~ 400)
    for (let i = 1; i <= lightEnd; i++) {
        const idStr = i.toString().padStart(2, '0');
        users.push({ id: i, type: 'LIGHT', token: `TestToken : GUTokenuser${idStr}` });
    }

    // 2. 헤비 유저 (401 ~ 475)
    for (let i = lightEnd + 1; i <= heavyEnd; i++) {
        users.push({ id: i, type: 'HEAVY', token: `TestToken : GUTokenuser${i}` });
    }

    // 3. 아웃라이어 (476 ~ 500)
    for (let i = heavyEnd + 1; i <= outlierEnd; i++) {
        users.push({ id: i, type: 'OUTLIER', token: `TestToken : GUTokenuser${i}` });
    }
    return users;
}

const users = generateUsers();

// 2. VU 번호에 따른 유저 할당 (결정적 할당)
function getUserForVU() {
    const slot = (exec.vu.idInTest - 1) % 50;

    if (slot < 40) {
        return users[slot]; // Light (1~40번 유저 사용)
    } else if (slot < 48) {
        return users[400 + (slot - 40)]; // Heavy
    } else {
        return users[475 + (slot - 48)]; // Outlier
    }
}

function getRandomSixMonthsRange() {
    const year = "2025";

    const startMonthInt = Math.random() < 0.5 ? 6 : 7;
    const endMonthInt = startMonthInt + 5;

    const startMonthStr = String(startMonthInt).padStart(2, '0');
    const endMonthStr = String(endMonthInt).padStart(2, '0');

    const d1 = Math.floor(Math.random() * 28) + 1;
    const d2 = Math.floor(Math.random() * 28) + 1;

    const day1Str = String(d1).padStart(2, '0');
    const day2Str = String(d2).padStart(2, '0');

    return {
        startDate: `${year}-${startMonthStr}-${day1Str}`,
        endDate: `${year}-${endMonthStr}-${day2Str}`
    };
}

// 4. 테스트 옵션
export const options = {
    scenarios: {
        warm_up: {
            executor: 'constant-vus',
            vus: 5,
            duration: '10s',
            exec: 'warmUpLogic',
            gracefulStop: '5s',
        },
        reset_stats: {
            executor: 'per-vu-iterations',
            vus: 1,              // 딱 1명만 보내면 됨
            iterations: 1,       // 딱 1번만 실행
            startTime: '20s',    // 워밍업 끝나고 10초 뒤 실행 (안전하게)
            exec: 'resetStatsLogic',
        },
        scenario_refactored: {
            executor: 'constant-vus',
            vus: 50,
            duration: '1m',
            startTime: '30s',
            exec: 'refactoringGetCampaignTotalData',
            tags: { api_version: 'v2_dsl' },
        },
    },
    thresholds: {
        'http_req_failed': ['rate<0.01'],
        'http_req_duration{api_version:v2_dsl}': ['p(95)<1000'], // 목표: 1초 미만 (안전하게)
    },
};

// 5. 실행 로직
export function warmUpLogic() {
    // 웜업은 7월 전체로 가볍게
    const url = `http://localhost:8080/api/campaign/getMyCampaigns`;
    const params = { headers: { 'Content-Type': 'application/json', 'Authorization': users[0].token } };
    http.get(url, params);
    sleep(1);
}

// ★ 통계 초기화 함수
export function resetStatsLogic() {
    // 1. 관리자급 유저 토큰 사용 (users[0] 추천)
    const adminToken = users[0].token;

    // 2. 초기화 API 호출
    const url = 'http://localhost:8080/test/stats/reset';
    const params = {
        headers: {
            'Content-Type': 'application/json',
            'Authorization': adminToken
        }
    };

    const res = http.post(url, null, params);

    // 3. 로그 출력 (K6 콘솔에서 확인용)
    if (res.status === 200) {
        console.log('✅ [Reset Scenario] Hibernate Statistics Reset Successful!');
    } else {
        console.error(`❌ [Reset Scenario] Failed to reset stats. Status: ${res.status}`);
    }
}

export function refactoringGetCampaignTotalData() {
    const user = getUserForVU();
    // const dates = getRandomSixMonthsRange(); // 랜덤 날짜 생성

    // URL에 쿼리 파라미터 추가
    const url = `http://localhost:8080/api/campaign/totalAnalysisData3?start=2025-06-01&end=2025-12-30`;

    const params = {
        headers: {
            'Authorization': user.token,
            'Content-Type': 'application/json',
        },
        tags: { api_version: 'v2_dsl' },
    };

    const res = http.get(url, params);

    check(res, { 'V2 status is 200': (r) => r.status === 200 });
    sleep(1);
}