# Load Test (k6)

포트폴리오의 각 수치가 어떤 스크립트와 결과 캡처에서 나왔는지 찾아가기 위한 색인입니다.
MSA/글로벌 캐시 PoC의 부하 테스트는 [`poc/global-cache-msa/k6Test`](../poc/global-cache-msa/k6Test)에 따로 있습니다.

## 실행

```bash
cd infra && cp .env.example .env && docker compose up -d                       # MariaDB 10.11 (호스트 포트 DB_HOST_PORT, 기본 3306)
docker compose -f docker-compose.monitoring.yml up -d                           # Prometheus :9090, Grafana :3001 (GRAFANA_HOST_PORT)

# 1) 테이블 생성: bottleNeckTest 프로필로 한 번 기동 (ddl-auto: update)
cd ../apps/api-server
export DATASOURCE_URL=jdbc:mariadb://localhost:3306/grouup DATASOURCE_USERNAME=root DATASOURCE_PASSWORD=<.env 값>
# OAuth·JWT 관련 환경변수(KAKAO_*, GOOGLE_*, JWT_*, RE_DIRECT_URL_*)도 아무 값으로 채워야 기동된다
./gradlew bootRun --args='--spring.profiles.active=bottleNeckTest'

# 2) 데이터 적재 (300만 행, 로컬에서 약 45초)
docker exec -i grouup-mariadb mariadb -uroot -p<.env 값> grouup < load-test/02-segment-tree-cache/sql/seed-data.sql

# 3) 측정
k6 run load-test/02-segment-tree-cache/scripts/05-six-month-lazycache.js
```

스크립트는 `JwtTokenProviderStubImpl`의 테스트 토큰(`TestToken : GUTokenuser{n}`)을 씁니다. `JwtAuthFilterStub`이 이를 `user{n}@test.com`으로 바꿉니다.

- 호스트에 MySQL 등이 이미 3306을 쓰고 있으면 `.env`의 `DB_HOST_PORT`를 바꾸고 `DATASOURCE_URL`도 맞춥니다.
- Grafana에는 Prometheus 데이터 소스와 대시보드 두 개가 자동으로 등록됩니다(`infra/grafana`).
  - **Spring Boot 3.x Statistics**(Grafana.com ID 19004): 예전 캡처와 같은 대시보드입니다. HikariCP Acquire·Usage 패널은 기동 후 누적 평균(`sum / count`)입니다.
  - **GrouUp ② 세그먼트 트리 캐시 재측정**: 같은 지표의 최대값과 15초 구간 평균입니다.
- ② 재측정은 [`02-segment-tree-cache/rerun/run.sh`](02-segment-tree-cache/rerun/run.sh)로 돌립니다. 회차마다 서버를 새로 띄우고, 결과를 `results/2026-10-rerun/<이름>/`에 저장합니다. 요약 표는 `summarize.py`(회차별)와 `aggregate.py`(반복 회차 중앙값)로 만듭니다. Phase 4 풀 크기 튜닝(HikariCP = ioExecutor = N)은 [`pool-sweep.sh`](02-segment-tree-cache/rerun/pool-sweep.sh)로 돌리고 `python aggregate.py "" phase4-cp`로 요약합니다.

### 테스트 환경 (2026-10-27 재측정 환경)

예전 측정 환경(서버 사양, 데이터 분포)은 기록이 없습니다. 아래는 재측정에 쓰는 로컬 환경이며, 예전 수치와 직접 비교하지 않습니다.

| 항목 | 값 |
|---|---|
| 호스트 | Intel Core i3-1115G4 (2코어 4스레드), RAM 7.7GB, Windows 11 Home |
| 실행 위치 | api-server·k6는 호스트, MariaDB·Prometheus·Grafana는 Docker Desktop(VM 4 CPU, 3.8GB) |
| JDK | 17 |
| DB | MariaDB 10.11.19, 기본 설정(`innodb_buffer_pool_size` 128MB, `sql_mode`에 `ONLY_FULL_GROUP_BY` 없음) |
| 데이터 | [`seed-data.sql`](02-segment-tree-cache/sql/seed-data.sql): 회원 500명 × 캠페인 10개, keyword 3,000,000행(2025-06-01 ~ 12-31, 테이블 약 480MB). Light(1~400) 캠페인당 300행, Heavy(401~475) 1,200행, Outlier(476~500) 3,600행 |
| 인덱스 | 기본은 FK 인덱스 `(campaign_id)`만. 커버링 인덱스는 [`covering-index.sql`](02-segment-tree-cache/sql/covering-index.sql)로 켜고 끈다 |
| HikariCP pool | `bottleNeckTest` 프로필 30 |
| ioExecutor / cpuExecutor | 30 / 코어 수+1 (이 PC에서 5) |
| 모니터링 | Prometheus 5초 간격 수집, `hikaricp_connections_{active,pending,acquire_seconds,usage_seconds}` 수집 확인 |

> 데이터 분포는 원래 값이 남아 있지 않아 k6 스크립트의 사용자 구성(Light/Heavy/Outlier)에 맞춰 다시 정했습니다. 포트폴리오의 "6개월 약 300만 행"과 총량만 맞춥니다.

## ① 엑셀 — `01-excel/`

| 스크립트 | 엔드포인트 | 비고 |
|---|---|---|
| [excel-download.js](01-excel/scripts/excel-download.js) | `GET /api/marginforcam/downloadExcel` | 다운로드 부하. 업로드 시나리오는 [poc 쪽 excel-upload.js](../poc/global-cache-msa/k6Test/excel-upload.js) |

## ② 6개월 대시보드 조회 / 세그먼트 트리 캐시 — `02-segment-tree-cache/`

### 스크립트

| # | 스크립트 | 엔드포인트 | 시나리오 | 포트폴리오 단계 |
|---|---|---|---|---|
| 01 | [01-baseline.js](02-segment-tree-cache/scripts/01-baseline.js) | `totalAnalysisData` | warm-up 5VU 10s → 50VU 1m, 1개월 랜덤 범위 | 개선 전 |
| 02 | [02-querydsl-refactor.js](02-segment-tree-cache/scripts/02-querydsl-refactor.js) | `totalAnalysisData2` ※ | 동일 | QueryDSL 쿼리 통합(3회 → 1회) |
| 03 | [03-six-month-nocache.js](02-segment-tree-cache/scripts/03-six-month-nocache.js) | `totalAnalysisData3` | 6개월 랜덤 범위, 통계 리셋 후 측정 | 커버링 인덱스, 캐시 없음 |
| 04 | [04-six-month-single-user.js](02-segment-tree-cache/scripts/04-six-month-single-user.js) | `totalAnalysisData3` | 1VU 1m, 사용자 유형(Normal/Heavy/Outlier)별 | 사용자 유형별 비교 |
| 05 | [05-six-month-lazycache.js](02-segment-tree-cache/scripts/05-six-month-lazycache.js) | `totalAnalysisData4` | 03과 동일 + 캐시 | Lazy 세그먼트 트리 |
| 06 | [06-fixed-range-nocache.js](02-segment-tree-cache/scripts/06-fixed-range-nocache.js) | `totalAnalysisData3` | 고정 범위 2025-06-01 ~ 2025-12-30 | 캐시 없음 |
| 07 | [07-fixed-range-lazycache.js](02-segment-tree-cache/scripts/07-fixed-range-lazycache.js) | `totalAnalysisData4` | 고정 범위 | 캐시 |
| 08 | [08-drilldown-nocache.js](02-segment-tree-cache/scripts/08-drilldown-nocache.js) | `totalAnalysisData3` | 1명 첫 요청 → 50명 동시 1회 → 드릴다운 | 캐시 없음 |
| 09 | [09-drilldown-lazycache.js](02-segment-tree-cache/scripts/09-drilldown-lazycache.js) | `totalAnalysisData4` | 08과 동일, `cacheRate`로 hit율 수집 | 캐시 + 비동기 빌드 |

※ `totalAnalysisData2`는 이후 리팩터링에서 사라졌습니다. 재현하려면 해당 시점 커밋을 checkout해야 합니다.

### 결과 캡처

| 폴더 | 내용 |
|---|---|
| [results/query-refactor](02-segment-tree-cache/results/query-refactor) | QueryDSL 리팩터링 후 쿼리 수 |
| [results/single-user](02-segment-tree-cache/results/single-user) | 사용자 유형별 cache / nocache (heavy, normal, outlier) |
| [results/six-month](02-segment-tree-cache/results/six-month), [results/fixed-range](02-segment-tree-cache/results/fixed-range) | 6개월 랜덤 / 고정 범위 |
| [results/drilldown](02-segment-tree-cache/results/drilldown) | 드릴다운 nocache vs lazycache. 2026-02-02 스레드 분리·DB pool 조정, 2026-03-02 GC·메모리·DB pool 비교, 스레드 상태 |
| [results/phase-0](02-segment-tree-cache/results/phase-0) ~ [phase-4](02-segment-tree-cache/results/phase-4) | 비동기 빌드·자원 분리 단계별 k6 결과와 DB pool 패널. phase-4는 pool 20/30/40 비교. phase-1 캡처는 없음 |
| [results/thread-pool](02-segment-tree-cache/results/thread-pool) | Tomcat 스레드와 IO 스레드의 커넥션 경합 증거, 분리 전/후, 분리 후 pool 축소 |
| [results/2026-10-rerun](02-segment-tree-cache/results/2026-10-rerun) | 정합성 수정 후 로컬 재측정. 회차별 k6 요약·로그, Prometheus 지표, keyword 인덱스 상태. 해석은 [② 재측정](../docs/portfolio/02-segment-tree-cache.md#측정-환경과-원칙) |

> 캡처별 수치(P95, Acquire, Usage, Pending)는 [② 부록: 예전 측정 기록](../docs/portfolio/02-segment-tree-cache.md#부록-예전-측정-기록)에 정리했습니다.

## 삭제 API — `03-delete-api/`

| 스크립트 | 엔드포인트 | 결과 |
|---|---|---|
| [delete-test.js](03-delete-api/scripts/delete-test.js) | `GET getMyCampaigns`, `DELETE RefactoringDeleteCampaign` ※ | [before](03-delete-api/results/before.jpg), [after-refactor](03-delete-api/results/after-refactor.jpg) |

※ 현재 엔드포인트는 `DELETE /api/campaign/deleteCampaign`입니다(Facade 리팩터링 이후).

## 원본 파일명 매핑

원본 `k6Test/` 폴더의 파일명(공백·한글 포함)을 URL에 안전한 이름으로 바꿨습니다.

<details>
<summary>펼치기</summary>

| 원본 | 현재 |
|---|---|
| `load-test.js` | `01-excel/scripts/excel-download.js` |
| `getCampaignTotalData.js` | `02-segment-tree-cache/scripts/01-baseline.js` |
| `getCampaignTotalData_refactor.js` | `…/scripts/02-querydsl-refactor.js` |
| `getCampaignTotalDatat_sixMonth.js` | `…/scripts/03-six-month-nocache.js` |
| `getCampainTotalData_sixMonth_cache_singleUser.js` | `…/scripts/04-six-month-single-user.js` |
| `getCampaignTotalData_sixMonth_LazyCache.js` | `…/scripts/05-six-month-lazycache.js` |
| `campaignTotalDataNoRandomDateIn6Month/*_NoCache.js`, `*_LazyCache.js` | `…/scripts/06-…`, `07-…` |
| `campaignTotalDataDrillDownDateIN6month/*_noCache.js`, `*_LazyCache.js` | `…/scripts/08-…`, `09-…` |
| `campaignTotalDatat_singleUserType{Heavy,Normal,Outliner}_{Cache,nonCache}.jpg` | `…/results/single-user/{heavy,normal,outlier}-{cache,nocache}.jpg` |
| `campaignTotalDataDrillDownDateIN6month/Phase N/*` | `…/results/phase-N/*` |
| `campaignTotalDataDrillDownDateIN6month/2026-0x-02 *.jpg` | `…/results/drilldown/2026-0x-02-*.jpg` |
| `스레드 자원 경쟁 분리 전/후(+ 풀 사이즈 다운).jpg` | `…/results/thread-pool/{before-split,after-split,after-split-pool-down}.jpg` |
| `스레드 (간 자원) 경쟁 증거 (IO/메인 스레드 러닝).jpg` | `…/results/thread-pool/contention-{io,main}-thread-running.jpg` |
| `delete test 수행 결과..jpg`, `delete 리팩토링 결과.jpg` | `03-delete-api/results/{before,after-refactor}.jpg` |

</details>
