# Known Issues

2026-10-26 코드 분석에서 확인한 결함 목록입니다. 포트폴리오 수치를 재측정하기 전에 고쳐야 할 대상입니다.
경로는 모두 `apps/api-server/src/main/java/growup/spring/springserver/` 기준입니다.

## 정합성: 결과 값이 틀림 (최우선)

| # | 위치 | 문제 | 재현 / 근거 |
|---|---|---|---|
| C1 ✅ 해결 (`ec6fc8b`) | `global/cache/LazySegmentTreeService.java` `buildTree` | `leftValue.add(rightValue)`에서 `AllCampaignTypeData.add()`가 자기 자신을 수정해 반환한다. 그래서 부모 노드와 가장 왼쪽 자손 노드가 같은 객체를 공유한다. | 1/1~1/10 데이터를 하루 광고비 10, 매출 100으로 빌드하고 조회한 결과: 1/1 단일 조회가 **100/1000(10배)**, 1/1~1/10 조회가 **150/1500**으로 나온다. 기대값은 각각 10/100, 100/1000. → 부모 노드를 새 객체를 만드는 `sum()`으로 생성하도록 수정. 재현 테스트 `LazySegmentTreeServiceBuildTest`. |
| C2 | 같은 파일 `findAndUpdateTargetNode` | 삭제를 반영할 때 값을 빼지 않고 `add`로 더한다. C1의 객체 공유 때문에 같은 객체에 여러 번 더해지기도 한다. | 코드 확인 |
| C3 | 같은 파일 `updateTreeByPeriodData(List<Tuple>)` | 결과 맵을 만드는 루프가 `map.put`을 하지 않아, 부분 업데이트가 아무것도 반영하지 않는다. | 코드 확인 |
| C4 | `keyword/service/KeywordService.java` `extractDeleteCampaignDataByPeriod` | `sum()` 반환값을 버리고 맵에 넣지 않아 항상 빈 맵을 반환한다. | 코드 확인 |
| C5 | `keyword/repository/KeywordRepositoryImpl.java` `getAllTypeOfCampaignAdCostSumAndAdSalesSumByPeriodAndCampaignIds` | `date`를 SELECT 하지만 GROUP BY에는 `camAdType`만 있다. MariaDB가 날짜를 임의로 골라 반환한다. | 코드 확인 |
| C6 | `campaign/facade/CampaignTotalDataFacade.java` | 연도를 넘는 조회에서 `end.getYear()` 대신 `start.getYear()`로 빌드 이벤트를 두 번 발행한다. | 코드 확인 |
| C7 ✅ 해결 (`762e2ce`) | `LazySegmentTreeService.isTreeBuild` / `buildTree` | 빌드 시작 시 연도 엔트리를 먼저 만들어, 빌드 도중에도 "빌드됨"으로 보인다. 그 사이 요청은 빈 노드를 0으로 합산한다. 시작 연도만 확인하는 문제도 있다. | 코드 확인 → 트리를 별도 객체에 다 만든 뒤 등록하고, 조회 시 빈 연도 엔트리를 만들지 않으며, 시작·끝 연도를 모두 확인하도록 수정. 2년을 넘는 구간은 중간 연도가 합산되지 않으므로 캐시를 쓰지 않는다. C6이 고쳐지기 전까지 연도를 넘는 조회는 끝 연도 트리가 만들어지지 않아 DB로 조회한다. 재현 테스트 `LazySegmentTreeServiceBuildStateTest`, `CampaignTotalDataFacadeTest`. |
| C8 | `campaign/dto/CampaignDeleteDto.checkThreshold` | 300일 **이상**일 때 부분 업데이트를 고른다. 부분 업데이트 비용이 O(T log N)이라면 큰 범위일수록 전체 재빌드가 유리하므로 조건이 반대일 가능성이 있다. | `TreeCpuBenchmarkTest` 결과와 대조 필요 |

## 안정성

| # | 위치 | 문제 |
|---|---|---|
| S1 | `global/listener/CacheBuildEventListener.java` | DB 조회가 try/finally 밖에 있다. 조회가 실패하면 빌드 중 표시 키가 남아 해당 사용자·연도 트리가 다시는 빌드되지 않는다. |
| S2 | `global/cache/AllCampaignTypeData.java` | 내부가 `HashMap`인데 업데이트 스레드와 조회 스레드가 동시에 접근한다. |
| S3 | `marginforcampaign/service/ExcelParsingService.java` | 실제 캠페인 ID 대신 테스트용 `getRandomCampaign()`을 쓴다. 엑셀에 없는 옵션을 삭제하는 로직도 주석 처리되어 있다. |
| S4 | 같은 파일, `ExcelUploadContext` | IOException이 나면 null을 반환한다. 빈 행이 있으면 NPE로 업로드 전체가 실패한다. `Map.get`은 예외를 던지지 않으므로 `CAMPAIGN_NOT_FOUND`가 발생하지 않는다. 같은 상품명이 중복되면 중복 INSERT가 된다. |
| S5 | `marginforcampaign/controller/ExcelDownUpLoadController.java` | 업로드에 실패해도 HTTP 200을 반환한다. |
| S6 | `apps/batch-server` | `beanMapped()`는 record와 호환되지 않고, `ad_sales`와 `ad_sale` 이름이 다르다. 행 단위 upsert가 누적 덧셈이라 다시 실행하면 이중 집계된다. `monthly_summary` DDL도 없다. |

## 테스트

| # | 문제 |
|---|---|
| T1 | `apps/api-server`에서 `./gradlew test`를 실행하면 321개 중 113개가 실패합니다(통합 전 커밋 `d035a65`과 같은 결과). 주요 원인은 다음과 같습니다. ① `@ActiveProfiles("test")`가 없는 테스트가 기본 프로필 `bottleNeckTest`로 실제 MariaDB(`${DATASOURCE_URL}`)를 찾습니다. ② `testdatagenerator`는 테스트가 아니라 데이터 적재 스크립트입니다. ③ 컨트롤러 슬라이스 테스트에 `CampaignDeleteFacade` 등의 Mock이 없습니다. ④ `passwordEncoder` 빈이 중복 정의되어 있습니다. `SPRING_PROFILES_ACTIVE=test`로 실행해도 109개가 실패합니다. 운영 종료로 CI는 두지 않았으므로 로컬에서 확인합니다. |

## 보안 / 운영

| # | 위치 | 문제 |
|---|---|---|
| O1 | `apps/api-server/Dockerfile` | JMX를 인증·SSL 없이 9010 포트로 노출한다. |
| O2 | `hibernate/controller/HibernateStatController`, `global/error/DummyControllers`, `/api/campaign/cacheReset` | 테스트용 엔드포인트가 운영 코드에 남아 있다. |
| O3 | `application.yml` | 기본 프로필이 `bottleNeckTest`이고, 모든 프로필이 `ddl-auto: update`이다. |
| O4 | 엔티티 | 포트폴리오의 커버링 인덱스가 코드나 마이그레이션 파일에 없다. 재현이 불가능하다. |

## PoC (`poc/global-cache-msa`)

| # | 위치 | 문제 |
|---|---|---|
| P1 | `growup-legacy-server/.../LegacyLazySegmentTreeService.java` | C1과 같은 객체 공유 버그가 있다. MSA와 비교하는 기준값(legacy)이 오염되어 있다. |
| P2 | `growup-common/.../AllCampaignTypeData.java` | `sum()`이 null 검사 전에 `other` 필드에 접근한다. max 초기값으로 `Double.MIN_VALUE`(양수)를 써서 음수를 처리하지 못한다. |
| P3 | `docker/master-init.sql` | 파일이 아니라 디렉터리라 초기화 스크립트가 실행되지 않는다. |
| P4 | `README.md` | 본문은 Kafka를 설명하는데 현재 브랜치에는 RabbitMQ가 추가되어 있다. 5.3절은 V1.1 이전 구조를 설명한다. |
