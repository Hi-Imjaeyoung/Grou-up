import http from 'k6/http';
import { check, sleep } from 'k6';
import exec from 'k6/execution';

// -----------------------------------------------------------------------
// 1. 🏗️ 유저 데이터 생성 (User Data Generation)
//    - 전체 500명 (일반 400 + 헤비 75 + 아웃라이어 25)
// -----------------------------------------------------------------------
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

export const options = {
    scenarios: {
        // 1️.  웜업
        warm_up: {
            executor: 'constant-vus',
            vus: 5,
            duration: '1m',
            exec: 'getDashboard',
            gracefulStop: '0s',
        },
        // [시나리오 A] 배경 트래픽 (50 VUs)
        background_traffic: {
            executor: 'constant-vus',
            vus: 50,
            startTime: '1m',
            duration: '40s',
            exec: 'getDashboard',
        },

        // // [시나리오 B] 스파이크 (5 VUs) 
        excel_spike: {
            executor: 'shared-iterations',
            vus: 5,
            iterations: 5,
            startTime: '1m10s',
            exec: 'deleteCampaign',
        },
    },
    thresholds: {
        'http_req_failed': ['rate<0.01'],
        'http_req_duration{scenario:background_traffic}': ['p(95)<1000'],
        'http_req_duration{scenario:excel_spike}': ['p(95)<1000'],
    },
};


function getBackgroundUser() {
    // 1. 현재 VU가 50명 중 몇 번째 녀석인지 계산 (0 ~ 49)
    //    (idInTest는 1부터 시작하고 계속 증가하므로, 50으로 나눈 나머지를 사용)
    const slot = (exec.vu.idInTest) % 50;

    // 2. 슬롯 번호에 따라 역할 배정
    if (slot <= 40) {
        // [0 ~ 39] (40명, 80%) -> 일반 유저 (Light)
        // users 배열의 0번 인덱스부터 차례대로 매핑
        return users[slot];
    }
    else if (slot <= 48) {
        // [40 ~ 47] (8명, 16%) -> 헤비 유저 (Heavy)
        // users 배열의 Heavy 시작점(400)부터 매핑
        // slot이 40일 때 400번 유저를 가져와야 함 -> 400 + (slot - 40)
        return users[400 + (slot - 40)];
    }
    else {
        // [48 ~ 49] (2명, 4%) -> 아웃라이어 유저 (Outlier)
        // users 배열의 Outlier 시작점(475)부터 매핑
        return users[475 + (slot - 48)];
    }
}

function getSpikeUser() {
    const spikeIndex = exec.scenario.iterationInTest;

    if (spikeIndex < 3) {
        // 0, 1, 2 (3명) -> LIGHT
        return users[spikeIndex];
    } else if (spikeIndex === 3) {
        // 3 (1명) -> HEAVY
        return users[403];
    } else {
        // 4 (1명) -> OUTLIER
        return users[480];
    }
}

// [배경 트래픽 실행]
export function getDashboard() {
    const user = getBackgroundUser();
    const url = `http://localhost:8080/api/campaign/getMyCampaigns`;
    const params = {
        headers: {
            'Authorization': `${user.token}`,
            'Content-Type': 'application/json',
        },
        tags: { my_tag: 'dashboard' },
    };

    const res = http.get(url, params);
    check(res, { 'dashboard 200 OK': (r) => r.status === 200 });
    sleep(Math.random() * 10 + 3);
}

// [스파이크 실행]
export function deleteCampaign() {
    const user = getSpikeUser();

    console.log(`🚀 [Spike] VU:${exec.vu.idInTest} is hitting Excel API with User Type: ${user.type}, UserID: ${user.id}`);

    const payload = JSON.stringify(
        [(user.id * 10) - 1, (user.id * 10) - 2, (user.id * 10) - 3]
    );

    const url = `http://localhost:8080/api/campaign/RefactoringDeleteCampaign`;
    const params = {
        headers: {
            'Authorization': `${user.token}`,
            'Content-Type': 'application/json',
        },
        tags: { my_tag: 'excel' },
    };

    const res = http.del(url, payload, params);
    check(res, { 'excel 200 OK': (r) => r.status === 200 });
}