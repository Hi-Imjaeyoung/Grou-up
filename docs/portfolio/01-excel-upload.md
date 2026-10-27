# ① 상품 등록을 크롤링에서 엑셀 업로드로 전환

> 크롤링 대신 엑셀 업로드를 제안했고, 병목을 두 단계로 개선해 **동시 50명·500행 업로드에서 P95 1.02초**를 기록했습니다. 목표였던 1초에는 0.02초 못 미쳤습니다.

> **주의:** 1.02초는 당시 서버에서 잰 값이고, 측정 코드에는 테스트용 `getRandomCampaign()`이 들어 있었습니다(S3). S3~S5·S7을 고친 뒤 로컬 PC에서 단계별로 다시 쟀고, 결과는 [재측정 (2026-10-27)](#재측정-2026-10-27)에 따로 정리했습니다. 두 결과는 환경이 달라 직접 비교하지 않습니다.

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

그래서 엑셀 업로드로 방향을 바꾸자고 제안했습니다. 편의성을 지키기 위한 목표는 **동시 사용자 50명, 500행 업로드, 응답 1초 이내**로 잡았습니다.

> 목표치 근거(셀러 수, 피크 시간대)를 한 줄 보강해야 합니다.

## 병목 1: JPA IDENTITY 전략 때문에 INSERT가 한 건씩 실행됨

업로드 과정을 5단계로 나눠 측정했습니다. ① MultipartFile → Sheet, ② Context 생성, ③ 파싱, ④ 매칭되지 않는 데이터 제거, ⑤ 삽입. 이 중 **⑤ 삽입이 전체 시간의 64%**를 차지했습니다.

- **원인:** `GenerationType.IDENTITY`를 쓰면 `saveAll()`도 행마다 INSERT를 실행합니다.
- **조치:** JPA를 거치지 않고 `JdbcTemplate.batchUpdate`로 Bulk Insert를 하도록 바꿨습니다(batch size 1000).
- **결과:** DB IO 0.27ms → 0.06ms.

> **확인한 것 (2026-10-27)**
> - 0.27ms → 0.06ms가 행당 값인지 요청 전체 값인지는 측정 방법이 남아 있지 않아 확인할 수 없습니다. 재측정에서는 대신 HikariCP **커넥션 사용 시간**으로 삽입 구간의 비용을 봤습니다(최대 14.1s → 4.7s).
> - PK는 직접 만들지 않습니다. `mfc_id`는 AUTO_INCREMENT이고, IDENTITY 때문에 JPA가 막던 배치를 `JdbcTemplate.batchUpdate`로 우회한 것입니다. 이전 판의 "PK 직접 생성" 문구는 코드와 다릅니다.
> - JDBC URL에 배치 옵션은 없지만, 쓰는 드라이버(MariaDB Connector/J 3.3.3)는 `useBulkStmts`가 기본으로 켜져 있어 배치를 bulk 프로토콜로 보냅니다. 끄고 잰 결과는 재측정 표에 있습니다(`rewriteBatchedStatements`는 MySQL 드라이버 옵션이라 해당 없음).

## 병목 2: 트랜잭션 범위와 반복 객체 생성

- **원인:**
  - 파싱부터 삽입까지 한 트랜잭션이라, DB가 필요 없는 파싱 구간에도 커넥션을 점유했습니다.
  - 파싱 루프에서 유틸 객체(`DataFormatter`)를 행마다 새로 만들었습니다.
- **조치:**
  - 파싱(`ExcelParsingService`)을 트랜잭션 밖으로 분리하고, `@Transactional`은 쓰기 단계(`ExcelService.processUploadedExcel`)에만 걸었습니다.
  - `DataFormatter`를 한 번만 만들어 재사용하도록 했습니다.

- **근거 (2026-10-27 재측정):** `DataFormatter`만 다른 두 구성(v2a·v2)을 비교하면 요청당 힙 할당이 **34.2MB → 18.4MB(46% 감소)**, 30초 동안 GC 횟수가 52회 → 40회로 줄었습니다(Micrometer `jvm_gc_memory_allocated_bytes_total`, `jvm_gc_pause_seconds`). 다만 응답 시간 차이(P95 3.16s → 2.84s)는 측정 편차 범위 안이라, 할당은 확실히 줄었지만 지연 개선 효과는 작습니다.

## 결과

| 단계 | 당시 측정 P95 | 재측정 P95 (로컬, 3회 중앙값) |
|---|---|---|
| 개선 전 | 기록 없음 | 7.76s |
| 병목 1 해결 후 (Bulk Insert) | 기록 없음 | 4.45s |
| 병목 2 해결 후 (트랜잭션 분리 + `DataFormatter` 재사용) | **1.02s** | **2.84s** |

재측정 환경은 서버·k6·DB를 노트북 한 대에서 함께 돌려 모든 구성에서 CPU가 100%였습니다. 그래서 절대값은 당시 1.02s와 비교하지 않고, 단계 사이의 상대 개선(7.76s → 2.84s, 63% 감소)만 봅니다.

## 재측정 (2026-10-27)

- **코드:** S3~S5·S7을 고친 코드에서 개선 부분만 되돌려 단계별 구성을 재현했습니다. 세 가지 개선이 한 커밋(`52ed226`)에 함께 들어가 단계별 커밋이 없기 때문입니다([`build-variant-jars.py`](../../load-test/01-excel/rerun/build-variant-jars.py), 바꾼 내용은 [patches](../../load-test/01-excel/rerun/patches)).
- **환경:** [load-test/README.md](../../load-test/README.md#테스트-환경-2026-10-27-재측정-환경)와 같습니다(i3-1115G4 노트북, MariaDB 10.11, HikariCP 30).
- **시나리오:** [`excel-upload.js`](../../load-test/01-excel/scripts/excel-upload.js).
  - 예열 5명 10초 뒤 50명이 30초 동안 업로드하고, 응답마다 1초 쉽니다.
  - 각자 자기 캠페인 10개 × 옵션 50개 = 500행 파일을 올립니다. 첫 업로드는 INSERT, 이후는 UPDATE입니다.
- **결과 원본:** [results/2026-10-rerun](../../load-test/01-excel/results/2026-10-rerun). 표는 [`aggregate.py`](../../load-test/01-excel/rerun/aggregate.py)로 만들었습니다. 구성마다 3회이고 중앙값(범위)입니다. 15회 모두 실패 0건이었고, 저장 행은 25,000건으로 맞았습니다.

| 구성 | 트랜잭션 | 저장 | DataFormatter | P95 | 평균 | 요청당 힙 할당 | Acquire 최대 | Usage 최대 |
|---|---|---|---|---|---|---|---|---|
| v0 개선 전 | 파싱 포함 | JPA `saveAll` | 셀마다 생성 | 7.76s (7.16~10.26s) | 2.75s | 34.5MB | 7.29s | 14.12s |
| v1 Bulk Insert | 파싱 포함 | `batchUpdate` | 셀마다 생성 | 4.45s (1.83~5.79s) | 2.21s | 34.4MB | 3.49s | 4.74s |
| v2a + 트랜잭션 분리 | 쓰기만 | `batchUpdate` | 셀마다 생성 | 3.16s (2.41~4.55s) | 1.71s | 34.2MB | 1.51s | 2.64s |
| **v2 현재 코드** | 쓰기만 | `batchUpdate` | 재사용 | **2.84s** (2.19~3.77s) | 1.67s | 18.4MB | 1.80s | 2.65s |
| v2 + `useBulkStmts=false` | 쓰기만 | `batchUpdate` | 재사용 | 3.52s (2.15~6.37s) | 2.09s | 18.5MB | 1.71s | 3.45s |

- **Bulk Insert(v0 → v1):** 커넥션 사용 최대가 14.1s → 4.7s로 가장 크게 줄었습니다. 행마다 INSERT하던 쓰기 구간이 짧아졌기 때문입니다.
- **트랜잭션 분리(v1 → v2a):** 파싱 중에는 커넥션을 잡지 않아 획득 대기 최대가 3.5s → 1.5s로 줄었습니다. P95는 4.45s → 3.16s이지만 v1의 범위가 넓어(1.83~5.79s) 크기는 단정하지 않습니다.
- **`DataFormatter` 재사용(v2a → v2):** 힙 할당이 절반 가까이 줄었지만 응답 시간 차이는 편차 안이었습니다.
- **bulk 프로토콜:** 드라이버 기본값(`useBulkStmts` 켜짐)이 끈 경우보다 평균이 낮았지만(1.67s vs 2.09s) P95 범위가 겹칩니다. 남은 지연의 주 원인은 DB 쓰기보다 CPU 포화(엑셀 파싱)로 보입니다.

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
- POI Workbook은 파일 전체를 메모리에 올립니다. SAX(스트리밍) 방식을 쓰지 않은 이유는 무엇인가요? (재측정에서 500행 업로드 한 번에 힙 18MB를 할당했습니다)
- 동시 업로드에서 삭제와 삽입이 데드락을 일으킨 이유와 해결 방법은 무엇인가요? (S7)
