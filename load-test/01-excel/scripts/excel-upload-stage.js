// ① 엑셀 업로드 단계별 소요 시간 측정용 (2026-10-30). 업로드 API 한 건 안에서 어느 단계가 오래 걸리는지 보는 것이
// 목적이라 동시성은 뺀다. 기본은 1명이고 VUS 환경변수로 늘릴 수 있다(2026-10-30 결과는 1명).
//
// 예열: user41~45 가 10번 올려 JIT 를 데운다(집계에서 제외).
// 본 측정: user01~10 을 동시 사용자(VU 1개짜리 시나리오 VUS 개)가 나눠 맡고, 사용자마다 A → B → A 순서로 올린다.
//   첫 업로드는 100행 INSERT, 이후 두 번은 값이 바뀐 100행 UPDATE 다. 같은 파일을 다시 올리면 개선 전 구성(JPA 변경 감지)은
//   UPDATE 쿼리를 보내지 않으므로 가격만 다른 B 파일(data/generate.py)과 번갈아 올린다.
// 단계별 시간은 서버 로그(EXCEL_STAGE)에서 읽는다. 측정용 jar 는 rerun/stage-timing.py build 로 만든다.
import http from 'k6/http';
import { check, sleep } from 'k6';

const VUS = Number(__ENV.VUS || 1);
const USERS = 10;
const UPLOADS_PER_USER = 3;

// 동시 사용자마다 VU 1개짜리 시나리오를 따로 두고 SLOT 으로 맡을 사용자를 정한다.
// __VU 는 시나리오와 무관한 전체 번호라, 한 시나리오의 VU 번호로 사용자를 나누면 두 VU 가 같은 사용자를 맡을 수 있다.
const scenarios = {
    warm_up: {
        executor: 'per-vu-iterations', vus: 1, iterations: 10, exec: 'warmUp',
        tags: { phase: 'warmup' },
    },
};
for (let slot = 0; slot < VUS; slot++) {
    scenarios[`main_${slot}`] = {
        executor: 'per-vu-iterations', vus: 1, iterations: (USERS / VUS) * UPLOADS_PER_USER,
        startTime: '20s', maxDuration: '5m', exec: 'measure', env: { SLOT: String(slot) },
        tags: { phase: 'main' },
    };
}
export const options = { scenarios };

const pad = (n) => String(n).padStart(2, '0');
const files = {};
for (const n of [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 41, 42, 43, 44, 45]) {
    files[`${pad(n)}`] = open(`../data/upload-user${pad(n)}.xlsx`, 'b');
    files[`${pad(n)}-b`] = open(`../data/upload-user${pad(n)}-b.xlsx`, 'b');
}

function upload(number, variant) {
    const key = variant === 'b' ? `${number}-b` : number;
    const res = http.post('http://localhost:8080/api/marginforcam/upload', {
        file: http.file(files[key], `upload-user${key}.xlsx`,
            'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'),
    }, {
        headers: { Authorization: `TestToken : GUTokenuser${number}` },
    });
    check(res, { 'status is 200': (r) => r.status === 200 });
    sleep(0.5);
}

export function warmUp() {
    upload(pad(41 + (__ITER % 5)), __ITER < 5 ? 'a' : 'b');
}

export function measure() {
    // 슬롯 s 는 user s+1, s+1+VUS, … 를 맡아 사용자마다 A → B → A 로 올린다
    const slot = Number(__ENV.SLOT);
    const user = slot + Math.floor(__ITER / UPLOADS_PER_USER) * VUS + 1;
    upload(pad(user), __ITER % UPLOADS_PER_USER === 1 ? 'b' : 'a');
}
