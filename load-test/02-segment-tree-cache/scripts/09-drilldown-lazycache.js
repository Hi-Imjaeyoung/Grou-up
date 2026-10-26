import http from 'k6/http';
import { check, sleep } from 'k6';
import exec from 'k6/execution';

// -----------------------------------------------------------------------
// 1. 유저 데이터 생성 (기존 로직 유지)
// -----------------------------------------------------------------------
const TOTAL_USERS = 500;
const RATIO = { light: 0.80, heavy: 0.15, outlier: 0.05 };

function generateUsers() {
    const users = [];
    const lightCount = TOTAL_USERS * RATIO.light;
    const heavyCount = TOTAL_USERS * RATIO.heavy;
    const outlierCount = TOTAL_USERS * RATIO.outlier;

    const lightEnd = lightCount;
    const heavyEnd = lightCount + heavyCount;
    const outlierEnd = TOTAL_USERS;

    for (let i = 1; i <= lightEnd; i++) {
        const idStr = i.toString().padStart(2, '0');
        users.push({ id: i, type: 'LIGHT', token: `TestToken : GUTokenuser${idStr}` });
    }
    for (let i = lightEnd + 1; i <= heavyEnd; i++) {
        users.push({ id: i, type: 'HEAVY', token: `TestToken : GUTokenuser${i}` });
    }
    for (let i = heavyEnd + 1; i <= outlierEnd; i++) {
        users.push({ id: i, type: 'OUTLIER', token: `TestToken : GUTokenuser${i}` });
    }
    return users;
}

const users = generateUsers();

function getUserForVU() {
    const uniqueIter = exec.scenario.iterationInTest;
    const batchSize = 50;
    const batchIndex = Math.floor(uniqueIter / batchSize);
    const slot = uniqueIter % batchSize;
    if (slot < 40) {
        const index = (batchIndex * 40 + slot) % 400;
        return users[index];
    }
    else if (slot < 48) {
        const offset = slot - 40;
        const index = 400 + ((batchIndex * 8 + offset) % 75);
        return users[index];
    }
    else {
        const offset = slot - 48;
        const index = 475 + ((batchIndex * 2 + offset) % 25);
        return users[index];
    }
}

const BASE_URL = 'http://localhost:8080';
const MONITOR_URL = `${BASE_URL}/api/campaign`;

// -----------------------------------------------------------------------
// 2. 시나리오 설정 (시간차 공격!)
// -----------------------------------------------------------------------
export const options = {
    scenarios: {
        // [0] 워밍업 (DB 커넥션 풀 등 예열)
        warm_up: {
            executor: 'constant-vus',
            vus: 5, duration: '5s',
            exec: 'warmUpLogic',
            gracefulStop: '0s',
        },

        // [Init] 메인 테스트 전 통계 0으로 초기화
        init_reset: {
            executor: 'per-vu-iterations',
            vus: 1, iterations: 1,
            startTime: '6s', // 워밍업 후 실행
            exec: 'resetStatsLogic',
        },

        // -------------------------------------------------
        // [Step 1] 하반기 통조회 (Base Camp)
        // -------------------------------------------------
        step_1_base: {
            executor: 'per-vu-iterations',
            vus: 50, iterations: 1, // 50명이 1번씩 요청
            startTime: '20s',        // 초기화 후 시작
            exec: 'runStep1',
            tags: { step: '1.Base_Half_Year' },
        },
        check_1: {
            executor: 'per-vu-iterations',
            vus: 1, iterations: 1,
            startTime: '30s',       // Step 1 끝나고 검증 (여유있게 10초 뒤)
            exec: 'checkAndResetStep1',
        },

        // -------------------------------------------------
        // [Step 2] 3분기 조회 (Subset) -> 100% 히트 예상
        // -------------------------------------------------
        step_2_subset: {
            executor: 'per-vu-iterations',
            vus: 50, iterations: 1,
            startTime: '40s',
            exec: 'runStep2',
            tags: { step: '2.Subset_Q3' },
        },
        check_2: {
            executor: 'per-vu-iterations',
            vus: 1, iterations: 1,
            startTime: '50s',
            exec: 'checkAndResetStep2',
        },

        // -------------------------------------------------
        // [Step 3] 8월 조회 (Deep Dive) -> 100% 히트 예상
        // -------------------------------------------------
        step_3_deep: {
            executor: 'per-vu-iterations',
            vus: 50, iterations: 1,
            startTime: '60s',
            exec: 'runStep3',
            tags: { step: '3.Subset_August' },
        },
        check_3: {
            executor: 'per-vu-iterations',
            vus: 1, iterations: 1,
            startTime: '70s',
            exec: 'checkAndResetStep3',
        },

        // -------------------------------------------------
        // [Step 4] 6월~7월 (Overlap) -> 50% 히트 예상
        // -------------------------------------------------
        step_4_overlap: {
            executor: 'per-vu-iterations',
            vus: 50, iterations: 1,
            startTime: '80s',
            exec: 'runStep4',
            tags: { step: '4.Overlap_June_July' },
        },
        check_4: {
            executor: 'per-vu-iterations',
            vus: 1, iterations: 1,
            startTime: '90s',
            exec: 'checkAndResetStep4',
        },
    },
    thresholds: {
        // 1. 전체 글로벌 통계 (기본)
        'http_req_failed': ['rate<0.01'], // 에러율 1% 미만

        // 2. [Step 1] 하반기 통조회 전용 통계
        // 문법: '지표이름{태그키:태그값}': ['조건']
        'http_req_duration{step:1.Base_Half_Year}': ['p(95)<1000'], // Step 1은 DB 조회라 5초 허용
        'http_reqs{step:1.Base_Half_Year}': ['count>=50'],          // 요청이 50개 찍혔는지 확인

        // 3. [Step 2] 3분기 (캐시 히트 예상)
        'http_req_duration{step:2.Subset_Q3}': ['p(95)<500'],       // 캐시니까 0.2초 안에 끝나야 함

        // 4. [Step 3] 8월 (캐시 히트 예상)
        'http_req_duration{step:3.Subset_August}': ['p(95)<500'],

        // 5. [Step 4] 6~7월 (반반)
        'http_req_duration{step:4.Overlap_June_July}': ['p(95)<1000'],
    },
};


// 공통 API 호출 헬퍼
function callApi(start, end) {
    const user = getUserForVU();
    const url = `${BASE_URL}/api/campaign/totalAnalysisData4?start=${start}&end=${end}`;
    // console.log(user.token);
    const params = {
        headers: {
            'Authorization': user.token,
            'Content-Type': 'application/json',
        },
        tags: { api_version: 'v2_dsl' },
    };

    const res = http.get(url, params);
    check(res, { 'status is 200': (r) => r.status === 200 });
}

// 공통 검증 & 리셋 헬퍼
function verifyAndReset(stepName, expectedMsg) {
    // 1. 통계 조회
    const params = { headers: { 'Content-Type': 'application/json', 'Authorization': users[0].token } };
    const res = http.get(`${MONITOR_URL}/cacheRate`, params);

    if (res.status !== 200) {
        console.error(`❌ [${stepName}] 통계 조회 실패!`);
        return;
    }

    const stats = res.json();

    // 2. 콘솔 출력
    console.log(`\n==================================================`);
    console.log(`📢 [결과 검증] ${stepName}`);
    console.log(`   - Total Requests : ${stats.total}`);
    console.log(`   - Cache Hits     : ${stats.hit}`);
    console.log(`   - Hit miss       : ${stats.miss} (예상: ${expectedMsg})`);
    console.log(`==================================================\n`);

    // 3. 통계 리셋 (다음 스텝을 위해)
    const resetUrl = `${MONITOR_URL}/cacheReset`;
    http.post(resetUrl, null, params);
}


export function warmUpLogic() {
    const url = `${BASE_URL}/api/campaign/getMyCampaigns`;
    const params = { headers: { 'Content-Type': 'application/json', 'Authorization': users[0].token } };
    http.get(url, params);
    sleep(1);
}

export function resetStatsLogic() {
    const resetUrl = `${MONITOR_URL}/cacheReset`;
    const params = { headers: { 'Content-Type': 'application/json', 'Authorization': users[0].token } };
    http.post(resetUrl, null, params);
    console.log("🔄 초기 리셋 완료");
}

// Step 1: 하반기 전체 (DB 부하 발생 예상)
export function runStep1() { callApi('2025-07-01', '2025-12-31'); }
export function checkAndResetStep1() { verifyAndReset("Step 1. 하반기 통조회", "Low Hit (초기 적재)"); }

// Step 2: 3분기 (100% 캐시 히트 예상)
export function runStep2() { callApi('2025-07-01', '2025-09-30'); }
export function checkAndResetStep2() { verifyAndReset("Step 2. 3분기 (Subset)", "100% Hit"); }

// Step 3: 8월 (100% 캐시 히트 예상)
export function runStep3() { callApi('2025-08-01', '2025-08-31'); }
export function checkAndResetStep3() { verifyAndReset("Step 3. 8월 (Deep Dive)", "100% Hit"); }

// Step 4: 6~7월 (50% 캐시 히트 예상)
export function runStep4() { callApi('2025-06-01', '2025-07-31'); }
export function checkAndResetStep4() { verifyAndReset("Step 4. 6~7월 (Overlap)", "~50% Hit"); }