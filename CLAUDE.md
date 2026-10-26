# CLAUDE.md

GrouUp 포트폴리오 모노레포에서 작업할 때 필요한 맥락입니다.

## 현재 상태 (2026-10-26)

- **운영 종료.** 배포하지 않고 CI/CD도 두지 않는다. 검증은 로컬 빌드·테스트로만 하고, 배포·인프라 내용은 `docs/`에 문서로 남긴다(`docs/deployment.md`).
- **목적:** 포트폴리오(`docs/portfolio/`)의 각 주장을 코드·k6 스크립트·결과 캡처로 뒷받침하는 것.
- **진행 중인 일:** `docs/known-issues.md`의 결함을 고친 뒤 부하를 재측정하고 포트폴리오 수치를 갱신한다.

## 구조

| 경로 | 내용 | 빌드 |
|---|---|---|
| `apps/api-server` | Spring Boot 3.3.5 모놀리스. 패키지 `growup.spring.springserver` | `./gradlew build` (Java 17) |
| `apps/batch-server` | Spring Batch backfill 프로토타입 | `./gradlew compileJava` |
| `apps/web` | React (CRA) | `npm ci && npm run build` |
| `apps/excel-parser` | Django 엑셀 파서 | `python manage.py check` (`SECRET_KEY`, `DB_*` 환경변수 필요) |
| `poc/global-cache-msa` | MSA/CQRS 글로벌 캐시 PoC, Boot 3.2.3 멀티모듈 | `./gradlew build -x test` |
| `load-test/` | k6 스크립트·결과. 매핑은 `load-test/README.md` | — |
| `docs/` | `portfolio/01~04`, `known-issues.md`, `deployment.md` | — |

각 앱은 독립 빌드이고 루트 Gradle은 없다. 원본 저장소들(SpringServer, global-cache-poc 등)은 subtree/merge로 히스토리째 들어와 있다.

## 이슈 처리 방식

- **기준 목록:** `docs/known-issues.md`. ID(C1, S3, …)로 부른다.
- **우선순위:** C(정합성) → S(안정성) → T(테스트) → O(운영/보안) → P(PoC). 정합성 버그는 같은 코드 경로를 공유하므로 C1 → C7 → C2/C3 → C4/C5 → C6 → C8 순서를 권장한다.
- **고치는 순서:**
  1. 버그를 재현하는 테스트를 먼저 작성한다(실패 확인).
  2. 수정한다.
  3. 테스트가 통과하는지 확인한다.
- **기록:** 고친 항목은 `known-issues.md`에서 지우지 말고 "해결" 표시와 커밋을 남긴다. 포트폴리오 문서의 해당 "재측정 예정" 메모도 함께 갱신한다.
- **Git:** main에서 브랜치를 따서 이슈 단위로 커밋하고 PR을 만든다. 머지는 사용자가 한다.

## 주의할 점

- **api-server 테스트:** 기존에 113/321개가 실패한다(T1). 새로 만드는 테스트는 `@ActiveProfiles("test")`(H2)로 독립 실행되게 하고, `./gradlew test --tests <클래스>`로 대상만 돌린다. 기본 프로필 `bottleNeckTest`는 실제 MariaDB가 필요하다.
- **`AllCampaignTypeData.add()`:** `this`를 수정하고 반환한다. `sum()`은 새 객체를 만들지만 얕은 복사다. `CampaignAnalysisDto.add(double, double)`은 새 객체를 반환한다(불변처럼 동작).
- **캐시:** 트리 키는 `(start << 9) | end`이고 날짜는 연초부터의 일수(1~366)다. 캐시는 Caffeine `email → year → node map` 구조다.
- **Windows 환경:** 긴 경로 때문에 worktree를 만들 때는 `-c core.longpaths=true`가 필요하다. `javac`에는 `-encoding UTF-8`을 준다.
- **커밋 메시지:** 기존 스타일(`feat :`, `fix(scope):`, 한국어 본문)을 따른다.
