# ② 캐시를 적용했는데 왜 첫 요청은 더 느려졌을까

> 대시보드 조회 기간을 6개월로 늘린 뒤의 DB 병목을 QueryDSL과 커버링 인덱스로 줄였습니다. 이어서 반복되는 구간 조회를 세그먼트 트리 캐시로 처리했고, 그 과정에서 생긴 Cold Start와 DB 커넥션 경합을 비동기 빌드와 스레드 풀 분리로 해결했습니다.
>
> 정합성 결함(C1~C9)을 고친 뒤 로컬에서 다시 잰 결과(2026-10-27~28): 커버링 인덱스로 P95 **463ms → 273ms**, 캐시의 첫 요청이 캐시 없음과 같은 수준(**1.44s vs 1.45s**), Phase 0 → 4로 첫 조회 P95 **2.24s → 1.48s(약 34%)**. 커넥션 풀과 IO 스레드 풀을 같은 크기로 10~50까지 올려 본 결과, 이 PC에서는 **30**이 첫 조회 P95(1.29s)와 Acquire+Usage 합이 가장 낮은 구간에 있었습니다.

## 측정 환경과 원칙

| 항목 | 내용 |
|---|---|
| 환경 | i3-1115G4 노트북(4코어) 한 대에서 서버·k6·Docker(MariaDB 10.11)를 함께 실행. 자세한 사양은 [load-test/README.md](../../load-test/README.md#테스트-환경-2026-10-27-재측정-환경) |
| 데이터 | 6개월 더미 데이터 약 300만 행 |
| 엔드포인트 | `GET /api/campaign/totalAnalysisData3`(캐시 없음), `totalAnalysisData4`(Lazy 세그먼트 트리) |
| 실행 | [`rerun/run.sh`](../../load-test/02-segment-tree-cache/rerun/run.sh): 회차마다 서버를 새로 띄워 캐시와 HikariCP 누적 지표를 비운 뒤 k6를 돌리고 Prometheus 지표를 저장 |
| 결과 | [results/2026-10-rerun](../../load-test/02-segment-tree-cache/results/2026-10-rerun) (회차별 k6 요약·로그, Prometheus 지표, keyword 인덱스 상태) |

- **편차가 큽니다.** 같은 구성을 3번 재면 첫 조회 P95가 최대 2배까지 차이 났습니다(예: Phase 0 1.70s~3.70s). 반복한 구성은 중앙값과 범위를 함께 적고, **범위가 겹치는 차이는 결론으로 쓰지 않습니다.**
- Acquire·Usage·Pending의 "최대"는 Prometheus 5초 간격 수집값의 최대라 순간 최대치를 놓칠 수 있습니다.
- 2026-01~02의 예전 측정은 수정 전 코드와 사양 기록이 없는 서버에서 잰 값이라 직접 비교하지 않고 [부록](#부록-예전-측정-기록)에 남겼습니다.

## 근거 맵

| 주장 | 코드 | k6 스크립트 | 결과 |
|---|---|---|---|
| QueryDSL로 DB I/O 3회 → 1회 | [`KeywordRepositoryImpl`](../../apps/api-server/src/main/java/growup/spring/springserver/keyword/repository/KeywordRepositoryImpl.java) | [01-baseline](../../load-test/02-segment-tree-cache/scripts/01-baseline.js), [02-querydsl-refactor](../../load-test/02-segment-tree-cache/scripts/02-querydsl-refactor.js) | [query-count](../../load-test/02-segment-tree-cache/results/query-refactor/query-count.jpg) (재측정 안 함) |
| 커버링 인덱스 463ms → 273ms | [`Keyword`](../../apps/api-server/src/main/java/growup/spring/springserver/keyword/domain/Keyword.java) `@Index`, [전환 SQL](../../load-test/02-segment-tree-cache/sql/covering-index.sql) ([O4](../known-issues.md)) | [03-six-month-nocache](../../load-test/02-segment-tree-cache/scripts/03-six-month-nocache.js) | [03-nocache-noindex](../../load-test/02-segment-tree-cache/results/2026-10-rerun/03-nocache-noindex), [03-nocache-index](../../load-test/02-segment-tree-cache/results/2026-10-rerun/03-nocache-index) |
| 세그먼트 트리 캐시 | [`LazySegmentTreeService`](../../apps/api-server/src/main/java/growup/spring/springserver/global/cache/LazySegmentTreeService.java) | [05](../../load-test/02-segment-tree-cache/scripts/05-six-month-lazycache.js), [06](../../load-test/02-segment-tree-cache/scripts/06-fixed-range-nocache.js), [07](../../load-test/02-segment-tree-cache/scripts/07-fixed-range-lazycache.js) | [05](../../load-test/02-segment-tree-cache/results/2026-10-rerun/05-lazycache-index), [06](../../load-test/02-segment-tree-cache/results/2026-10-rerun/06-fixed-nocache), [07](../../load-test/02-segment-tree-cache/results/2026-10-rerun/07-fixed-lazycache) |
| Drill-down 캐시 적중, 첫 요청이 캐시 없음과 같은 수준 | 위와 같음 | [08](../../load-test/02-segment-tree-cache/scripts/08-drilldown-nocache.js), [09](../../load-test/02-segment-tree-cache/scripts/09-drilldown-lazycache.js) | [08](../../load-test/02-segment-tree-cache/results/2026-10-rerun/08-drilldown-nocache), [09 pool 30](../../load-test/02-segment-tree-cache/results/2026-10-rerun/09-drilldown-lazycache-pool30) |
| 비동기 빌드 (Spring Event + CompletableFuture, 500ms 지연) | [`CacheBuildEventListener`](../../apps/api-server/src/main/java/growup/spring/springserver/global/listener/CacheBuildEventListener.java), [`CampaignTotalDataFacade`](../../apps/api-server/src/main/java/growup/spring/springserver/campaign/facade/CampaignTotalDataFacade.java) | 09 | `phase0~3-pool10-r1~3`, 재현 빌드 [`build-phase-jars.py`](../../load-test/02-segment-tree-cache/rerun/build-phase-jars.py) |
| IO/CPU 스레드 풀 분리와 풀 크기 | [`ThreadPoolConfig`](../../apps/api-server/src/main/java/growup/spring/springserver/global/config/ThreadPoolConfig.java), [`application-bottleNeckTest.yml`](../../apps/api-server/src/main/resources/application-bottleNeckTest.yml) | 09 | `phase4-pool10/30-r1~3`, `phase4-cp<N>-io<N>-r1~3` ([`pool-sweep.sh`](../../load-test/02-segment-tree-cache/rerun/pool-sweep.sh)) |
| 부분 업데이트와 전체 재빌드 임계값 | [`CampaignDeleteDto`](../../apps/api-server/src/main/java/growup/spring/springserver/campaign/dto/CampaignDeleteDto.java), [`TreeCpuBenchmarkTest`](../../apps/api-server/src/test/java/growup/spring/springserver/global/cache/TreeCpuBenchmarkTest.java) | — | [아래 표](#무효화와-정합성) |
| 단순 대안(선형 합산) 비교, 트리 메모리 | [`RangeQueryBenchmarkTest`](../../apps/api-server/src/test/java/growup/spring/springserver/global/cache/RangeQueryBenchmarkTest.java), [`SegmentTreeMemoryTest`](../../apps/api-server/src/test/java/growup/spring/springserver/global/cache/SegmentTreeMemoryTest.java) | — | [아래 표](#단순-대안과-비교-2026-10-26-jmh) |

결과 폴더 이름은 모두 [results/2026-10-rerun](../../load-test/02-segment-tree-cache/results/2026-10-rerun) 아래에 있습니다.

## 병목 1: 6개월 조회에서 DB 조회 자체가 문제

- **원인:**
  - 멤버 → 캠페인 → 일별 데이터 순서로 나눠 조회한 뒤 애플리케이션에서 조합하고 있었습니다. 같은 통계를 위해 불필요한 I/O가 생겼습니다.
  - 실행 계획을 보면 Full Scan은 없었지만, 집계 컬럼(`adCost`, `adSales`)을 읽기 위한 Key Lookup이 발생했습니다.
- **조치:**
  1. QueryDSL로 조회를 하나로 합쳐 DB I/O를 3회에서 1회로 줄였습니다([query-count](../../load-test/02-segment-tree-cache/results/query-refactor/query-count.jpg)). 이 단계는 재측정하지 않았습니다.
  2. 집계 컬럼까지 담은 커버링 인덱스를 만들었습니다. 원래 DDL이 저장소에 없어 다시 설계했습니다([전환 SQL](../../load-test/02-segment-tree-cache/sql/covering-index.sql), [O4](../known-issues.md)).

**재측정 (03, 랜덤 6개월 범위, 캐시 없음, 1회씩)**

| 구성 | P95 | 평균 |
|---|---|---|
| 인덱스 없음(FK 인덱스만) | 463ms | 133ms |
| 커버링 인덱스 | **273ms** | **81ms** |

- P95가 41% 줄었습니다. `EXPLAIN`에서 `keyword` 접근이 `Using index`로 바뀌는 것을 확인했습니다.

## 병목 2: 반복되는 세부 구간 조회

6개월 전체를 본 뒤 특정 월이나 분기로 범위를 좁히는 Drill-down 패턴을 예상했습니다.

| 구조 | 구간 합 | MAX/MIN | 수정 | 메모리 |
|---|---|---|---|---|
| Raw 데이터 + 선형 합산 | O(N) | O(N) | O(1) | N |
| 누적합 | O(1) | 불가 | O(N) | N |
| **세그먼트 트리** | O(log N) | O(log N) | O(log N) | ~2N(반복형) / 4N(재귀형) |

- **Lazy Loading의 한계:** 상위 구간을 조회한 뒤 하위 구간을 조회하면 리프 노드가 비어 있어 DB를 다시 조회했습니다. 그래서 1년치를 한 번에 채우는 **Range-based Eager Loading**으로 바꿨습니다.
- **트리가 대신하는 범위:** 이 API 응답은 **캠페인별 합계**와 **광고 유형별 합계** 두 부분입니다. 세그먼트 트리는 광고 유형별 합계만 대신하고, 캠페인별 합계는 매번 DB로 조회합니다([`CampaignTotalDataFacade`](../../apps/api-server/src/main/java/growup/spring/springserver/campaign/facade/CampaignTotalDataFacade.java)).

### 6개월 조회: 캐시 유무 (03·05·06·07, 커버링 인덱스, 1회씩)

| 시나리오 | 캐시 없음 | 캐시 |
|---|---|---|
| 랜덤 6개월 범위 (03/05) | 273ms | 200ms |
| 고정 범위, 모든 사용자 같은 구간 (06/07) | 105ms | 133ms |

- 랜덤 범위에서는 캐시가 P95를 조금 낮췄습니다.
- 고정 범위에서는 오히려 캐시 쪽이 느렸습니다. 캐시 없이도 DB 버퍼 풀에 같은 페이지가 계속 남아 빠르고, 캐시 쪽은 사용자마다 첫 요청에 1년치를 읽는 빌드 비용이 더해지기 때문입니다.

### Drill-down: 캐시 유무 (08 1회, 09 pool 30 4회)

[09-drilldown-lazycache](../../load-test/02-segment-tree-cache/scripts/09-drilldown-lazycache.js)는 50명이 동시에 6개월 전체를 처음 조회(Cold Start)한 뒤 3분기 → 8월 → 6~7월로 좁혀 들어갑니다.

| 단계 | 캐시 없음 (08) | 캐시 (09, 중앙값과 범위) |
|---|---|---|
| 1. 6개월 첫 조회 (Cold Start) | 1.45s | 1.44s (1.05s~1.56s) |
| 2. 3분기 | 761ms | 386ms (360ms~462ms) |
| 3. 8월 | 392ms | 333ms (243ms~449ms) |
| 4. 6~7월 | 342ms | 336ms (270ms~473ms) |

- 2~4단계는 캐시 미스가 0이었습니다(`cacheRate`).
- **첫 요청이 캐시 없을 때와 같은 수준입니다.** 동기로 트리를 만들던 시절에는 첫 요청이 캐시 없음보다 1.86배 느렸습니다([부록](#부록-예전-측정-기록)). 이 문제를 아래 병목 3에서 풀었습니다.
- 캐시 효과는 3분기 조회(약 2배)에서만 크게 보입니다. 범위가 짧을수록 트리가 대신하지 않는 캠페인별 합계(DB 조회)가 응답 시간을 좌우합니다.

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

1. **빌드 비동기화 (Phase 1).** 트리 생성이 끝날 때까지 Tomcat 스레드가 기다려 응답이 늦었습니다. 흐름을 DB 조회 → 트리 빌드 → 응답에서 DB 조회 → 응답 + 비동기 빌드로 바꿨습니다. `CompletableFuture`로 트리 생성을 백그라운드에서 실행합니다. Kafka/MQ는 단일 서버에서 과하다고 판단했습니다.
2. **On-Demand에서 Eager Loading으로 (Phase 2).** 요청 구간만 나눠 조회하던 방식(트리 하나에 최대 12회)을 1년치를 한 번에 조회해 전체 트리를 만드는 방식으로 바꿨습니다. 초기 비용은 커지지만 비동기라 사용자가 기다리지 않고, 이후 요청의 캐시 적중률이 올라갑니다. 재빌드 범위가 1년으로 넓어져 부분 업데이트 임계값도 다시 정했습니다([무효화와 정합성](#무효화와-정합성)).
3. **실행 시점 지연 (Phase 3).** 비동기로 바꿔도 첫 조회가 빨라지지 않았습니다. 요청 스레드가 일하는 동안 비동기 빌드가 같은 커넥션 풀을 먼저 가져가 HikariCP Pending이 쌓였기 때문입니다. 이벤트를 `@TransactionalEventListener(AFTER_COMMIT)`로 받아 요청 트랜잭션이 끝난 뒤 시작하고, `delayedExecutor`로 500ms 더 늦춰 두 작업의 실행 시간을 떼어 놓았습니다.
4. **격벽(Bulkhead)과 스레드 풀 분리 (Phase 4).** `CompletableFuture` 기본 풀(`ForkJoinPool.commonPool`)은 스레드가 적어 I/O가 길어지면 다른 작업까지 밀릴 수 있었습니다. DB 조회는 `ioExecutor`, 트리 빌드는 `cpuExecutor`(코어 수 + 1)로 나누고, `ioExecutor` 크기에 맞춰 HikariCP pool을 10 → 30으로 늘렸습니다.

### Phase별 재현 (09, 구성마다 3회)

현재 코드(Phase 4)에서 `CacheBuildEventListener`만 바꿔 Phase 0·2·3을 재현했습니다([`build-phase-jars.py`](../../load-test/02-segment-tree-cache/rerun/build-phase-jars.py), 바꾼 내용은 [patches](../../load-test/02-segment-tree-cache/rerun/patches)). Phase 1(Lazy Fetch 단계의 비동기)은 그 시점의 트리 구조가 필요해 재현하지 않았습니다.

| Phase | 구성 | HikariCP | 첫 조회 P95 | Acquire 최대 | Usage 최대 | Pending 최대 |
|---|---|---|---|---|---|---|
| 0 | 동기 빌드 | 10 | 2.24s (1.70s~3.70s) | 1.51s | 721ms | 21 (0~40) |
| 2 | 단순 비동기(commonPool, 즉시 실행) | 10 | 2.15s (1.94s~2.52s) | 1.58s | 726ms | 0 (0~13) |
| 3 | + AFTER_COMMIT·500ms 지연 | 10 | 1.71s (1.59s~1.73s) | 1.19s | 603ms | 0 (0~40) |
| 4 | + IO/CPU 풀 분리 | 10 | 2.37s (2.04s~2.48s) | 1.81s | 780ms | 7 (0~13) |
| 4 | + IO/CPU 풀 분리 | 30 | **1.48s** (1.05s~1.56s) | 874ms | 1.01s | 0 (0~20) |

Acquire·Usage·Pending은 3회의 중앙값입니다.

- **Phase 0 → 4(pool 30): 첫 조회 P95 2.24s → 1.48s, 약 34% 감소.**
- **500ms 지연(Phase 3)** 은 3회 모두 1.6~1.7s로 Phase 0·2보다 빠르고 편차도 작았습니다. 요청과 빌드의 실행 시간을 떼어 놓는 효과가 재현됐습니다.
- **스레드 풀 분리는 커넥션 풀을 같이 늘려야 효과가 납니다.** pool 10에서는 IO 스레드 30개가 커넥션 10개를 두고 다퉈 Phase 3보다 느렸고 Pending이 쌓였습니다. 즉 Phase 4의 개선은 풀 분리 자체보다 **커넥션 풀 확장**에서 나온 몫이 큽니다.
- 예전 기록의 "단순 비동기에서 Connection Usage 최대 약 4s"는 재현되지 않았습니다(Phase 2 Usage 최대 0.59~1.37s).

### Phase 4 풀 크기 튜닝: 커넥션 풀 = IO 스레드 풀 (09, 크기마다 3회)

위 표의 Phase 4 pool 10은 IO 스레드를 30으로 둔 채 커넥션만 10이었습니다. 이번에는 HikariCP `maximum-pool-size`와 `ioExecutor` 크기를 **같은 값 N**으로 함께 올리며 이 PC에서 가장 나은 N을 찾았습니다([`pool-sweep.sh`](../../load-test/02-segment-tree-cache/rerun/pool-sweep.sh), 결과 `phase4-cp<N>-io<N>-r1~3`). 회차 순서는 N을 돌아가며 섞어 시간대에 따른 편차가 한 구성에 몰리지 않게 했습니다.

판단 기준은 **Connection Acquire(커넥션을 얻기까지 기다린 시간)** 와 **Usage(커넥션을 빌려 쓴 시간, 대부분 쿼리 실행)** 입니다. 풀을 키우면 Acquire는 줄지만, 같은 4코어 DB에 동시 쿼리가 늘어 Usage는 늘어날 수 있습니다. 두 값의 합이 가장 작은 지점이 이 자원에서의 적정 크기라고 보았습니다. Acquire·Usage P95는 HikariCP 타이머의 히스토그램으로 구했습니다(15초 구간 P95의 최대값).

| N (커넥션 = IO 스레드) | 첫 조회 P95 | Acquire P95 | Usage P95 | Acquire + Usage | Pending 최대 |
|---|---|---|---|---|---|
| 10 | 1.95s (1.45s~2.01s) | 1.44s | 881ms | 2.32s | 0 (0~26) |
| 20 | 1.36s (1.27s~1.64s) | 976ms | 616ms | 1.59s | 0 (0~59) |
| **30** | **1.29s** (1.05s~1.44s) | 547ms | 770ms | **1.32s** | 0 (0~0) |
| 40 | 1.77s (1.62s~3.43s) | 1.21s | 1.60s | 2.81s | 0 (0~0) |
| 50 | 1.37s (1.17s~1.48s) | **128ms** | 1.05s | **1.18s** | 0 (0~0) |

값은 3회 중앙값입니다(Acquire + Usage는 두 중앙값의 합).

- **10은 부족합니다.** 범위(1.45s~2.01s)가 30(1.05s~1.44s)과 겹치지 않습니다. Acquire가 1.44s로, 커넥션을 기다리는 시간이 쿼리 시간보다 길었습니다.
- **20·30·50은 범위가 겹쳐 우열을 가릴 수 없습니다.** 그중 30이 첫 조회 P95 중앙값이 가장 낮고, Pending이 3회 모두 0이었습니다. 20은 1회에서 Pending 59, 3·4단계 P95 3.6s의 튀는 값이 있었습니다.
- **50에서는 대기가 DB 쪽으로 옮겨 갔습니다.** 동시 사용자 50명이 모두 커넥션을 바로 얻어 Acquire가 128ms까지 줄었지만, Usage가 1.05s로 늘어 첫 조회 P95는 30과 같은 수준이었습니다. 4코어에서 DB가 동시에 처리할 수 있는 쿼리 수가 정해져 있어, 풀을 키워도 기다리는 곳이 HikariCP에서 DB로 바뀔 뿐입니다.
- **40은 3회 모두 나빴습니다**(Usage P95 1.37s~1.87s, 30과 범위 겹치지 않음). 크기에 따라 단조롭게 변하지 않는 이유는 찾지 못했습니다.
- **IO 스레드를 커넥션 수에 맞추는 것이 낫습니다.** 커넥션 10일 때 IO 스레드 30(앞 표, 2.37s, 2.04s~2.48s)보다 IO 스레드 10(1.95s, 1.45s~2.01s)이 빨랐습니다. 커넥션보다 많은 IO 스레드는 커넥션 대기만 늘립니다.
- **결론:** 현재 설정(커넥션 30, IO 스레드 30)을 유지합니다. 이 PC에서 첫 조회와 Acquire+Usage가 가장 낮은 구간에 있고 편차도 작았습니다. 50은 합이 비슷하지만 DB 커넥션을 더 쓰면서 응답은 나아지지 않았습니다.

> **한계:** 서버·DB·k6가 노트북 한 대에서 CPU를 나눠 썼으므로, 이 결과는 "이 PC에서의 적정값"입니다. DB가 다른 서버에 있으면 적정 크기는 달라집니다. IO 스레드의 활성 수·큐 길이도 수집했지만, 빌드 작업이 5초 수집 간격 사이에 끝나 거의 잡히지 않았습니다.

## 캐시 운영 정책

### 메모리 (2026-10-26 JOL 실측)

1년 트리 1개는 노드 729개(리프 365 + 내부 364)입니다([`SegmentTreeMemoryTest`](../../apps/api-server/src/test/java/growup/spring/springserver/global/cache/SegmentTreeMemoryTest.java)).

| 광고 유형 수 | 트리 1개 | 사용자 5,000명(1년치) |
|---|---|---|
| 1 | 207KB | 약 1.0GB |
| 3 (실제 유형 수) | 375KB | 약 1.8GB |

- 메모리 중 약 27%가 박싱된 `Double`입니다(`CampaignAnalysisDto`의 `Double` 필드). 원시 타입으로 바꾸면 줄일 수 있습니다.

### 만료와 크기 제한

세그먼트 트리 캐시는 `LazySegmentTreeService`의 Caffeine 캐시입니다.

```java
Caffeine.newBuilder()
        .expireAfterAccess(1, TimeUnit.HOURS)
        .maximumSize(5000)
```

- **만료:** 마지막 **조회** 후 1시간입니다. 만든 시점 기준이 아니라서 계속 조회하는 사용자의 트리는 만료되지 않습니다. 그래서 새 데이터가 올라오면 명시적으로 지워야 합니다([무효화와 정합성](#무효화와-정합성)).
- **크기 제한:** 키가 email이라 5000은 **사용자 수(엔트리 수)** 입니다. 한 엔트리에 사용자의 연도별 트리가 모두 들어갑니다. 상한을 넘으면 Caffeine이 W-TinyLFU 정책으로 덜 쓰는 엔트리를 내보냅니다.
- **한계:** 상한이 트리 크기나 연도 수를 반영하지 않습니다. 위 실측대로라면 상한까지 차면 1년치만으로 약 1.8GB가 필요합니다. `maximumWeight`와 weigher(연도 수 × 트리 크기)로 바꾸는 것이 맞습니다.
- 캠페인 조회(`campaigns`)·순매출 통계(`salesStats`)에 쓰는 Spring 캐시([`CacheConfig`](../../apps/api-server/src/main/java/growup/spring/springserver/global/config/CacheConfig.java), 저장 후 15분, 최대 1,000개)는 이 트리 캐시와 별개입니다.

### 무효화와 정합성

- 캠페인 삭제: 사용자 트리 전체를 버립니다(`removeAllTreeDataByEmail`).
- 기간 삭제: 삭제된 값을 트리에서 빼거나(부분 업데이트) 해당 연도를 다시 빌드합니다. 같은 연도이면서 60일 이하일 때만 부분 업데이트합니다.
  - **설계 의도 (Phase 2):** Eager Loading으로 재빌드 범위가 1년(365일)이 되면서 계산상 교차점은 약 42일이었습니다. 하지만 재빌드는 바뀌지 않은 나머지 날짜까지 DB에서 다시 읽고, 백그라운드에서 커넥션과 CPU를 잡아 다른 API와 경합합니다. 그래서 교차점보다 보수적으로 60일 초과일 때만 재빌드하도록 정했습니다.
  - **실측 (2026-10-26):** 트리 CPU 비용만 보면 60일과 90일 사이에서 역전됩니다([`TreeCpuBenchmarkTest`](../../apps/api-server/src/test/java/growup/spring/springserver/global/cache/TreeCpuBenchmarkTest.java)). DB I/O를 빼고도 60일 기준이 맞습니다.

  | 삭제 일수 | 10 | 30 | 60 | 90 | 180 | 300 |
  |---|---|---|---|---|---|---|
  | 부분 업데이트 | 12µs | 39µs | 73µs | 123µs | 270µs | 385µs |
  | 전체 재빌드 | 81µs | 95µs | 84µs | 86µs | 83µs | 83µs |

  원래 코드는 이 의도와 달리 300일 이상일 때 부분 업데이트를 골라 조건이 반대였습니다([C8](../known-issues.md)). 다만 두 방식 모두 1ms 미만이고, 실제 비용은 DB 조회(부분 업데이트는 삭제 기간, 재빌드는 1년치)가 좌우합니다.
- **새 데이터 업로드:** 광고 데이터는 Django excel-parser가 DB에 직접 넣기 때문에 Spring 캐시는 이를 알 수 없었고, `expireAfterAccess`라서 계속 조회하는 사용자의 트리는 만료되지도 않았습니다([C9](../known-issues.md)). 지금은 Django가 업로드를 마친 뒤 요청자 JWT로 `POST /api/campaign/cache/invalidate`를 호출해 해당 사용자의 트리를 지웁니다. 업로드 직전에 시작된 비동기 빌드가 무효화 뒤에 등록되는 경우는 아직 막지 못합니다.

### 캐시 스탬피드

- 같은 사용자·연도의 빌드는 `buildingInProgress` Set으로 한 번만 실행합니다.
- 빌드는 별도 객체에서 끝까지 만든 뒤 등록합니다. 빌드 중에 들어온 요청은 캐시를 쓰지 않고 DB로 조회합니다([C7](../known-issues.md) 수정).

## 회고

- 인덱스는 빠른 해결책이지 영구적인 해결책이 아니었습니다.
- 무거운 캐시는 생성 비용과 정합성 관리 부담을 함께 가져왔습니다.
- 작업을 뒤로 미루는 것과 자원을 덜 쓰는 것은 다른 문제였습니다.
- 다시 재 보니 개선 폭은 예전 기록(50.5%)보다 작은 약 34%였고, Phase 4의 개선 상당 부분은 스레드 풀 분리가 아니라 커넥션 풀 확장에서 나왔습니다. 변경을 하나씩 켜고 반복 측정하지 않으면 원인을 잘못 짚을 수 있었습니다.
- 기술보다 먼저 "이 서비스에 필요한가"를 따져야 했습니다. 이후 분산 환경으로 확장한 결과는 [④ MSA 글로벌 캐시](04-global-cache-msa.md)에 정리했습니다.

## 부록: 예전 측정 기록

정합성 결함(C1~C9)을 고치기 전 코드로, 사양 기록이 없는 서버에서 잰 값입니다. 위 재측정과 환경이 달라 직접 비교하지 않습니다. 캡처는 [results](../../load-test/02-segment-tree-cache/results)에 있습니다.

**병목 1 (03 랜덤 6개월)**

| 조치 | P95 | TPS | Connection Acquire max |
|---|---|---|---|
| 개선 전 | — | 3.87/s | 6s |
| QueryDSL로 조회 통합 | 2.97s | 11.06/s | 2s |
| 커버링 인덱스 | 877ms | 23.45/s | 500ms |

**2026-01-28 Drill-down (09, 동기 빌드):** 6개월 첫 조회 P95가 캐시 없음 2.76s, 캐시 5.12s로 1.86배였습니다([nocache](../../load-test/02-segment-tree-cache/results/drilldown/nocache-thresholds.jpg), [lazycache](../../load-test/02-segment-tree-cache/results/drilldown/lazycache-refactor.jpg)).

**2026-02-02 스레드 분리 실험 (09, 동기 빌드)**

| 구성 | 첫 조회 P95 | 전체 P95 | 캡처 |
|---|---|---|---|
| 캐시 없음 | 2.97s | 2.79s | [k6](../../load-test/02-segment-tree-cache/results/drilldown/2026-02-02-nocache.jpg) |
| 캐시, 동기 빌드 | 3.34s | 2.68s | [k6](../../load-test/02-segment-tree-cache/results/drilldown/2026-02-02-lazycache.jpg) |
| + IO/CPU 스레드 분리 | 3.60s | 2.70s | [k6](../../load-test/02-segment-tree-cache/results/drilldown/2026-02-02-lazycache-split-thread.jpg) |
| + DB pool 조정 | 3.19s | 2.65s | [k6](../../load-test/02-segment-tree-cache/results/drilldown/2026-02-02-lazycache-split-thread-db-pool.jpg) |

**2026-02-07 Phase별 측정 (09)**

| Phase | 변경 | HikariCP | 첫 조회 P95 | 전체 P95 | Acquire (최고 → 안정) | Usage (최고 → 안정) | Pending 최대 | 캡처 |
|---|---|---|---|---|---|---|---|---|
| 0 | 기준(비동기 전) | 10 | 3.96s | 3.76s | ~960ms → ~830ms | ~450ms | 0 | [k6](../../load-test/02-segment-tree-cache/results/phase-0/k6.jpg), [pool](../../load-test/02-segment-tree-cache/results/phase-0/db-connection-panel.jpg) |
| 1 | 비동기 빌드 | 10 | — | — | — | 최대 약 4s | — | 캡처 없음 |
| 2 | + Eager Loading | 10 | 3.92s | 2.46s | ~1.1s → ~520ms | ~420ms → ~210ms | 20 | [k6](../../load-test/02-segment-tree-cache/results/phase-2/k6.jpg), [pool](../../load-test/02-segment-tree-cache/results/phase-2/db-pool-panel.jpg) |
| 3 | + AFTER_COMMIT·500ms 지연 | 10 | 2.50s | 2.06s | ~580ms → ~300ms | ~330ms → ~180ms | 0 | [k6](../../load-test/02-segment-tree-cache/results/phase-3/k6.jpg), [pool](../../load-test/02-segment-tree-cache/results/phase-3/db-pool-panel.jpg) |
| 4 | + IO/CPU 풀 분리, pool 30 | 30 | 1.96s | 1.90s | ~160ms → ~85ms | ~850ms → ~430ms | 0 | [k6](../../load-test/02-segment-tree-cache/results/phase-4/k6-pool-30.jpg), [pool](../../load-test/02-segment-tree-cache/results/phase-4/db-pool-panel-pool-30.jpg) |

- 이 표가 이전 README의 "Cold Start 50.5% 단축"(3.96s → 1.96s)의 출처입니다. Phase 4는 HikariCP 10 → 30 확장 효과가 섞여 있고, pool 20/40 결과도 있습니다([phase-4](../../load-test/02-segment-tree-cache/results/phase-4)).
- Acquire·Usage는 Grafana 패널에서 읽은 근삿값입니다. 당시 대시보드(Grafana.com "Spring Boot 3.x Statistics", ID 19004)는 이 값을 `sum / count`, 즉 **서버 기동 후 누적 평균**으로 그립니다. 그래서 "최고 → 안정"은 구간별 값이 아니라 누적 평균이 줄어드는 모양입니다.
- Phase 1은 캡처가 없습니다. "Connection Usage 4s"는 이 단계에서 Tomcat 스레드와 트리 빌드 스레드가 커넥션을 두고 경쟁할 때 관측한 최대값입니다.
- Phase 0을 "비동기 전"으로 본 것은 Phase 3 서술(비동기 전환 후에도 3.96s → 3.92s)에서 추정했습니다.
