# ① 상품 등록을 크롤링에서 엑셀 업로드로 전환

> 크롤링 대신 엑셀 업로드를 제안했고, 병목을 단계적으로 개선해 **동시 50명·100행 업로드에서 P95 1.92초 → 1.05초(약 45%)**로 줄였습니다(로컬 재측정, 3회 중앙값). 개선 효과는 대부분 Bulk Insert에서 나왔습니다.

> **주의:** 당시에는 500행 파일로 쟀고(P95 1.5초 → 1.08초), 측정 코드에 테스트용 `getRandomCampaign()`이 들어 있었습니다(S3). S3~S5·S7을 고친 뒤 하루 등록 데이터 규모에 맞춰 100행으로 로컬에서 단계별로 다시 쟀습니다([재측정 (2026-10-28, 100행)](#재측정-2026-10-28-100행)). 두 결과는 환경과 행 수가 달라 직접 비교하지 않습니다.

| 항목 | 내용 |
|---|---|
| 서비스 | 쿠팡 개인 셀러용 광고비·마진 분석 대시보드 (GrouUp) |
| 역할 | 백엔드 (2인 팀) |
| 관련 코드 | [`ExcelDownUpLoadController`](../../apps/api-server/src/main/java/growup/spring/springserver/marginforcampaign/controller/ExcelDownUpLoadController.java) → [`ExcelParsingService`](../../apps/api-server/src/main/java/growup/spring/springserver/marginforcampaign/service/ExcelParsingService.java) → [`ExcelService`](../../apps/api-server/src/main/java/growup/spring/springserver/marginforcampaign/service/ExcelService.java) |
| 부하 테스트 | [poc excel-upload.js](../../poc/global-cache-msa/k6Test/excel-upload.js), 다운로드: [excel-download.js](../../load-test/01-excel/scripts/excel-download.js) |

## 문제

- 크롤링 기반 자동 업로드를 구현하던 중 Anti-Bot·IP 차단에 막혔고, 별도 인프라가 필요해져 MVP가 1주 이상 늦어질 상황이 됐습니다.
- HTML 구조가 바뀔 때마다 기능이 깨져 유지보수 비용도 클 것으로 예상했습니다.

## 판단: 요구사항과 구현 방법을 분리

- 셀러 데이터는 원래 엑셀로 제공되므로 사용자에게 익숙한 형식입니다.
- 하루 데이터는 100건 이하로 규모가 작습니다.
- Apache POI로 빠르게 구현할 수 있습니다.

그래서 엑셀 업로드로 방향을 바꾸자고 제안했습니다. 편의성을 지키기 위한 목표는 **동시 사용자 50명, 100행 업로드, 응답 1초 이내**로 잡았습니다.

- 동시 사용자 50명은 목표 PCU입니다.
- 100행은 하루 등록 데이터(100건 이하)에 맞춘 값입니다. 당시 측정은 500행으로 했고, 2026-10-28 재측정부터 100행으로 바꿨습니다.

> 목표 PCU 50의 근거(셀러 수, 피크 시간대)를 한 줄 보강해야 합니다.

## 병목 1: JPA IDENTITY 전략 때문에 INSERT가 한 건씩 실행됨

업로드 한 건(500행)을 Spring `StopWatch`로 5단계로 나눠 쟀습니다. ① MultipartFile → Sheet, ② Context 생성, ③ 파싱, ④ 매칭되지 않는 데이터 제거, ⑤ 삽입. 이 중 **⑤ 삽입이 전체 시간의 64%**를 차지했습니다.

- **원인:** `GenerationType.IDENTITY`를 쓰면 `saveAll()`도 행마다 INSERT를 실행합니다.
- **조치:**
  - JPA를 거치지 않고 `JdbcTemplate.batchUpdate`로 Bulk Insert를 하도록 바꿨습니다(batch size 1000).
  - 같은 변경에서 파싱 루프의 유틸 객체(`DataFormatter`)를 행마다 만들지 않고 재사용하도록 했습니다.
- **결과 (업로드 한 건, `StopWatch`):**

  | 단계 | 개선 전 (`saveAll`) | Bulk Insert + 객체 재사용 |
  |---|---|---|
  | ③ 파싱 | 133ms (30%) | 64ms (67%) |
  | ④ 데이터 제거 | 8ms (2%) | 14ms (14%) |
  | ⑤ 삽입 | **280ms (64%)** | **10.5ms (11%)** |
  | 전체 | 436ms | 96ms |

  삽입이 280ms → 10.5ms로 줄면서 병목이 파싱으로 옮겨갔습니다.
- **재측정 (2026-10-30, 100행, 사용자 1명, `StopWatch`):** 요청 한 건 안에서 어느 단계가 오래 걸리는지 보려고 동시 요청 없이 쟀습니다. 개선 전(v0)과 최종(v2) 구성마다 3회, 회차마다 사용자 10명이 A → B → A 순서로 올렸습니다. 첫 업로드는 INSERT, 이후는 가격만 바뀐 B·A 파일로 UPDATE입니다. 값은 중앙값입니다(INSERT 30건, UPDATE 60건).

  | 단계 | 개선 전 INSERT | 최종 INSERT | 개선 전 UPDATE | 최종 UPDATE |
  |---|---|---|---|---|
  | ① MultipartFile → Sheet | 11.9ms (8%) | 11.0ms (37%) | 11.1ms (25%) | 10.7ms (19%) |
  | ② Context 생성 (내 캠페인·옵션 조회) | 8.5ms (6%) | 11.8ms (40%) | 9.9ms (22%) | 13.9ms (25%) |
  | ③ 파싱 | 5.9ms (4%) | 3.5ms (12%) | 5.9ms (13%) | 3.5ms (6%) |
  | ④ 데이터 제거 | 0.0ms | 0.0ms | 0.1ms | 0.1ms |
  | ⑤ 삽입·수정 | **123.3ms (82%)** | **3.3ms (11%)** | 17.1ms (39%) | 28.3ms (50%) |
  | 합계 | 153.4ms | 30.2ms | 45.9ms | 56.3ms |

  - INSERT는 100행에서도 개선 전에는 ⑤ 삽입이 82%였고, Bulk Insert로 123ms → 3.3ms가 됐습니다. 개선 후에는 ①·② 파일 변환과 DB 조회가 시간의 대부분입니다.
  - UPDATE는 개선 효과가 없었습니다(⑤ 17.1ms → 28.3ms). 측정 프로필(`bottleNeckTest`)에 Hibernate `jdbc.batch_size: 5000`이 있어, 개선 전에도 변경 감지 UPDATE가 배치로 나갑니다. IDENTITY 전략은 INSERT 배치만 막습니다. 운영 프로필(`application-prod.yml`)에는 이 설정이 없어 당시 운영에서는 UPDATE도 한 건씩 나갔을 것입니다.
  - 개선 전 구성의 UPDATE도 실행 시간에 포함되도록 ⑤ 끝에서 `flush()`했습니다. 같은 파일을 다시 올리면 개선 전 구성은 값이 같아 UPDATE를 보내지 않으므로, 가격만 다른 B 파일과 번갈아 올렸습니다.
  - 방법과 원본: [`stage-timing.py`](../../load-test/01-excel/rerun/stage-timing.py)(측정용 jar 빌드·집계, 바꾼 내용은 [stopwatch.patch](../../load-test/01-excel/rerun/patches/stopwatch.patch)), [`excel-upload-stage.js`](../../load-test/01-excel/scripts/excel-upload-stage.js), [results/2026-10-30-stage-timing](../../load-test/01-excel/results/2026-10-30-stage-timing).
- **동시 50명 부하 (k6):** P95 **1.5s**(평균 818ms, 최대 1.56s, 306건 실패 0). 목표 1초에 못 미쳐 병목 2로 이어졌습니다.

> **확인한 것 (2026-10-27)**
> - 이전 판의 "DB IO 0.27ms → 0.06ms"는 위 `StopWatch` 기록과 맞지 않습니다. 원본에서 삽입은 0.28**s** → 0.0105s이고, 0.064s는 개선 후 파싱 시간입니다. 단위와 항목을 바로잡았습니다.
> - 재측정에서는 삽입 구간의 비용을 HikariCP **커넥션 사용 시간**으로도 봤습니다(100행 기준 최대 3.55s → 1.59s).
> - PK는 직접 만들지 않습니다. `mfc_id`는 AUTO_INCREMENT이고, IDENTITY 때문에 JPA가 막던 배치를 `JdbcTemplate.batchUpdate`로 우회한 것입니다. 이전 판의 "PK 직접 생성" 문구는 코드와 다릅니다.
> - JDBC URL에 배치 옵션은 없지만, 쓰는 드라이버(MariaDB Connector/J 3.3.3)는 `useBulkStmts`가 기본으로 켜져 있어 배치를 bulk 프로토콜로 보냅니다. 끄고 잰 결과는 재측정 표에 있습니다(`rewriteBatchedStatements`는 MySQL 드라이버 옵션이라 해당 없음).

## 병목 2: 트랜잭션 범위

- **원인:** 파싱부터 삽입까지 한 트랜잭션이라, DB가 필요 없는 파싱 구간에도 커넥션을 점유했습니다. 병목 1 뒤에는 파싱이 업로드 시간의 67%였으므로 이 낭비가 커졌습니다.
- **조치:** 파싱(`ExcelParsingService`)을 트랜잭션 밖으로 분리하고, `@Transactional`은 쓰기 단계(`ExcelService.processUploadedExcel`)에만 걸었습니다.
- **결과 (동시 50명, k6):** P95 1.5s → **1.08s**, 평균 818ms → 728ms, 최대 1.56s → 1.23s (310건 실패 0). p90은 1.02s였습니다.

## 객체 재사용의 효과

당시에는 `DataFormatter` 재사용을 Bulk Insert와 함께 넣어 따로 재지 않았습니다.

- **근거 (2026-10-28 재측정, 100행):** `DataFormatter`만 다른 두 구성(v2a·v2)을 비교하면 요청당 힙 할당이 **8.7MB → 5.6MB(36% 감소)**, 측정 구간의 GC 횟수가 53회 → 31회로 줄었습니다(Micrometer `jvm_gc_memory_allocated_bytes_total`, `jvm_gc_pause_seconds`). 응답 시간은 P95 1.05s로 같아, 할당은 확실히 줄었지만 지연 개선 효과는 보이지 않았습니다. 500행에서도 같은 경향이었습니다(34.2MB → 18.4MB, P95 차이는 편차 범위 안).

## 결과

| 단계 | 당시 측정 P95 (동시 50명·500행) | 재측정 P95 (동시 50명·100행, 로컬 3회 중앙값) |
|---|---|---|
| 개선 전 | 기록 없음 (업로드 한 건 436ms) | 1.92s (v0) |
| 병목 1 해결 후 (Bulk Insert + `DataFormatter` 재사용) | 1.5s | 977ms (v1, 재사용 제외) |
| 병목 2 해결 후 (+ 트랜잭션 분리) | **1.08s** | **1.05s** (v2) |

- 재측정은 각 개선의 효과를 따로 보려고 순서를 Bulk Insert → 트랜잭션 분리 → `DataFormatter` 재사용으로 나눴습니다(v1 → v2a → v2). 그래서 가운데 행은 당시와 구성이 다릅니다.
- 재측정 환경은 서버·k6·DB를 노트북 한 대에서 함께 돌려 측정 중 시스템 CPU가 85~100%였습니다. 행 수도 다르므로 절대값은 당시 1.08s와 비교하지 않고, 단계 사이의 상대 개선(1.92s → 1.05s, 약 45% 감소)만 봅니다.
- 100행에서는 개선이 거의 모두 Bulk Insert(v0 → v1)에서 나왔고, 그 뒤 단계(v1 → v2)는 P95 범위가 겹쳐 차이가 없었습니다. 당시 500행 측정의 1.5s → 1.08s(28% 감소)와 다른 결과입니다.

## 재측정 (2026-10-28, 100행)

- **코드:** S3~S5·S7을 고친 코드에서 개선 부분만 되돌려 단계별 구성을 재현했습니다. 세 가지 개선이 한 커밋(`52ed226`)에 함께 들어가 단계별 커밋이 없기 때문입니다([`build-variant-jars.py`](../../load-test/01-excel/rerun/build-variant-jars.py), 바꾼 내용은 [patches](../../load-test/01-excel/rerun/patches)).
- **환경:** [load-test/README.md](../../load-test/README.md#테스트-환경-2026-10-27-재측정-환경)와 같습니다(i3-1115G4 노트북, MariaDB 10.11, HikariCP 30).
- **시나리오:** [`excel-upload.js`](../../load-test/01-excel/scripts/excel-upload.js).
  - 예열 5명 10초 뒤 50명이 30초 동안 업로드하고, 응답마다 1초 쉽니다.
  - 각자 자기 캠페인 10개 × 옵션 10개 = 100행 파일을 올립니다([`generate.py`](../../load-test/01-excel/data/generate.py)). 첫 업로드는 INSERT, 이후는 UPDATE입니다.
- **결과 원본:** [results/2026-10-28-100rows](../../load-test/01-excel/results/2026-10-28-100rows). 표는 [`aggregate.py`](../../load-test/01-excel/rerun/aggregate.py)로 만들었습니다. 구성마다 3회이고 회차마다 구성을 돌아가며 쟀습니다. 값은 중앙값(범위)입니다. 15회 모두 실패 0건이었고, 저장 행은 5,000건으로 맞았습니다.
- `v2-nobulk-r3`은 측정 중 PC가 절전에 들어가 k6가 5시간 넘게 이어져, 버리고 다시 쟀습니다.
- 사용자마다 같은 파일을 반복해서 올렸습니다. 그래서 개선 전 구성(v0)의 재업로드는 값이 같아 UPDATE 쿼리가 나가지 않았고, v0의 P95는 주로 사용자별 첫 업로드(INSERT)가 결정합니다. 값이 바뀌는 UPDATE의 단계별 비용은 [병목 1](#병목-1-jpa-identity-전략-때문에-insert가-한-건씩-실행됨)의 1명 재측정에 있습니다.

| 구성 | 트랜잭션 | 저장 | DataFormatter | P95 | 평균 | 요청당 힙 할당 | Acquire 최대 | Usage 최대 |
|---|---|---|---|---|---|---|---|---|
| v0 개선 전 | 파싱 포함 | JPA `saveAll` | 셀마다 생성 | 1.92s (723ms~3.52s) | 405ms | 8.7MB | 2.34s | 3.55s |
| v1 Bulk Insert | 파싱 포함 | `batchUpdate` | 셀마다 생성 | 977ms (839~996ms) | 372ms | 8.8MB | 768ms | 1.59s |
| v2a + 트랜잭션 분리 | 쓰기만 | `batchUpdate` | 셀마다 생성 | 1.05s (1.00~1.11s) | 358ms | 8.7MB | 529ms | 884ms |
| **v2 현재 코드** | 쓰기만 | `batchUpdate` | 재사용 | **1.05s** (942ms~1.06s) | 339ms | 5.6MB | 609ms | 922ms |
| v2 + `useBulkStmts=false` | 쓰기만 | `batchUpdate` | 재사용 | 1.23s (822ms~2.67s) | 598ms | 5.6MB | 686ms | 1.33s |

- **Bulk Insert(v0 → v1):** P95가 1.92s → 977ms로 줄었고, 커넥션 획득 대기 최대 2.34s → 768ms, 사용 최대 3.55s → 1.59s로 함께 줄었습니다. 행마다 INSERT하던 쓰기 구간이 짧아졌기 때문입니다. 다만 v0의 범위가 넓어(723ms~3.52s) 감소 폭은 중앙값 기준입니다.
- **트랜잭션 분리(v1 → v2a):** 커넥션 사용 최대가 1.59s → 884ms로 줄었습니다. 파싱하는 동안 커넥션을 잡지 않기 때문입니다. 하지만 P95는 977ms → 1.05s로 범위가 겹쳐 차이가 없었습니다. 100행은 파싱이 짧아 커넥션 점유로 생기는 대기가 크지 않았습니다.
- **`DataFormatter` 재사용(v2a → v2):** 힙 할당이 36% 줄었지만 응답 시간 차이는 없었습니다.
- **bulk 프로토콜:** 드라이버 기본값(`useBulkStmts` 켜짐)이 끈 경우보다 평균이 낮았지만(339ms vs 598ms) P95 범위가 겹칩니다.
- 측정 중 시스템 CPU가 85~100%였으므로, 남은 지연은 DB 쓰기보다 같은 PC에서 함께 도는 서버·DB·k6의 CPU 경쟁과 엑셀 파싱 비용으로 봅니다.

### 이전 재측정 (2026-10-27, 500행)

같은 방법으로 파일당 500행(캠페인 10개 × 옵션 50개)을 올렸습니다. 결과 원본은 [results/2026-10-rerun](../../load-test/01-excel/results/2026-10-rerun)에 있고, 저장 행은 25,000건으로 맞았습니다.

| 구성 | P95 | 평균 | 요청당 힙 할당 | Acquire 최대 | Usage 최대 |
|---|---|---|---|---|---|
| v0 개선 전 | 7.76s (7.16~10.26s) | 2.75s | 34.5MB | 7.29s | 14.12s |
| v1 Bulk Insert | 4.45s (1.83~5.79s) | 2.21s | 34.4MB | 3.49s | 4.74s |
| v2a + 트랜잭션 분리 | 3.16s (2.41~4.55s) | 1.71s | 34.2MB | 1.51s | 2.64s |
| v2 현재 코드 | 2.84s (2.19~3.77s) | 1.67s | 18.4MB | 1.80s | 2.65s |
| v2 + `useBulkStmts=false` | 3.52s (2.15~6.37s) | 2.09s | 18.5MB | 1.71s | 3.45s |

- 500행에서는 트랜잭션 분리도 P95를 낮췄습니다(4.45s → 3.16s, 범위는 겹침). 파일이 클수록 파싱이 길어지고, 그동안 커넥션을 잡고 있는 비용도 커지기 때문으로 봅니다.

### 수정 과정에서 찾은 동시성 결함 (S7)

S3에서 "엑셀에 없는 옵션 삭제"를 되살리자, 50명 동시 업로드에서 요청의 **51%가 데드락으로 실패**했습니다([로그 발췌](../../load-test/01-excel/results/2026-10-rerun/deadlock-before-s7/app-log-excerpt.txt)).

- **원인:** JPQL `DELETE ... NOT IN`이 `campaign_id` FK 인덱스 범위에 gap lock을 잡습니다. 그래서 캠페인 ID가 인접한 다른 사용자의 INSERT와 서로를 기다렸습니다.
- **수정:** 파싱 때 읽은 기존 옵션에서 지울 행의 PK를 골라 PK로만 지웁니다. 지울 것이 없으면 DELETE를 보내지 않습니다([S7](../known-issues.md)). 수정 후 데드락은 0건입니다.
- 개선 전 코드에도 같은 DELETE가 있었으므로, 당시 측정이 `getRandomCampaign()`과 삭제 로직 주석 처리 상태에서 이뤄진 것과 관련이 있을 수 있습니다.

## 회고

- **요구사항을 구현 방법과 같은 것으로 보지 않기.** 사용자가 원한 것은 크롤링이 아니라 편한 상품 등록이었습니다.
- **기능을 적절한 단위로 나누기.** 나누면 가독성뿐 아니라 트랜잭션 같은 자원의 사용 범위도 좁힐 수 있습니다.

## 예상 질문

- 트랜잭션을 분리한 뒤 일부 행 삽입이 실패하면 어떻게 처리하나요? 원자성과 행 단위 에러 피드백 정책은 무엇인가요?
- POI Workbook은 파일 전체를 메모리에 올립니다. SAX(스트리밍) 방식을 쓰지 않은 이유는 무엇인가요? (재측정에서 업로드 한 번에 100행은 힙 5.6MB, 500행은 18MB를 할당했습니다)
- 동시 업로드에서 삭제와 삽입이 데드락을 일으킨 이유와 해결 방법은 무엇인가요? (S7)
