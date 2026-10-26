# 배포 구성 (운영 종료)

서비스 운영은 종료했습니다. 이 저장소에는 CI/CD 워크플로를 두지 않습니다. 아래는 운영 당시의 배포 방식을 기록으로 남긴 것이고, 워크플로 원본은 git 히스토리에서 확인할 수 있습니다.

| 앱 | 배포 대상 | 트리거 | 방식 | 원본 워크플로 |
|---|---|---|---|---|
| api-server | AWS EC2 | `main` push | GitHub Actions에서 `./gradlew bootJar` 실행 → `scp`로 jar 전송 → `ssh`로 `killall java` 후 `nohup java -jar` 재기동. DB·JWT·OAuth 값은 GitHub Secrets를 실행 인자로 주입 | `.github/workflows/branch-created.yml` (커밋 `9271589`, `50d6d93`에서 제거) |
| excel-parser | AWS Lambda (ap-northeast-2) | `devServer` push | Zappa. Secrets로 `zappa_settings.json`을 만들고 `slim_handler`로 패키징한 뒤 S3 버킷을 거쳐 배포 | `apps/excel-parser/.github/workflows/deploy.yml` (통합 커밋 `b569319` 시점) |
| web | 팀원 fork (`fa7271/FrontServer`) | `main` push | 원본 저장소 push를 fork로 동기화. fork 쪽에서 호스팅 | `apps/web/.github/workflows/sync-to-fork.yml` (통합 커밋 `21e1ce2` 시점) |
| 모니터링 | 로컬 / 서버 Docker | 수동 | Prometheus + Grafana compose. Spring Actuator `/actuator/prometheus` 수집 | [`infra/docker-compose.monitoring.yml`](../infra/docker-compose.monitoring.yml) |

## 운영 당시의 한계

- **api-server:** 무중단 배포가 아니었습니다(프로세스를 내리고 다시 올림). 헬스체크, 롤백, 테스트 단계도 없었습니다.
- **비밀값 노출:** 비밀값을 `java -jar` 인자로 넘겨 프로세스 목록(`ps`)에 노출될 수 있었습니다.
- **JMX:** Docker 이미지가 JMX를 인증 없이 열었습니다([known-issues O1](known-issues.md)).
- **보안 대응:** 운영 중 스캐너 요청에 대응해 WAF와 Rate Limiter를 적용했습니다([③ TIMED_WAITING](portfolio/03-timed-waiting-threads.md)).

## 로컬 실행

배포 없이 로컬에서 실행하는 방법은 [README의 실행 표](../README.md#실행)와 [load-test/README.md](../load-test/README.md)를 참고하세요.
