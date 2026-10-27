"""
Phase 0~3 구성을 재현하는 jar 를 만든다. 현재 코드(Phase 4)의 CacheBuildEventListener 만 바꿔 빌드하고,
바꾼 내용은 patches/<phase>.patch 로 남긴 뒤 파일을 원래대로 되돌린다.

  phase0  동기 빌드: 이벤트를 요청 스레드에서 바로 처리(DB 조회 → 트리 빌드 → 응답)
  phase2  단순 비동기 + Eager Loading: 이벤트 즉시 commonPool 에서 실행(AFTER_COMMIT·지연·풀 분리 없음)
  phase3  + AFTER_COMMIT·500ms 지연 (풀 분리 없음, commonPool)
  (phase4 는 현재 코드 그대로)

Phase 1(Lazy Fetch 단계의 비동기)은 Eager Loading 이전 트리 구조가 필요해 재현하지 않는다.
사용: python build-phase-jars.py   (Grou-up 루트 어디서 실행해도 된다)
"""
import pathlib
import shutil
import subprocess

ROOT = pathlib.Path(__file__).resolve().parents[3]
API = ROOT / "apps" / "api-server"
SRC = API / "src/main/java/growup/spring/springserver/global/listener/CacheBuildEventListener.java"
OUT = API / "build" / "phase-jars"
PATCHES = pathlib.Path(__file__).resolve().parent / "patches"

AFTER_COMMIT = "@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)"
DELAYED_IO = "CompletableFuture.delayedExecutor(500, TimeUnit.MILLISECONDS, ioExecutor));"
CPU = "}, cpuExecutor);"
IMPORT_ANCHOR = "import org.springframework.stereotype.Component;\n"


def with_event_listener(s):
    return s.replace(AFTER_COMMIT, "@EventListener").replace(
        IMPORT_ANCHOR, IMPORT_ANCHOR + "import org.springframework.context.event.EventListener;\n")


PHASES = {
    "phase0": lambda s: with_event_listener(s)
        .replace(DELAYED_IO, "(Executor) Runnable::run);")
        .replace(CPU, "}, (Executor) Runnable::run);"),
    "phase2": lambda s: with_event_listener(s)
        .replace(DELAYED_IO, "ForkJoinPool.commonPool());")
        .replace(CPU, "}, ForkJoinPool.commonPool());")
        .replace("import java.util.concurrent.Executor;\n",
                 "import java.util.concurrent.Executor;\nimport java.util.concurrent.ForkJoinPool;\n"),
    "phase3": lambda s: s
        .replace(DELAYED_IO, "CompletableFuture.delayedExecutor(500, TimeUnit.MILLISECONDS));")
        .replace(CPU, "});"),
}


def main():
    original = SRC.read_text(encoding="utf-8")
    for needle in (AFTER_COMMIT, DELAYED_IO, CPU, IMPORT_ANCHOR):
        assert needle in original, f"리스너 코드가 바뀌어 패치를 적용할 수 없습니다: {needle}"
    OUT.mkdir(parents=True, exist_ok=True)
    PATCHES.mkdir(exist_ok=True)
    gradlew = str(API / ("gradlew.bat" if shutil.which("cmd") else "gradlew"))
    try:
        for phase, transform in PHASES.items():
            SRC.write_text(transform(original), encoding="utf-8", newline="")
            diff = subprocess.run(["git", "diff", "--", str(SRC)], cwd=ROOT, capture_output=True,
                                  text=True, encoding="utf-8").stdout
            (PATCHES / f"{phase}.patch").write_text(diff, encoding="utf-8", newline="")
            subprocess.run([gradlew, "bootJar", "-q"], cwd=API, check=True)
            jar = next(p for p in (API / "build" / "libs").glob("*-SNAPSHOT.jar") if "plain" not in p.name)
            shutil.copy(jar, OUT / f"{phase}.jar")
            print(f"{phase}: {OUT / (phase + '.jar')}")
    finally:
        SRC.write_text(original, encoding="utf-8", newline="")
        subprocess.run([gradlew, "bootJar", "-q"], cwd=API, check=True)  # 기본 jar 를 현재 코드로 되돌린다


if __name__ == "__main__":
    main()
