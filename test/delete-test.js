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

    // 1. [구간 계산] 각 그룹이 끝나는 번호를 미리 계산하자 (헷갈리지 않게!)
    const lightEnd = lightCount;                        // 1 ~ 400
    const heavyEnd = lightCount + heavyCount;           // 401 ~ 475
    const outlierEnd = TOTAL_USERS;                     // 476 ~ 500

    // 1. 일반 유저 (1 ~ 400)
    for (let i = 1; i <= lightEnd; i++) {
        // ✨ padStart(2, '0'): "1" -> "01", "10" -> "10" (자동으로 0 붙여줌)
        const idStr = i.toString().padStart(2, '0');
        users.push({ id: i, type: 'LIGHT', token: `TestToken : GUTokenuser${idStr}` });
    }

    // 2. 헤비 유저 (401 ~ 475)
    // ✨ i는 401부터 시작해서 475까지!
    for (let i = lightEnd + 1; i <= heavyEnd; i++) {
        users.push({ id: i, type: 'HEAVY', token: `TestToken : GUTokenuser${i}` });
    }

    // 3. 아웃라이어 (476 ~ 500)
    // ✨ i는 476부터 시작해서 500까지!
    for (let i = heavyEnd + 1; i <= outlierEnd; i++) {
        users.push({ id: i, type: 'OUTLIER', token: `TestToken : GUTokenuser${i}` });
    }

    return users;
}

const users = generateUsers(); // 500명의 유저 리스트 생성 완료!

// -----------------------------------------------------------------------
// 2. ⚙️ k6 옵션 설정 (Scenarios)
// -----------------------------------------------------------------------
export const options = {
    scenarios: {
        // [시나리오 A] 배경 트래픽 (50 VUs)
        // background_traffic: {
        //     executor: 'constant-vus',
        //     vus: 50,
        //     duration: '20s',
        //     exec: 'getDashboard', // -> getDashboard 함수 실행
        // },

        // // [시나리오 B] 스파이크 (5 VUs) -> 10초 뒤에 딱 1번 실행
        excel_spike: {
            executor: 'per-vu-iterations',
            vus: 5,
            iterations: 1,
            startTime: '10s',
            exec: 'deleteCampaign', // -> downloadExcel 함수 실행
        },
    },
    thresholds: {
        'http_req_failed': ['rate<0.01'],
        'http_req_duration{scenario:background_traffic}': ['p(95)<1000'],
        'http_req_duration{scenario:excel_spike}': ['p(95)<1000'],
    },
};

// -----------------------------------------------------------------------
// 3. 🎯 유저 선택 헬퍼 함수 (User Picker Logic)
// -----------------------------------------------------------------------

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

// [Helper B] 스파이크용: 5명을 3:1:1로 정확하게 지정 선택
function getSpikeUser() {
    // exec.vu.idInTest는 전체 테스트에서 유니크한 VU 번호야.
    // 배경 트래픽이 50명이니까, 스파이크 VU들의 번호는 51번부터 시작할 거야. (k6 할당 방식에 따라 다를 수 있음)
    // 안전하게 "시나리오 내의 인덱스"를 계산하는 게 좋아.

    // 하지만 per-vu-iterations에서는 간단하게 vu.idInInstance 등을 쓰기 까다로울 수 있어.
    // 가장 확실한 방법: (VU ID) % 5 를 이용해 역할을 나눈다.

    const spikeIndex = (exec.vu.idInTest) % 5;

    if (spikeIndex < 3) {
        // 0, 1, 2 (3명) -> LIGHT 유저 (0~399번 인덱스 중 하나)
        return users[spikeIndex]; // 그냥 앞쪽 0,1,2번 유저 사용
    } else if (spikeIndex === 3) {
        // 3 (1명) -> HEAVY 유저 (400번 인덱스 사용)
        return users[400];
    } else {
        // 4 (1명) -> OUTLIER 유저 (475번 인덱스 사용)
        return users[475];
    }
}

// -----------------------------------------------------------------------
// 4. 🏃‍♂️ 실행 함수 (Main Functions)
// -----------------------------------------------------------------------

// [배경 트래픽 실행]
export function getDashboard() {
    const user = getBackgroundUser();

    // (로그 찍어서 확인해보면 좋아 - 나중에 주석 처리)
    // console.log(`[Background] VU:${exec.vu.idInTest} used User Type: ${user.token}`);

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
    // ✨ [수정] 3초 ~ 7초 사이로 랜덤하게 휴식 (평균 5초)
    // Math.random() * 4  => 0.0 ~ 3.99...
    // + 3                => 3.0 ~ 6.99...
    sleep(Math.random() * 10 + 3);
}

// [스파이크 실행]
export function deleteCampaign() {
    const user = getSpikeUser();

    // ✨ 여기가 핵심! 우리가 원하는 대로 3:1:1로 들어가는지 로그로 확인!
    console.log(`🚀 [Spike] VU:${exec.vu.idInTest} is hitting Excel API with User Type: ${user.type}, UserID: ${user.id}`);

    const payload = JSON.stringify(
        [(user.id * 10) - 7, (user.id * 10) - 8, (user.id * 10) - 9]
    );

    const url = `http://localhost:8080/api/campaign/deleteCampaign`;
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