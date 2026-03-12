# 📈 GrouUp (그로우업)
> **이커머스 개인 셀러를 위한 데이터 기반 광고비 통계 및 마진 분석 대시보드**
> 
> 단순한 기능 구현을 넘어, **대용량 데이터 조회 시의 병목을 선제적으로 식별하고 인메모리 캐싱 및 비동기 아키텍처로 성능을 최적화**한 프로젝트입니다.

---

## 팀 및 역할
- **구성:** 2인 팀 프로젝트 (풀스택 개발)
- **My Role (Backend & Architecture Focus):**
  - 서비스 기획 및 전반적인 DB 스키마 설계
  - Spring Boot 기반의 RESTful API 서버 개발
  - **대용량 데이터 조회(6개월 치) 성능 최적화를 위한 인메모리 세그먼트 트리 캐싱 아키텍처 설계**
  - **Spring Event 및 CompletableFuture를 활용한 비동기 논블로킹(Non-blocking) 스레드/커넥션 풀 분리**
  - K6, Prometheus, Grafana를 활용한 데이터 기반 부하 테스트 및 병목 모니터링

---

## 프로젝트 기술 스택

### Backend
<img src="https://img.shields.io/badge/Java 17-007396?style=flat-square&logo=OpenJDK&logoColor=white"/> <img src="https://img.shields.io/badge/Spring-6DB33F?style=flat-square&logo=Spring&logoColor=white"/>

### Database
<img src="https://img.shields.io/badge/MySQL-4479A1?style=flat-square&logo=MySQL&logoColor=white"/> <img src="https://img.shields.io/badge/Redis-DC382D?style=flat-square&logo=Redis&logoColor=white"/> 

### Infrastructure & CI/CD
<img src="https://img.shields.io/badge/Amazon AWS-232F3E?style=flat-square&logo=amazonaws&logoColor=white"/><img src="https://img.shields.io/badge/Docker-2496ED?style=flat-square&logo=Docker&logoColor=white"/> 

### Monitoring & Test
<img src="https://img.shields.io/badge/K6-7D64FF?style=flat-square&logo=k6&logoColor=white"/> <img src="https://img.shields.io/badge/Prometheus-E6522C?style=flat-square&logo=Prometheus&logoColor=white"/> <img src="https://img.shields.io/badge/Grafana-F46800?style=flat-square&logo=Grafana&logoColor=white"/> <img src="https://img.shields.io/badge/Loki-000000?style=flat-square&logo=Grafana&logoColor=white"/> <img src="https://img.shields.io/badge/JUnit5-25A162?style=flat-square&logo=JUnit5&logoColor=white"/>

---

## 시스템 아키텍처
<img width="671" height="808" alt="Image" src="https://github.com/user-attachments/assets/e334bf09-b318-4973-9059-58efb6ccd2a9" />

---

## 핵심 엔지니어링 및 리팩토링 경험

| 핵심 목표  | 문제 상황  | 해결 전략 및 성과 | 핵심 코드 |
|:---|:---|:---|:---:|
| **세부 조회 요청 최적화**<br>`인메모리 캐싱` | 빈번한 사용자 정의 기간 집계 쿼리로 인한 극심한 DB I/O 병목 | 구간 합 쿼리에 최적화된 **세그먼트 트리**를 인메모리에 구현<br>➡️ **P95 응답속도 82% 단축** | [🔗 Link](https://github.com/Hi-Imjaeyoung/Grou-up/blob/7825c75c7788ce7fe8865ccaaf9586d4f5367bf8/src/main/java/growup/spring/springserver/global/cache/LazySegmentTreeService.java#L161C1-L191) |
| **비동기 격벽(Bulkhead)**<br>`초기 로딩(Cold Start) 단축` | 메인 트랜잭션 커밋과 무거운 트리 빌드 작업 간의 HikariCP 커넥션 및 자원 경합 | **Spring Event + 논블로킹 스케줄링(500ms 지연)**으로 스레드 풀 완벽 분리<br>➡️ **초기 로딩(Cold Start) 지연 50.5% 단축** | [🔗 Link](https://github.com/Hi-Imjaeyoung/Grou-up/blob/7825c75c7788ce7fe8865ccaaf9586d4f5367bf8/src/main/java/growup/spring/springserver/global/listener/CacheBuildEventListener.java#L30-L55) |
| **캐시 무효화 전략 최적화**<br>`동적 업데이트 라우팅` | 데이터 변경 시 매번 1년 치 전체 재빌드($O(N)$) 수행에 따른 CPU 낭비 및 GC 오버헤드 | **임계값** 도출로, 부분 업데이트($O(T \log N)$)와 전체 재빌드를 동적 라우팅 적용 | [🔗 Link](https://github.com/Hi-Imjaeyoung/Grou-up/blob/921d95aee9a50a781133144a039b16885956f851/src/test/java/growup/spring/springserver/global/cache/TreeCpuBenchmarkTest.java#L37-L57) |
| **쿼리 실행 계획 개선**<br>`DB 인덱스 튜닝` | 조회 기간 확장(1개월 ➡️ 6개월) 시 대용량 테이블의 **단일 인덱스 스캔 한계로 인한 쿼리 지연** | 파레토 법칙 기반 헤비 유저 더미 데이터 부하 테스트 및 **복합 인덱스** 설계를 통해 **쿼리 속도 80% 계산 (1.7s ➡️ 0.6s)**   | - |

> 💡 **자세한 고민 과정과 아키텍처 설계도는 [포트폴리오 PDF](링크)에서 확인하실 수 있습니다.**
