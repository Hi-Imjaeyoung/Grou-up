# 포트폴리오 수치 재측정·보완 체크리스트

2026-10-26 작성. 포트폴리오(`docs/portfolio/01~04`)의 빈 수치와 "재측정 예정"·"확인 필요" 메모를 채우기 위한 작업 목록입니다.
결함 ID(C1, S3, …)는 [known-issues.md](known-issues.md)를 따릅니다. 항목을 끝내면 `[x]`로 바꾸고 커밋이나 PR 번호를 옆에 적습니다.

## 현재 상태

- [x] C1 세그먼트 트리 객체 공유: #9
- [x] C7 빌드 중·조회만 된 연도가 "빌드됨"으로 보임: #10
- [x] C2/C3 부분 업데이트 미반영·가산: #11
- [x] C8 부분 업데이트·재빌드 선택 조건 반대: 1단계 PR

## 원칙

- **측정 환경이 바뀐다.** 예전 서버 사양은 기록이 없으므로 새 수치는 로컬 PC 기준이다(4코어, k6·Docker·JDK 17 설치됨). 예전 값과 새 값을 한 표에 섞지 않고 Before/After를 같은 환경에서 다시 잰다.
- 예전 캡처는 지우지 않고 "당시 측정(C1 버그 포함)"으로 표시해 남긴다.
- 새 결과는 `load-test/<영역>/results/2026-xx-rerun/`에 저장하고, 스크립트·환경·커밋 해시를 함께 적는다.

## 사용자 결정 필요

- [ ] **측정 DB:** 문서는 MariaDB인데 `infra/docker-compose.yml`은 `mysql:8.0.29`다. MySQL 8 기본 `ONLY_FULL_GROUP_BY`에서는 C5 쿼리가 에러가 난다. 운영 당시 DB에 맞춘다.
- [ ] **커버링 인덱스 정의:** 기억이 없으면 쿼리를 보고 다시 설계하고 문서에 "재설계"라고 밝힌다(O4).
- [ ] **① 목표치 근거:** 셀러 수, 피크 시간대 (코드로 알 수 없음).
- [ ] **Usage 4s 출처:** 이전 판 ②의 "Connection Usage 4s"를 보여 주는 캡처가 없다.
- [ ] **Phase 0~3 변경 내용:** 2026-02-07 측정은 커밋 없이 설정을 바꿔 가며 진행되어 각 Phase의 변경점을 알 수 없다.
- [ ] **PDF "44%" 수치:** 원본 PDF 캐시 섹션에 있지만 ② 문서에는 없다. 무엇의 44%인지 확인.

## 1단계: DB 없이 채울 수 있는 것

- [x] ② 단순 대안 비교 (`RangeQueryBenchmarkTest`, 180일 선형 11.6µs vs 트리 2.3µs): 일별 365개 선형 합산 vs 세그먼트 트리 구간 조회를 JMH로 측정한다. ("단순 대안과의 실측 비교" 메모)
- [x] ② 1년 트리 메모리 실측 (`SegmentTreeMemoryTest`, 유형 3개 375KB): 노드 수와 retained size(JOL 또는 힙 덤프). ("빠진 내용" 메모)
- [x] C8 임계값 (60~90일 사이 역전, 60일 이하 부분 업데이트로 수정): `TreeCpuBenchmarkTest`(updateDays 10~180)를 실행하고 부분 업데이트와 전체 재빌드가 역전되는 지점으로 `CampaignDeleteDto.checkThreshold`를 고친다. 결과를 ② 근거 맵의 "임계값" 행에 넣는다.
- [x] ② "수치 정리 필요" 메모 처리 → "측정 기록" 절로 대체
  - [x] 877ms(랜덤 6개월 범위)와 3.34s(50명 동시 Drill-down Cold Start)의 시나리오를 표에 표기
  - [x] 핵심 성과를 P95(4.5%)가 아니라 커넥션 지표로 서술
  - [ ] (부분) Phase 0~4의 캡처 수치는 표로 정리. 각 Phase에서 바꾼 내용은 커밋에 없어 사용자 확인 필요. Phase 0~4에서 무엇을 바꿨는지 캡처(`load-test/02-segment-tree-cache/results/phase-*`)와 git 히스토리로 채우기
  - [x] README의 "P95 82% / Cold Start 50.5%" 문구는 이미 제거됨 → 출처 연결 메모 삭제 (50.5%는 Phase 0 → 4의 Base P95 3.96s → 1.96s로 확인)
- [x] ② "빠진 내용" → "캐시 운영 정책" 절로 작성. 코드로 쓸 수 있는 것: Eviction 정책, 스탬피드 방지(`buildingInProgress`), C7 반영 내용

## 2단계: 측정 환경 준비

- [ ] C5: `getAllTypeOfCampaignAdCostSumAndAdSalesSumByPeriodAndCampaignIds` GROUP BY에 `date`를 추가한다(트리 입력 데이터의 정합성).
- [ ] C4: 설명이 현재 코드와 맞지 않는다(`extractDeleteCampaignDataByPeriod`는 쿼리 결과를 그대로 반환하고, 설명 내용은 C3와 같다). 다시 확인하고 문서를 정리한다.
- [ ] S1: `CacheBuildEventListener`의 DB 조회를 try/finally 안으로 옮긴다(실패하면 빌드 키가 남아 다시 빌드되지 않음).
- [ ] C9: 업로드(Django)로 들어온 새 데이터가 캐시를 무효화하지 못한다. 재측정과는 무관하지만 ②의 정합성 서술에 영향.
- [ ] C6: 연도를 넘는 조회에서 끝 연도 이벤트를 발행하도록 고친다. 재측정 시나리오(2025-06~12)에는 영향이 없으므로 순서는 자유.
- [ ] O4: 커버링 인덱스 DDL을 저장소에 추가한다(`infra/` 초기화 SQL 또는 엔티티 `@Index`).
- [ ] 측정 DB를 띄우고 `testdatagenerator`로 6개월 약 300만 행을 적재한다. 행 수를 기록한다.
- [ ] `load-test/README.md` 테스트 환경 표를 채운다(CPU, RAM, OS, DB 버전, 데이터량, HikariCP, ioExecutor/cpuExecutor).
- [ ] 모니터링(`infra/docker-compose.monitoring.yml`, Prometheus·Grafana)이 커넥션 지표를 수집하는지 확인한다.

## 3단계: ② 재측정

- [ ] 03 `six-month-nocache` vs 05 `six-month-lazycache` (커버링 인덱스 적용)
- [ ] 인덱스 제거 상태로 03 → 커버링 인덱스 Before/After(2.97s → 877ms 주장)
- [ ] 06/07 고정 범위 nocache vs lazycache
- [ ] 08 `drilldown-nocache` vs 09 `drilldown-lazycache`: 첫 요청 지연, Cache Hit율, DB 접근 횟수
- [ ] 09에서 HikariCP pool 20/30/40, Connection Acquire/Usage/Pending 캡처
- [ ] ② 결과 표를 새 수치로 바꾸고 "재측정 예정" 주의 메모를 갱신한다. 루트 README 요약 문구도 맞춘다.

## 4단계: ① 엑셀 업로드 수정 후 측정

- [ ] S3: `getRandomCampaign()` 제거, 엑셀에 없는 옵션 삭제 로직 복원 여부 결정
- [ ] S4: IOException 시 null 반환, 빈 행 NPE, `CAMPAIGN_NOT_FOUND`가 나지 않는 문제, 중복 상품명 중복 INSERT
- [ ] S5: 업로드 실패 시 4xx/5xx를 반환
- [ ] 단계별 커밋 찾기: 개선 전 / Bulk Insert 적용 후 / 트랜잭션 분리 후
- [ ] 각 단계에서 50 PCU·500행 업로드(`poc/global-cache-msa/k6Test/excel-upload.js`)를 측정 → "(기입 필요)" 채우기
- [ ] JDBC `rewriteBatchedStatements=true`(MySQL) / `useBulkStmts`(MariaDB)를 추가하고 한 단계 더 측정한다(목표 1초까지 0.02초 부족한 부분)
- [ ] 확인 메모 처리: 0.27ms가 행당인지 요청 전체인지 표기, "PK 직접 생성" 문구를 코드(`mfc_id` AUTO_INCREMENT)와 맞추기
- [ ] GC 오버헤드 주장 근거: `DataFormatter` 재사용 전/후 JFR 또는 GC 로그 캡처

## 5단계: ③ TIMED_WAITING 재검증

- [ ] k6로 순간 동시 연결 200 이상 발생시키는 스크립트 작성 → `load-test/04-timed-waiting/`
- [ ] `jstack`에서 TIMED_WAITING 스택이 `TaskQueue.poll`인지 확인하고 저장
- [ ] 유휴 60초 후 스레드 수 감소를 Grafana로 캡처
- [ ] 401 flood 스크립트와 VisualVM 캡처도 남긴다
- [ ] 결과에 따라 ③ 결론과 WAF·Rate Limiter 역할 서술을 다시 쓴다

## 6단계: ④ MSA PoC (우선순위 낮음)

- [ ] P1: legacy-server 객체 공유 버그 수정(C1과 같은 수정)
- [ ] P2: `sum()` null 검사 순서, max 초기값 `Double.MIN_VALUE`
- [ ] P3: `docker/master-init.sql` 디렉터리 → 파일
- [ ] legacy 기준값과 MSA 1~3회차를 다시 측정한다(JVM 6개를 한 머신에서 돌리므로 비용이 큼)
- [ ] P4: README의 Kafka/RabbitMQ 설명과 5.3절 구조를 현재 브랜치에 맞춘다
