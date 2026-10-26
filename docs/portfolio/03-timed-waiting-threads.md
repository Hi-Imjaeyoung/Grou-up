# ③ CPU는 정상인데 스레드는 왜 200개까지 늘었을까

> 운영 서버에서 TIMED_WAITING 스레드가 200개 가까이 늘어난 이상 지표를 관측했습니다. 애플리케이션 내부 가설 두 가지를 검증했지만 재현되지 않았고, WAF와 Rate Limiter로 비정상 요청을 서버 앞단에서 차단했습니다.

> **재검증 예정:** 200은 Spring Boot 내장 Tomcat의 기본 `max-threads` 값입니다. 이 프로젝트에는 `server.tomcat.threads` 설정이 없습니다([application yml](../../apps/api-server/src/main/resources)). 순간 버스트로 워커 풀이 최대치까지 늘어난 뒤, 유휴 스레드가 `TaskQueue.poll(timeout)`에서 정리(기본 60초)를 기다리는 정상 동작일 가능성이 높습니다. 아래 "재검증 계획"으로 확인한 뒤 이 문서를 다시 씁니다.

## 관측 (2025-09-13 06:39~06:40)

| 지표 | 값 | 평소 |
|---|---|---|
| TIMED_WAITING 스레드 | ~200 | 5 이하 |
| HTTP duration max | 500ms | — |
| I/O rate | ~20 ops/s | — |
| CPU | 정상 | — |
| 요청 | `GET /frontend/.env`, `GET /conf.php.swp` 등 스캐너 요청 | — |

## 가설과 검증

| 가설 | 검증 방법 | 결과 |
|---|---|---|
| 1. 로그 기록 I/O가 스레드 대기를 유발했다 | k6로 401 요청을 대량 발생, 로그 on/off 비교, VisualVM(JMX)으로 관측 | 부하 차이는 있었지만 TIMED_WAITING 증가는 재현되지 않음 |
| 2. 인증·인가 처리의 블로킹 I/O | 코드 확인(DB·외부 호출 없음) + 401 대량 요청 | CPU ~100%, TIMED_WAITING ~40개. 재현되지 않음 |

> 가설 1 보강: 비동기 Appender는 큐가 가득 차지 않으면 워커 스레드를 블로킹하지 않습니다. 현재 [`logback-spring.xml`](../../apps/api-server/src/main/resources/logback-spring.xml)은 동기 `RollingFileAppender`를 씁니다.

> 401 flood k6 스크립트와 VisualVM 캡처는 저장소에 없습니다. 재검증할 때 함께 남길 예정입니다.

## 조치와 결과

- 애플리케이션 앞단에 WAF와 Rate Limiter를 적용해 비정상 요청을 서버 외부에서 차단했습니다. 월 평균 약 20,000건을 차단했습니다.
- 원인을 확정하지 못했으므로 "재발 방지"가 아니라 **이상 지표 완화**로 표현합니다.

## 재검증 계획

1. k6로 동시 연결을 순간적으로 200 이상 발생시킵니다.
2. `jstack`으로 TIMED_WAITING 스레드의 스택이 `org.apache.tomcat.util.threads.TaskQueue.poll`인지 확인합니다.
3. 유휴 60초 후 스레드 수가 줄어드는지 Grafana로 함께 관측합니다.
4. 2~3이 확인되면, 결론을 "원인을 못 찾음"에서 **가설을 반복 검증해 원인을 찾아냄**으로 바꿉니다. 그 다음 WAF·Rate Limiter의 역할을 다시 정의합니다.

## 회고

- 보안은 민감 데이터 보호를 넘어 가용성을 지키는 역할도 합니다.
- 병목은 코드뿐 아니라 실행 환경(스레드 풀 기본값, OS, 네트워크)에도 있습니다.
- JVM, 애플리케이션, 네트워크 계층별 지표를 함께 볼 수 있는 관측 환경이 필요합니다.
