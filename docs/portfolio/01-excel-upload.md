# ① 상품 등록을 크롤링에서 엑셀 업로드로 전환

> 크롤링 대신 엑셀 업로드를 제안했고, 병목을 두 단계로 개선해 **동시 50명·500행 업로드에서 P95 1.02초**를 기록했습니다. 목표였던 1초에는 0.02초 못 미쳤습니다.

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

> 확인 필요
> - 0.27ms가 행당 값인지 요청 전체 값인지 표기해야 합니다.
> - 현재 코드는 PK를 직접 만들지 않고 `mfc_id` AUTO_INCREMENT에 맡기고 있습니다. 포트폴리오의 "PK 직접 생성" 문구와 맞춰야 합니다.
> - JDBC URL에 `rewriteBatchedStatements`/`useBulkStmts` 같은 배치 옵션이 설정되어 있지 않습니다.

## 병목 2: 트랜잭션 범위와 반복 객체 생성

- **원인:**
  - 파싱부터 삽입까지 한 트랜잭션이라, DB가 필요 없는 파싱 구간에도 커넥션을 점유했습니다.
  - 파싱 루프에서 유틸 객체(`DataFormatter`)를 행마다 새로 만들었습니다.
- **조치:**
  - 파싱(`ExcelParsingService`)을 트랜잭션 밖으로 분리하고, `@Transactional`은 쓰기 단계(`ExcelService.processUploadedExcel`)에만 걸었습니다.
  - `DataFormatter`를 한 번만 만들어 재사용하도록 했습니다.

> GC 오버헤드 주장에는 GC 로그나 JFR 같은 근거 캡처를 추가해야 합니다.

## 결과

| 단계 | P95 (50 PCU · 500행) |
|---|---|
| 개선 전 | (기입 필요) |
| 병목 1 해결 후 | (기입 필요) |
| 병목 2 해결 후 | **1.02s** |

> 재측정 예정: [known-issues S3~S5](../known-issues.md)를 수정한 뒤 단계별로 다시 측정합니다. 현재 코드에는 테스트용 `getRandomCampaign()`이 남아 있습니다.

## 회고

- **요구사항을 구현 방법과 같은 것으로 보지 않기.** 사용자가 원한 것은 크롤링이 아니라 편한 상품 등록이었습니다.
- **기능을 적절한 단위로 나누기.** 나누면 가독성뿐 아니라 트랜잭션 같은 자원의 사용 범위도 좁힐 수 있습니다.

## 예상 질문

- 트랜잭션을 분리한 뒤 일부 행 삽입이 실패하면 어떻게 처리하나요? 원자성과 행 단위 에러 피드백 정책은 무엇인가요?
- POI Workbook은 파일 전체를 메모리에 올립니다. SAX(스트리밍) 방식을 쓰지 않은 이유는 무엇인가요?
