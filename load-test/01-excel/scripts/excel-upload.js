// ① 엑셀 업로드 부하 테스트 (2026-10-27).
// 원래 스크립트(poc/global-cache-msa/k6Test/excel-upload.js)와 같은 50명 동시 업로드지만,
// S3 수정 후에는 엑셀의 캠페인 ID 가 업로드한 사용자 소유여야 하므로 사용자마다 자기 파일을 올린다.
// 파일 생성: python load-test/01-excel/data/generate.py
//
// 구간: 예열(5명, 10초) → 본 측정(50명, 30초). 결과는 본 측정 구간(phase:main)만 본다.
// 첫 업로드는 500행 INSERT, 같은 사용자의 다음 업로드부터는 500행 UPDATE 다.
import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    scenarios: {
        warm_up: {
            executor: 'constant-vus', vus: 5, duration: '10s', gracefulStop: '2s',
            tags: { phase: 'warmup' },
        },
        main: {
            executor: 'constant-vus', vus: 50, duration: '30s', startTime: '12s',
            tags: { phase: 'main' },
        },
    },
    thresholds: {
        'http_req_failed{phase:main}': ['rate<0.01'],
        'http_req_duration{phase:main}': ['p(95)<1000'], // 목표: 50명·500행에서 1초 이내
    },
};

// k6 는 첫 초기화(__VU==0)에서 연 파일만 열 수 있으므로 50개 파일을 모두 init 단계에서 읽는다(파일당 약 24KB)
const files = Array.from({ length: 50 }, (_, i) =>
    open(`../data/upload-user${String(i + 1).padStart(2, '0')}.xlsx`, 'b'));

export default function () {
    // 예열이 끝난 뒤 본 측정이 시작되므로 VU 는 최대 50개(1~50)이고, VU 번호가 곧 사용자 번호다
    const index = __VU - 1;
    const number = String(index + 1).padStart(2, '0');
    const res = http.post('http://localhost:8080/api/marginforcam/upload', {
        file: http.file(files[index], `upload-user${number}.xlsx`,
            'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'),
    }, {
        headers: { Authorization: `TestToken : GUTokenuser${number}` },
    });
    check(res, { 'status is 200': (r) => r.status === 200 });
    sleep(1);
}
