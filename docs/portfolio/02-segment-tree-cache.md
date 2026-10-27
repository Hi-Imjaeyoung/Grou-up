# ② 캐시를 적용했는데 왜 첫 요청은 더 느려졌을까

> 대시보드 조회 기간을 6개월로 늘린 뒤의 DB 병목을 QueryDSL과 커버링 인덱스로 줄였습니다. 이어서 반복되는 구간 조회를 세그먼트 트리 캐시로 처리했고, 그 과정에서 생긴 Cold Start와 DB 커넥션 경합을 비동기 빌드와 스레드 풀 분리로 해결했습니다.

> **주의:** 2026-10-26 분석에서 캐시 정합성 결함(C1~C9)을 발견해 모두 고쳤습니다([known-issues](../known-issues.md), #9~#19). 아래 "측정 기록"의 2026-01~02 수치는 **수정 전 코드·기록 없는 서버**에서 잰 값입니다. 수정 후 로컬 PC에서 다시 잰 결과는 [재측정 (2026-10-27)](#재측정-2026-10-27)에 따로 정리했습니다. 두 결과는 환경이 달라 직접 비교하지 않습니다.

| 항목 | 내용 |
|---|---|
| 데이터 | 6개월 더미 데이터 약 300만 행 (MariaDB) |
| 엔드포인트 | `GET /api/campaign/totalAnalysisData3`(캐시 없음), `totalAnalysisData4`(Lazy 세그먼트 트리) |
| 테스트 환경 | 예전 측정: 서버 사양 기록 없음. 재측정: [load-test/README.md](../../load-test/README.md#테스트-환경-2026-10-27-재측정-환경) (i3-1115G4 노트북, MariaDB 10.11) |

## 근거 맵

| 주장 | 코드 | k6 스크립트 | 결과 |
|---|---|---|---|
| QueryDSL로 DB I/O 3회 → 1회 | [`KeywordRepositoryImpl`](../../apps/api-server/src/main/java/growup/spring/springserver/keyword/repository/KeywordRepositoryImpl.java) | [01-baseline](../../load-test/02-segment-tree-cache/scripts/01-baseline.js), [02-querydsl-refactor](../../load-test/02-segment-tree-cache/scripts/02-querydsl-refactor.js) | [query-count](../../load-test/02-segment-tree-cache/results/query-refactor/query-count.jpg) |
| 커버링 인덱스로 P95 2.97s → 877ms (재측정 463ms → 273ms) | [`Keyword`](../../apps/api-server/src/main/java/growup/spring/springserver/keyword/domain/Keyword.java) `@Index`, [전환 SQL](../../load-test/02-segment-tree-cache/sql/covering-index.sql) (원래 DDL이 없어 재설계, [O4](../known-issues.md)) | [03-six-month-nocache](../../load-test/02-segment-tree-cache/scripts/03-six-month-nocache.js) | [재측정](../../load-test/02-segment-tree-cache/results/2026-10-rerun) |
| 세그먼트 트리 캐시 | [`LazySegmentTreeService`](../../apps/api-server/src/main/java/growup/spring/springserver/global/cache/LazySegmentTreeService.java) | [05](../../load-test/02-segment-tree-cache/scripts/05-six-month-lazycache.js), [07](../../load-test/02-segment-tree-cache/scripts/07-fixed-range-lazycache.js) | [six-month](../../load-test/02-segment-tree-cache/results/six-month/lazycache.jpg), [fixed-range](../../load-test/02-segment-tree-cache/results/fixed-range) |
| Drill-down 캐시 적중, 첫 요청 최대 2배 지연 | 위와 같음 | [08](../../load-test/02-segment-tree-cache/scripts/08-drilldown-nocache.js), [09](../../load-test/02-segment-tree-cache/scripts/09-drilldown-lazycache.js) | [drilldown](../../load-test/02-segment-tree-cache/results/drilldown) |
| GC·메모리 비교 | — | 08, 09 | [gc-pause](../../load-test/02-segment-tree-cache/results/drilldown/2026-03-02-nocache-vs-cache-gc-pause.jpg), [memory](../../load-test/02-segment-tree-cache/results/drilldown/2026-03-02-nocache-vs-cache-memory.jpg) |
| 비동기 빌드 (Spring Event + CompletableFuture, 500ms 지연) | [`CacheBuildEventListener`](../../apps/api-server/src/main/java/growup/spring/springserver/global/listener/CacheBuildEventListener.java), [`CampaignTotalDataFacade`](../../apps/api-server/src/main/java/growup/spring/springserver/campaign/facade/CampaignTotalDataFacade.java) | 09 | [phase-0 ~ 3](../../load-test/02-segment-tree-cache/results) |
| IO/CPU 스레드 풀 분리, 커넥션 경합 해소 | [`ThreadPoolConfig`](../../apps/api-server/src/main/java/growup/spring/springserver/global/config/ThreadPoolConfig.java), [`application-bottleNeckTest.yml`](../../apps/api-server/src/main/resources/application-bottleNeckTest.yml) | 09 | [thread-pool](../../load-test/02-segment-tree-cache/results/thread-pool), [phase-4 pool 20/30/40](../../load-test/02-segment-tree-cache/results/phase-4) |
| 부분 업데이트와 전체 재빌드 임계값 | [`CampaignDeleteDto`](../../apps/api-server/src/main/java/growup/spring/springserver/campaign/dto/CampaignDeleteDto.java), [`TreeCpuBenchmarkTest`](../../apps/api-server/src/test/java/growup/spring/springserver/global/cache/TreeCpuBenchmarkTest.java) | — | [아래 표](#무효화와-정합성) |
| 단순 대안(선형 합산) 비교, 트리 메모리 | [`RangeQueryBenchmarkTest`](../../apps/api-server/src/test/java/growup/spring/springserver/global/cache/RangeQueryBenchmarkTest.java), [`SegmentTreeMemoryTest`](../../apps/api-server/src/test/java/growup/spring/springserver/global/cache/SegmentTreeMemoryTest.java) | — | [아래 표](#단순-대안과-비교-2026-10-26-jmh) |

## 병목 1: 6개월 조회에서 DB 조회 자체가 문제

- **원인:**
  - 멤버 → 캠페인 → 일별 데이터 순서로 나눠 조회한 뒤 애플리케이션에서 조합하고 있었습니다. 같은 통계를 위해 불필요한 I/O가 생겼습니다.
  - 실행 계획을 보면 Full Scan은 없었지만, 집계 컬럼(`adCost`, `adSales`)을 읽기 위한 Key Lookup이 발생했습니다.

| 조치 | P95 | TPS | Connection Acquire max |
|---|---|---|---|
| 개선 전 | — | 3.87/s | 6s |
| QueryDSL로 조회 통합 (I/O 3 → 1회) | 2.97s | 11.06/s | 2s |
| 커버링 인덱스 | **877ms** | **23.45/s** | 500ms |

재측정(2026-10-27, 03 시나리오 1회씩): 인덱스 없음(FK 인덱스만) P95 **463ms** → 커버링 인덱스 **273ms**(41% 감소). `EXPLAIN`에서 `keyword` 접근이 `Using index`로 바뀌는 것을 확인했습니다([전환 SQL](../../load-test/02-segment-tree-cache/sql/covering-index.sql)). 데이터·서버가 달라 예전 절대값(2.97s → 877ms)과는 비교하지 않습니다.

## 병목 2: 반복되는 세부 구간 조회

6개월 전체를 본 뒤 특정 월이나 분기로 범위를 좁히는 Drill-down 패턴을 예상했습니다.

| 구조 | 구간 합 | MAX/MIN | 수정 | 메모리 |
|---|---|---|---|---|
| Raw 데이터 + 선형 합산 | O(N) | O(N) | O(1) | N |
| 누적합 | O(1) | 불가 | O(N) | N |
| **세그먼트 트리** | O(log N) | O(log N) | O(log N) | ~2N(반복형) / 4N(재귀형) |

- **Lazy Loading의 한계:** 상위 구간을 조회한 뒤 하위 구간을 조회하면 리프 노드가 비어 있어 DB를 다시 조회했습니다. 그래서 **Range-based Eager Loading**으로 바꿨습니다.
- **결과:**
  - 이미 조회한 구간의 Drill-down은 DB 접근 0회, Cache Hit 100%.
  - 대신 **첫 요청은 캐시가 없을 때보다 최대 2배 느렸습니다**(트리를 동기로 구성하기 때문). 2026-01-28 측정에서 6개월 전체 조회(Base 단계) P95가 캐시 없음 2.76s, 캐시 5.12s로 1.86배였습니다([nocache](../../load-test/02-segment-tree-cache/results/drilldown/nocache-thresholds.jpg), [lazycache](../../load-test/02-segment-tree-cache/results/drilldown/lazycache-refactor.jpg)).

### 단순 대안과 비교 (2026-10-26 JMH)

일별 데이터만 캐싱하고 구간을 선형으로 더하는 방식과 비교했습니다. 데이터는 1년 × 광고 유형 3개이고, 메모리에 올린 상태에서 조회 비용만 쟀습니다([`RangeQueryBenchmarkTest`](../../apps/api-server/src/test/java/growup/spring/springserver/global/cache/RangeQueryBenchmarkTest.java), 로컬 4코어, JDK 17).

| 조회 구간 | 선형 합산 | 세그먼트 트리 |
|---|---|---|
| 7일 | 0.46µs | 1.65µs |
| 30일 | 1.98µs | 2.05µs |
| 90일 | 5.85µs | 2.31µs |
| 180일 | 11.55µs | 2.27µs |
| 365일 | 23.71µs | 0.09µs (루트 노드 1개) |

- 6개월 조회에서 트리가 약 5배 빠르지만 **두 방식 모두 마이크로초 단위**입니다. 요청 한 번의 DB 조회(수백 ms)와 비교하면 차이가 거의 없습니다.
- 캐시로 얻은 이득은 대부분 **DB를 다시 조회하지 않는 것**에서 나옵니다. 일별 캐시로도 같은 이득을 얻을 수 있습니다.
- 트리는 MAX/MIN 같은 구간 질의로 확장할 때와, 구간이 연 단위 이상으로 길어질 때 의미가 있습니다.

## 병목 3: 트리 생성 비용

1. **빌드 비동기화 (Phase 1).** 트리 생성이 끝날 때까지 Tomcat 스레드가 기다려 응답이 늦었습니다. 흐름을 DB 조회 → 트리 빌드 → 응답에서 DB 조회 → 응답 + 비동기 빌드로 바꿨습니다.
   - `CompletableFuture`로 트리 생성을 백그라운드에서 실행하고, 요청 스레드는 4ms 만에 반환됩니다.
   - Kafka/MQ는 단일 서버에서 과하다고 판단했습니다.
2. **On-Demand에서 Eager Loading으로 (Phase 2).** 요청 구간만 나눠 조회하던 방식(트리 하나에 최대 12회)을, 1년치를 한 번에 조회해 전체 트리를 만드는 방식으로 바꿨습니다. 초기 연산 비용이 커지지만 비동기라 사용자가 기다리지 않고, 이후 요청의 캐시 적중률이 올라갑니다.
   - 재빌드 범위가 요청 구간(최대 약 190일)에서 1년(365일)으로 넓어져, 부분 업데이트와 재빌드의 임계값을 다시 정했습니다([무효화와 정합성](#무효화와-정합성)).
3. **실행 시점 지연 (Phase 3).** 비동기로 바꿨는데도 Cold Start P95가 3.96s → 3.92s로 거의 그대로였습니다.
   - 모니터링에서 HikariCP Pending이 20까지 쌓이고 Acquire가 1s를 넘었습니다. 요청 스레드가 일하는 동안 비동기 빌드가 같은 커넥션 풀을 먼저 가져갔기 때문입니다.
   - 단순 비동기만 적용했을 때 Connection Usage는 최대 약 4s까지 올라갔습니다(캡처 없음). 재측정에서는 같은 구성(Phase 2)의 Usage 최대가 0.59~1.37s로, 4s는 재현되지 않았습니다.
   - 이벤트를 `@TransactionalEventListener(AFTER_COMMIT)`로 받아 요청 트랜잭션이 끝난 뒤 시작하고, `delayedExecutor`로 500ms 더 늦춰 두 작업의 실행 시간을 떼어 놓았습니다. Cold Start P95가 2.50s로 줄었습니다.
4. **격벽(Bulkhead)과 스레드 풀 분리 (Phase 4).** `CompletableFuture` 기본 풀(`ForkJoinPool.commonPool`)은 스레드가 적어, I/O가 길어지면 다른 작업까지 밀릴 수 있었습니다.
   - DB 조회는 `ioExecutor`(30, HikariCP pool 30에 맞춤), 트리 빌드는 `cpuExecutor`(코어 수 + 1)로 나눴습니다.
   - Cold Start P95가 1.96s, Acquire가 약 85ms가 됐습니다.

### 측정 기록

모두 [09-drilldown-lazycache](../../load-test/02-segment-tree-cache/scripts/09-drilldown-lazycache.js) 시나리오입니다. 50명이 동시에 6개월 전체를 처음 조회(Base, Cold Start)한 뒤 3분기 → 8월 → 6~7월로 좁혀 들어갑니다. 병목 1의 877ms(랜덤 6개월 범위, [03](../../load-test/02-segment-tree-cache/scripts/03-six-month-nocache.js))와는 시나리오가 다릅니다.

**2026-02-02: 스레드 분리 실험** (동기 빌드 기준)

| 구성 | Base P95 | 전체 P95 | 캡처 |
|---|---|---|---|
| 캐시 없음 | 2.97s | 2.79s | [k6](../../load-test/02-segment-tree-cache/results/drilldown/2026-02-02-nocache.jpg) |
| 캐시, 동기 빌드 | 3.34s | 2.68s | [k6](../../load-test/02-segment-tree-cache/results/drilldown/2026-02-02-lazycache.jpg) |
| + IO/CPU 스레드 분리 | 3.60s | 2.70s | [k6](../../load-test/02-segment-tree-cache/results/drilldown/2026-02-02-lazycache-split-thread.jpg) |
| + DB pool 조정 | 3.19s | 2.65s | [k6](../../load-test/02-segment-tree-cache/results/drilldown/2026-02-02-lazycache-split-thread-db-pool.jpg) |

**2026-02-07: Phase별 측정** (`1efc184`에 들어간 변경을 하나씩 켜며 측정)

| Phase | 변경 | HikariCP | Base P95 | 전체 P95 | Acquire (최고 → 안정) | Usage (최고 → 안정) | Pending 최대 | 캡처 |
|---|---|---|---|---|---|---|---|---|
| 0 | 기준(비동기 전) | 10 | 3.96s | 3.76s | ~960ms → ~830ms | ~450ms | 0 | [k6](../../load-test/02-segment-tree-cache/results/phase-0/k6.jpg), [pool](../../load-test/02-segment-tree-cache/results/phase-0/db-connection-panel.jpg) |
| 1 | 비동기 빌드 | 10 | — | — | — | 최대 약 4s | — | 캡처 없음 |
| 2 | + Eager Loading | 10 | 3.92s | 2.46s | ~1.1s → ~520ms | ~420ms → ~210ms | 20 | [k6](../../load-test/02-segment-tree-cache/results/phase-2/k6.jpg), [pool](../../load-test/02-segment-tree-cache/results/phase-2/db-pool-panel.jpg) |
| 3 | + AFTER_COMMIT·500ms 지연 | 10 | 2.50s | 2.06s | ~580ms → ~300ms | ~330ms → ~180ms | 0 | [k6](../../load-test/02-segment-tree-cache/results/phase-3/k6.jpg), [pool](../../load-test/02-segment-tree-cache/results/phase-3/db-pool-panel.jpg) |
| 4 | + IO/CPU 풀 분리, pool 30 | 30 | 1.96s | 1.90s | ~160ms → ~85ms | ~850ms → ~430ms | 0 | [k6](../../load-test/02-segment-tree-cache/results/phase-4/k6-pool-30.jpg), [pool](../../load-test/02-segment-tree-cache/results/phase-4/db-pool-panel-pool-30.jpg) |

Acquire·Usage는 Grafana 패널에서 읽은 근삿값입니다. 당시 대시보드(Grafana.com "Spring Boot 3.x Statistics", ID 19004)는 이 값을 `sum / count`, 즉 **서버 기동 후 누적 평균**으로 그립니다. 그래서 "최고 → 안정"은 구간별 값이 아니라 누적 평균이 줄어드는 모양입니다. Phase 4는 pool 20/40 결과도 있습니다([phase-4](../../load-test/02-segment-tree-cache/results/phase-4)).

**요약:** Phase 0 → 4에서 Cold Start P95는 3.96s → 1.96s(50.5% 감소, 이전 README의 "Cold Start 50.5% 단축"의 출처), Connection Acquire는 ~830ms → ~85ms로 줄었습니다. Phase 2에서 20까지 쌓였던 Pending은 Phase 3부터 0입니다. 다만 Phase 4는 HikariCP를 10 → 30으로 늘린 효과가 섞여 있습니다.

> **참고**
> - Phase 1은 캡처가 없습니다. 이전 판의 "Connection Usage 4s"는 이 단계에서 Tomcat 스레드와 트리 빌드 스레드가 커넥션을 두고 경쟁할 때 관측한 최대값입니다.
> - Phase 0을 "비동기 전"으로 본 것은 Phase 3 서술(비동기 전환 후에도 3.96s → 3.92s)에서 추정했습니다.

## 재측정 (2026-10-27)

정합성 결함(C1~C9)을 모두 고친 코드로 다시 쟀습니다. 환경은 [load-test/README.md](../../load-test/README.md#테스트-환경-2026-10-27-재측정-환경)에 있습니다: i3-1115G4 노트북 한 대에서 서버·k6·Docker(MariaDB 10.11, 300만 행)를 함께 돌렸습니다. 회차마다 서버를 새로 띄워 캐시와 지표를 비웠습니다([`rerun/run.sh`](../../load-test/02-segment-tree-cache/rerun/run.sh)). 원본 결과(k6 요약·로그, Prometheus 지표)는 [results/2026-10-rerun](../../load-test/02-segment-tree-cache/results/2026-10-rerun)에 있습니다.

> **편차가 큽니다.** 같은 구성을 3번 재면 첫 조회 P95가 최대 2배까지 차이 났습니다(예: Phase 0 1.70s~3.70s). 그래서 중앙값과 범위를 함께 적고, 범위가 겹치는 차이는 결론으로 쓰지 않습니다.

### 6개월 조회: 인덱스와 캐시 (03·05·06·07, 각 1회)

| 시나리오 | 구성 | P95 | 평균 |
|---|---|---|---|
| 랜덤 6개월 (03/05) | 캐시 없음, 인덱스 없음 | 463ms | 133ms |
| | 캐시 없음, 커버링 인덱스 | 273ms | 81ms |
| | 캐시, 커버링 인덱스 | 200ms | 55ms |
| 고정 범위 (06/07) | 캐시 없음, 커버링 인덱스 | 105ms | 53ms |
| | 캐시, 커버링 인덱스 | 133ms | 48ms |

- 커버링 인덱스는 이 환경에서도 효과가 분명합니다(463ms → 273ms).
- 캐시는 랜덤 범위에서 P95를 약간 낮췄지만, 모든 사용자가 같은 범위를 조회하는 고정 범위에서는 오히려 P95가 높았습니다. 고정 범위는 DB 버퍼 풀에 같은 페이지가 계속 남아 캐시 없이도 빠르고, 캐시 쪽은 첫 요청마다 1년치를 읽는 빌드 비용이 더해지기 때문입니다.

### Drill-down: 캐시 유무 (08 1회, 09 pool 30 4회)

| 단계 | 캐시 없음 (08) | 캐시 (09, 중앙값과 범위) |
|---|---|---|
| 1. 6개월 첫 조회 (Cold Start) | 1.45s | 1.44s (1.05s~1.56s) |
| 2. 3분기 | 761ms | 386ms (360ms~462ms) |
| 3. 8월 | 392ms | 333ms (243ms~449ms) |
| 4. 6~7월 | 342ms | 336ms (270ms~473ms) |

- 2~4단계는 캐시 미스가 0이었습니다(`cacheRate`).
- **첫 요청이 캐시 없을 때보다 느려지는 문제는 사라졌습니다.** 비동기 빌드 이후 Cold Start가 캐시 없음과 같은 수준입니다(예전 측정은 1.86배 느렸음).
- 캐시 효과가 3분기 조회(약 2배)에만 크게 보이는 이유: 이 API 응답은 **캠페인별 합계**와 **광고 유형별 합계** 두 부분인데, 세그먼트 트리는 광고 유형별 합계만 대신합니다. 캠페인별 합계는 매번 DB로 조회합니다(`CampaignTotalDataFacade`). 범위가 짧을수록 DB에 남는 쪽의 비용이 응답 시간을 좌우합니다.

### Phase별 재현 (09, 구성마다 3회)

현재 코드(Phase 4)에서 `CacheBuildEventListener`만 바꿔 Phase 0·2·3을 재현했습니다([`build-phase-jars.py`](../../load-test/02-segment-tree-cache/rerun/build-phase-jars.py), 바꾼 내용은 [patches](../../load-test/02-segment-tree-cache/rerun/patches)). Phase 1(Lazy Fetch 단계의 비동기)은 그 시점의 트리 구조가 필요해 재현하지 않았습니다.

| Phase | 구성 | HikariCP | 첫 조회 P95 | Acquire 최대 | Usage 최대 | Pending 최대 |
|---|---|---|---|---|---|---|
| 0 | 동기 빌드 | 10 | 2.24s (1.70s~3.70s) | 1.51s | 721ms | 21 (0~40) |
| 2 | 단순 비동기(commonPool, 즉시 실행) | 10 | 2.15s (1.94s~2.52s) | 1.58s | 726ms | 0 (0~13) |
| 3 | + AFTER_COMMIT·500ms 지연 | 10 | 1.71s (1.59s~1.73s) | 1.19s | 603ms | 0 (0~40) |
| 4 | + IO/CPU 풀 분리 | 10 | 2.37s (2.04s~2.48s) | 1.81s | 780ms | 7 (0~13) |
| 4 | + IO/CPU 풀 분리 | 30 | **1.48s** (1.05s~1.56s) | 874ms | 1.01s | 0 (0~20) |

Acquire·Usage·Pending은 Prometheus 5초 간격 수집값의 최대(3회 중앙값)라 순간 최대치를 놓칠 수 있습니다.

- **Phase 0 → 4(pool 30): 첫 조회 P95 2.24s → 1.48s, 약 34% 감소.** 예전 측정의 50.5%(3.96s → 1.96s)보다 작습니다.
- **500ms 지연(Phase 3)** 은 3회 모두 1.6~1.7s로 Phase 0·2보다 빠르고 편차도 작았습니다. 요청과 빌드의 실행 시간을 떼어 놓는 효과는 재현됐습니다.
- **스레드 풀 분리(Phase 4)는 커넥션 풀을 같이 늘려야 효과가 납니다.** pool 10에서는 IO 스레드 30개가 커넥션 10개를 두고 다퉈 Phase 3보다 느렸고 Pending이 쌓였습니다. pool 30에서야 가장 빨랐습니다. 즉 Phase 4의 개선은 풀 분리 자체보다 **ioExecutor 크기에 맞춘 커넥션 풀 확장**에서 나온 몫이 큽니다.
- pool 10/20/30/40을 1회씩 비교한 결과(첫 조회 1.32s/1.26s/1.41s/1.43s)는 편차 범위 안이라 최적 크기를 고르지 못했습니다.
- 이전 판의 "Connection Usage 4s"는 재현되지 않았습니다(Phase 2 Usage 최대 0.59~1.37s).

## 캐시 운영 정책

### 메모리 (2026-10-26 JOL 실측)

1년 트리 1개는 노드 729개(리프 365 + 내부 364)입니다([`SegmentTreeMemoryTest`](../../apps/api-server/src/test/java/growup/spring/springserver/global/cache/SegmentTreeMemoryTest.java)).

| 광고 유형 수 | 트리 1개 | 사용자 5,000명(1년치) |
|---|---|---|
| 1 | 207KB | 약 1.0GB |
| 3 (실제 유형 수) | 375KB | 약 1.8GB |

- 메모리 중 약 27%가 박싱된 `Double`입니다(`CampaignAnalysisDto`의 `Double` 필드). 원시 타입으로 바꾸면 줄일 수 있습니다.

### Eviction

- Caffeine `expireAfterAccess 1h`, `maximumSize 5000`입니다.
- 상한이 **사용자 수 기준**이고 트리 크기나 연도 수를 반영하지 않습니다. 위 실측대로라면 상한까지 차면 1년치만으로 약 1.8GB가 필요합니다. `maximumWeight`와 weigher(연도 수 × 트리 크기)로 바꾸는 것이 맞습니다.

### 무효화와 정합성

- 캠페인 삭제: 사용자 트리 전체를 버립니다(`removeAllTreeDataByEmail`).
- 기간 삭제: 삭제된 값을 트리에서 빼거나(부분 업데이트) 해당 연도를 다시 빌드합니다. 같은 연도이면서 60일 이하일 때만 부분 업데이트합니다.
  - **설계 의도 (Phase 2):** Eager Loading으로 재빌드 범위가 1년(365일)이 되면서 계산상 교차점은 약 42일이었습니다. 하지만 재빌드는 바뀌지 않은 나머지 날짜까지 DB에서 다시 읽고, 백그라운드에서 커넥션과 CPU를 잡아 다른 API와 경합합니다. 그래서 교차점보다 보수적으로 60일 초과일 때만 재빌드하도록 정했습니다.
  - **실측 (2026-10-26):** 트리 CPU 비용만 보면 60일과 90일 사이에서 역전됩니다([`TreeCpuBenchmarkTest`](../../apps/api-server/src/test/java/growup/spring/springserver/global/cache/TreeCpuBenchmarkTest.java)). DB I/O를 빼고도 60일 기준이 맞습니다.

  | 삭제 일수 | 10 | 30 | 60 | 90 | 180 | 300 |
  |---|---|---|---|---|---|---|
  | 부분 업데이트 | 12µs | 39µs | 73µs | 123µs | 270µs | 385µs |
  | 전체 재빌드 | 81µs | 95µs | 84µs | 86µs | 83µs | 83µs |

  원래 코드는 이 의도와 달리 300일 이상일 때 부분 업데이트를 골라 조건이 반대였습니다([C8](../known-issues.md)). 다만 두 방식 모두 1ms 미만이고, 실제 비용은 DB 조회(부분 업데이트는 삭제 기간, 재빌드는 1년치)가 좌우합니다. DB를 포함한 비교는 재측정 때 확인합니다.
- **새 데이터 업로드:** 광고 데이터는 Django excel-parser가 DB에 직접 넣기 때문에 Spring 캐시는 이를 알 수 없었고, `expireAfterAccess`라서 계속 조회하는 사용자의 트리는 만료되지도 않았습니다([C9](../known-issues.md)). 지금은 Django가 업로드를 마친 뒤 요청자 JWT로 `POST /api/campaign/cache/invalidate`를 호출해 해당 사용자의 트리를 지웁니다. 업로드 직전에 시작된 비동기 빌드가 무효화 뒤에 등록되는 경우는 아직 막지 못합니다.

### 캐시 스탬피드

- 같은 사용자·연도의 빌드는 `buildingInProgress` Set으로 한 번만 실행합니다.
- 빌드는 별도 객체에서 끝까지 만든 뒤 등록합니다. 빌드 중에 들어온 요청은 캐시를 쓰지 않고 DB로 조회합니다([C7](../known-issues.md) 수정).

## 회고

- 인덱스는 빠른 해결책이지 영구적인 해결책이 아니었습니다.
- 무거운 캐시는 생성 비용과 정합성 관리 부담을 함께 가져왔습니다.
- 작업을 뒤로 미루는 것과 자원을 덜 쓰는 것은 다른 문제였습니다.
- 기술보다 먼저 "이 서비스에 필요한가"를 따져야 했습니다. 이후 분산 환경으로 확장한 결과는 [④ MSA 글로벌 캐시](04-global-cache-msa.md)에 정리했습니다.
