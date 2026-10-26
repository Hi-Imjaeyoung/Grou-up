# ② 캐시를 적용했는데 왜 첫 요청은 더 느려졌을까

> 대시보드 조회 기간을 6개월로 늘린 뒤의 DB 병목을 QueryDSL과 커버링 인덱스로 줄였습니다. 이어서 반복되는 구간 조회를 세그먼트 트리 캐시로 처리했고, 그 과정에서 생긴 Cold Start와 DB 커넥션 경합을 비동기 빌드와 스레드 풀 분리로 해결했습니다.

> **주의:** 2026-10-26 분석에서 세그먼트 트리 빌드 시 객체 공유 버그([known-issues C1](../known-issues.md))를 발견해 수정했습니다(`ec6fc8b`). 같은 캐시 경로의 나머지 정합성 결함(C2~C8)이 남아 있어, 이 문서의 캐시 관련 수치는 이를 고친 뒤 **재측정 예정**입니다.

| 항목 | 내용 |
|---|---|
| 데이터 | 6개월 더미 데이터 약 300만 행 (MariaDB) |
| 엔드포인트 | `GET /api/campaign/totalAnalysisData3`(캐시 없음), `totalAnalysisData4`(Lazy 세그먼트 트리) |
| 테스트 환경 | [load-test/README.md](../../load-test/README.md#실행) (서버 사양 기입 필요) |

## 근거 맵

| 주장 | 코드 | k6 스크립트 | 결과 |
|---|---|---|---|
| QueryDSL로 DB I/O 3회 → 1회 | [`KeywordRepositoryImpl`](../../apps/api-server/src/main/java/growup/spring/springserver/keyword/repository/KeywordRepositoryImpl.java) | [01-baseline](../../load-test/02-segment-tree-cache/scripts/01-baseline.js), [02-querydsl-refactor](../../load-test/02-segment-tree-cache/scripts/02-querydsl-refactor.js) | [query-count](../../load-test/02-segment-tree-cache/results/query-refactor/query-count.jpg) |
| 커버링 인덱스로 P95 2.97s → 877ms | (인덱스 DDL 미보관, [O4](../known-issues.md)) | [03-six-month-nocache](../../load-test/02-segment-tree-cache/scripts/03-six-month-nocache.js) | — |
| 세그먼트 트리 캐시 | [`LazySegmentTreeService`](../../apps/api-server/src/main/java/growup/spring/springserver/global/cache/LazySegmentTreeService.java) | [05](../../load-test/02-segment-tree-cache/scripts/05-six-month-lazycache.js), [07](../../load-test/02-segment-tree-cache/scripts/07-fixed-range-lazycache.js) | [six-month](../../load-test/02-segment-tree-cache/results/six-month/lazycache.jpg), [fixed-range](../../load-test/02-segment-tree-cache/results/fixed-range) |
| Drill-down 캐시 적중, 첫 요청 최대 2배 지연 | 위와 같음 | [08](../../load-test/02-segment-tree-cache/scripts/08-drilldown-nocache.js), [09](../../load-test/02-segment-tree-cache/scripts/09-drilldown-lazycache.js) | [drilldown](../../load-test/02-segment-tree-cache/results/drilldown) |
| GC·메모리 비교 | — | 08, 09 | [gc-pause](../../load-test/02-segment-tree-cache/results/drilldown/2026-03-02-nocache-vs-cache-gc-pause.jpg), [memory](../../load-test/02-segment-tree-cache/results/drilldown/2026-03-02-nocache-vs-cache-memory.jpg) |
| 비동기 빌드 (Spring Event + CompletableFuture, 500ms 지연) | [`CacheBuildEventListener`](../../apps/api-server/src/main/java/growup/spring/springserver/global/listener/CacheBuildEventListener.java), [`CampaignTotalDataFacade`](../../apps/api-server/src/main/java/growup/spring/springserver/campaign/facade/CampaignTotalDataFacade.java) | 09 | [phase-0 ~ 3](../../load-test/02-segment-tree-cache/results) |
| IO/CPU 스레드 풀 분리, 커넥션 경합 해소 | [`ThreadPoolConfig`](../../apps/api-server/src/main/java/growup/spring/springserver/global/config/ThreadPoolConfig.java), [`application-bottleNeckTest.yml`](../../apps/api-server/src/main/resources/application-bottleNeckTest.yml) | 09 | [thread-pool](../../load-test/02-segment-tree-cache/results/thread-pool), [phase-4 pool 20/30/40](../../load-test/02-segment-tree-cache/results/phase-4) |
| 부분 업데이트와 전체 재빌드 임계값 | [`CampaignDeleteFacade`](../../apps/api-server/src/main/java/growup/spring/springserver/campaign/facade/CampaignDeleteFacade.java), [`TreeCpuBenchmarkTest`](../../apps/api-server/src/test/java/growup/spring/springserver/global/cache/TreeCpuBenchmarkTest.java) | — | — |

## 병목 1: 6개월 조회에서 DB 조회 자체가 문제

- **원인:**
  - 멤버 → 캠페인 → 일별 데이터 순서로 나눠 조회한 뒤 애플리케이션에서 조합하고 있었습니다. 같은 통계를 위해 불필요한 I/O가 생겼습니다.
  - 실행 계획을 보면 Full Scan은 없었지만, 집계 컬럼(`adCost`, `adSales`)을 읽기 위한 Key Lookup이 발생했습니다.

| 조치 | P95 | TPS | Connection Acquire max |
|---|---|---|---|
| 개선 전 | — | 3.87/s | 6s |
| QueryDSL로 조회 통합 (I/O 3 → 1회) | 2.97s | 11.06/s | 2s |
| 커버링 인덱스 | **877ms** | **23.45/s** | 500ms |

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
  - 대신 **첫 요청은 캐시가 없을 때보다 최대 2배 느렸습니다**(트리를 동기로 구성하기 때문).

> 단순 대안(일별 데이터 캐싱 + 선형 합산)과의 실측 비교를 추가해야 합니다. 1년 리프는 365개라 선형 합산도 마이크로초 단위일 수 있습니다.

## 병목 3: 트리 생성 비용

1. **빌드 비동기화.** 기존 흐름은 DB 조회 → 트리 빌드 → 응답이었고, 이를 DB 조회 → 응답 + 비동기 빌드로 바꿨습니다.
   - `@TransactionalEventListener(AFTER_COMMIT)`로 이벤트를 받아 `CompletableFuture`로 실행합니다.
   - 요청 스레드는 4ms 만에 반환됩니다.
   - Kafka/MQ는 단일 서버에서 과하다고 판단했습니다.
2. **Lazy Fetch에서 Whole Build로.** 트리 하나에 최대 12회 나눠 조회하던 방식을, 1년치를 한 번에 조회해 전체 트리를 만드는 방식으로 바꿨습니다.
3. **실행 시점 지연과 자원 분리.**
   - 비동기 빌드가 Tomcat 요청 스레드와 HikariCP 커넥션을 두고 경쟁했습니다.
   - 빌드 시작을 500ms 늦춰(`delayedExecutor`) 사용자 요청의 DB 작업과 겹치지 않게 했습니다.
   - DB 조회는 `ioExecutor`(30), 트리 빌드는 `cpuExecutor`(코어 수 + 1)로 나눴습니다.

| 지표 | Before | After |
|---|---|---|
| P95 | 3.34s | 3.19s |
| Connection Usage Time | 4s | 900ms |
| Connection Acquire Time | 600ms | 80ms |
| Connection Pending | 있음 | 0 |

> **수치 정리 필요**
> - 커버링 인덱스 후 P95 877ms와 이 표의 3.34s는 시나리오가 다릅니다(랜덤 6개월 범위 vs 50명 동시 Drill-down Cold Start). 시나리오를 함께 표기해야 합니다.
> - P95 개선폭은 4.5%로 작습니다. 핵심 성과는 커넥션 지표입니다.
> - Phase 0~3에 각각 무엇을 적용했는지 단계별로 기입해야 합니다.
> - README의 "P95 82% 단축", "Cold Start 50.5% 단축"이 어떤 측정에서 나왔는지 출처를 연결해야 합니다.

## 빠진 내용 (보강 예정)

- 캐시 무효화와 정합성: 당일 데이터 갱신, 삭제 반영([C2~C4](../known-issues.md))
- Eviction: Caffeine `expireAfterAccess 1h`, `maximumSize 5000`(사용자 수 기준, 트리 크기 가중치 없음)
- 캐시 스탬피드: 같은 키 빌드는 `buildingInProgress` Set으로 중복을 막지만, 빌드 중인 동안 조회 처리는 [C7](../known-issues.md)
- 1년 트리 메모리 실측값

## 회고

- 인덱스는 빠른 해결책이지 영구적인 해결책이 아니었습니다.
- 무거운 캐시는 생성 비용과 정합성 관리 부담을 함께 가져왔습니다.
- 작업을 뒤로 미루는 것과 자원을 덜 쓰는 것은 다른 문제였습니다.
- 기술보다 먼저 "이 서비스에 필요한가"를 따져야 했습니다. 이후 분산 환경으로 확장한 결과는 [④ MSA 글로벌 캐시](04-global-cache-msa.md)에 정리했습니다.
