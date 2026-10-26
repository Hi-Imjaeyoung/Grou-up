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

// ✨ 3. [New] 2025년 7월 내 랜덤 기간 생성기
function getRandomDateInJuly() {
    const year = "2025";
    const month = "07";

    // 1일부터 31일 사이 랜덤 숫자 2개 뽑기
    const d1 = Math.floor(Math.random() * 31) + 1;
    const d2 = Math.floor(Math.random() * 31) + 1;

    // 둘 중 작은 걸 시작일, 큰 걸 종료일로 (자동 정렬)
    const startDay = Math.min(d1, d2);
    const endDay = Math.max(d1, d2);

    // 두 자리 문자열 포맷팅 (예: 5 -> "05")
    const startStr = String(startDay).padStart(2, '0');
    const endStr = String(endDay).padStart(2, '0');

    return {
        startDate: `${year}-${month}-${startStr}`,
        endDate: `${year}-${month}-${endStr}`
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

export function refactoringGetCampaignTotalData() {
    const user = getUserForVU();
    const dates = getRandomDateInJuly(); // 랜덤 날짜 생성

    // URL에 쿼리 파라미터 추가
    const url = `http://localhost:8080/api/campaign/totalAnalysisData2?start=${dates.startDate}&end=${dates.endDate}`;

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