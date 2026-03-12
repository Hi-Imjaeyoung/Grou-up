# 📈 GrouUp (그로우업)
> **이커머스 개인 셀러를 위한 데이터 기반 광고비 통계 및 마진 분석 대시보드**
> 
> 단순한 기능 구현을 넘어, **대용량 데이터 조회 시의 병목을 선제적으로 식별하고 인메모리 캐싱 및 비동기 아키텍처로 성능을 최적화**한 프로젝트입니다.

---

## 👨‍💻 Team & My Role
- **구성:** 2인 팀 프로젝트 (풀스택 개발)
- **My Role (Backend & Architecture Focus):**
  - 서비스 기획 및 전반적인 DB 스키마 설계
  - Spring Boot 기반의 RESTful API 서버 개발
  - **대용량 데이터 조회(6개월 치) 성능 최적화를 위한 인메모리 세그먼트 트리 캐싱 아키텍처 설계**
  - **Spring Event 및 CompletableFuture를 활용한 비동기 논블로킹(Non-blocking) 스레드/커넥션 풀 분리**
  - K6, Prometheus, Grafana를 활용한 데이터 기반 부하 테스트 및 병목 모니터링

---

## 🛠 Tech Stack

### Backend
<img src="https://img.shields.io/badge/Java 17-007396?style=flat-square&logo=OpenJDK&logoColor=white"/> <img src="https://img.shields.io/badge/Spring-6DB33F?style=flat-square&logo=Spring&logoColor=white"/>

### Database
<img src="https://img.shields.io/badge/MySQL-4479A1?style=flat-square&logo=MySQL&logoColor=white"/> <img src="https://img.shields.io/badge/Redis-DC382D?style=flat-square&logo=Redis&logoColor=white"/> 

### Infrastructure & CI/CD
<img src="https://img.shields.io/badge/Amazon AWS-232F3E?style=flat-square&logo=amazonaws&logoColor=white"/><img src="https://img.shields.io/badge/Docker-2496ED?style=flat-square&logo=Docker&logoColor=white"/> 

### Monitoring & Test
<img src="https://img.shields.io/badge/K6-7D64FF?style=flat-square&logo=k6&logoColor=white"/> <img src="https://img.shields.io/badge/Prometheus-E6522C?style=flat-square&logo=Prometheus&logoColor=white"/> <img src="https://img.shields.io/badge/Grafana-F46800?style=flat-square&logo=Grafana&logoColor=white"/> <img src="https://img.shields.io/badge/Loki-000000?style=flat-square&logo=Grafana&logoColor=white"/> <img src="https://img.shields.io/badge/JUnit5-25A162?style=flat-square&logo=JUnit5&logoColor=white"/>

---

## 🏛 System Architecture
<img width="671" height="808" alt="Image" src="https://github.com/user-attachments/assets/e334bf09-b318-4973-9059-58efb6ccd2a9" />

---

## 🔥 핵심 엔지니어링 및 리팩토링 경험

### 1. 세그먼트 트리 기반 인메모리 캐싱 아키텍처 도입
- **문제:** 대시보드 조회 기간 확장(1개월 ➡️ 6개월)에 따라 대용량 데이터 조회 시 DB I/O 병목 현상 발생.
- **해결:** 단순 K-V 캐싱이 아닌, 구간 합 쿼리(Range Query)에 최적화된 **'세그먼트 트리(Segment Tree)' 구조를 인메모리에 직접 구현**하여 캐시 적중률과 조회 성능을 극대화함.
- **결과:** $O(N)$의 조회 시간복잡도를 $O(\log N)$으로 단축, 중복 구간 조회 시 Cache Hit 100% 당성 및 **P95 응답속도 최대 82% 단축 (1.7s ➡️ 0.6s)**.
- 💡 **[💻 Segment Tree 핵심 구현 코드 보기](https://github.com/Hi-Imjaeyoung/Grou-up/blob/7825c75c7788ce7fe8865ccaaf9586d4f5367bf8/src/main/java/growup/spring/springserver/global/cache/LazySegmentTreeService.java#L161C1-L191)**

### 2. Spring Event & 논블로킹(Non-blocking) 기반 비동기 격벽(Bulkhead) 패턴
- **문제:** 대용량 트리 초기 빌드 시, 메인 스레드와 비동기 스레드 간의 HikariCP DB 커넥션 및 CPU 자원 경합(Resource Contention) 발생.
- **해결:** - `@TransactionalEventListener(AFTER_COMMIT)`을 활용한 **이벤트 기반 설계(EDD)** 로 핵심 비즈니스 로직(Facade)과 캐시 동기화 책임을 완벽히 분리.
  - `CompletableFuture.delayedExecutor()`를 도입하여 스레드를 기절시키는(Blocking) `sleep()` 대신 **논블로킹 스케줄링(500ms 지연)** 적용.
  - 상태 확인 및 DB 조회는 **I/O 스레드 풀**, 무거운 트리 빌드 연산은 **CPU 스레드 풀**로 역할을 격리(Bulkhead).
- **결과:** 초기 로딩 지연(Cold Start) 시간 **50.5% 단축** 및 안정적인 트래픽 처리 환경 구축.
- 💡 **[💻 비동기 스레드 제어 및 Event 리스너 코드 보기](https://github.com/Hi-Imjaeyoung/Grou-up/blob/7825c75c7788ce7fe8865ccaaf9586d4f5367bf8/src/main/java/growup/spring/springserver/global/listener/CacheBuildEventListener.java#L30-L55)**

### 3. 임계값(Threshold) 기반 동적 캐시 업데이트/재빌드 전략
- **문제:** 데이터 삭제/수정 발생 시, 매번 1년 치 전체 트리를 다시 빌드($O(N)$)하는 것은 CPU 자원 낭비 및 GC 오버헤드를 유발.
- **해결:** 삭제되는 데이터의 양과 연산을 분석하여 **임계값(Threshold)** 을 도출.
  - 임계값 미만: 기존 트리에서 해당 구간만 부분 업데이트 ($O(T \log N)$).
  - 임계값 초과: 부분 업데이트 비용이 전체 재빌드보다 커지는 크로스오버 지점에서는 트리를 비동기로 전체 재빌드.
- **결과:** 상황에 맞는 동적 캐시 무효화/갱신 전략을 통해 시스템 리소스 낭비 최소화.
- 💡 **[💻 동적 캐시 업데이트 분기 로직 코드 보기]**(여기에 Facade나 Event 처리 분기 코드 링크 삽입)

### 4. 데이터 기반 스트레스 테스트 및 DB 인덱스 최적화
- **문제:** 서비스 오픈 전, 타겟 트래픽(PCU 50명) 도달 시 잠재적 병목 파악 필요.
- **해결:** 실제 데이터 누적 추세를 분석하고, 파레토 법칙에 기반한 상위 5% 헤비 유저의 더미 데이터를 구축하여 K6 부하 테스트 진행. 실행 계획(Explain) 분석을 통해 서비스 구조에 맞춘 복합 인덱스(Composite Index) 설계 및 적용.
- **결과:** 잠재적 DB 병목을 선제적으로 검증하고 쿼리 성능 최적화 완료.
- 💡 **[💻 K6 부하 테스트 스크립트 / 인덱스 튜닝 쿼리 보기]**(관련 폴더나 파일 링크 삽입))*
