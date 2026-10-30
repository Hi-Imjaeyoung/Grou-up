"""
① 엑셀 업로드의 단계별 소요 시간을 Spring StopWatch 로 잰다(2026-10-30).
당시 기록(500행 한 건: 삽입 280ms → 10.5ms)과 같은 5단계로 나눈다.

  1. MultipartFile → Sheet      2. ExcelUploadContext 생성(내 캠페인·옵션 조회)
  3. 파싱                       4. 엑셀에 없는 옵션 삭제      5. 삽입·수정

측정용 jar 에만 StopWatch 를 넣고 소스는 원래대로 되돌린다(바꾼 내용은 patches/stopwatch.patch).
5단계 끝에 flush() 를 넣어, 개선 전(JPA) 구성의 변경 감지 UPDATE 도 커밋이 아니라 이 구간에서 실행되게 한다.

  python stage-timing.py build            → build/excel-jars/v0-sw.jar, v2-sw.jar
  python stage-timing.py summary <폴더>   → 결과 폴더의 stage-timing.txt 를 단계별 중앙값으로 묶어 출력

시나리오는 scripts/excel-upload-stage.js 다. 목적이 API 한 건 안의 단계별 비중이라 동시성은 빼고 1명(VUS=1)으로 잰다.
동시 요청이 많으면 개선 전 구성이 커넥션을 측정 구간 밖(트랜잭션 시작)에서 기다려 단계 비교가 되지 않는다.
결과 이름은 <구성>-u<동시 사용자 수>-r<회차> 다(예: v0-u1-r1).
"""
import importlib.util
import pathlib
import re
import shutil
import statistics
import subprocess
import sys
from collections import defaultdict
from datetime import datetime

HERE = pathlib.Path(__file__).resolve().parent
spec = importlib.util.spec_from_file_location("variants", HERE / "build-variant-jars.py")
variants = importlib.util.module_from_spec(spec)
spec.loader.exec_module(variants)

PARSE_IMPORT = (variants.PARSING, "import org.springframework.stereotype.Service;\n",
                "import org.springframework.stereotype.Service;\nimport org.springframework.util.StopWatch;\n")
PARSE_SW = (variants.PARSING, """        try (Workbook workbook = openWorkbook(file)) {
            Sheet sheet = workbook.getSheetAt(0);
            ExcelUploadContext excelUploadContext = new ExcelUploadContext(email,loadMyCampaignCache(email),loadMyMFCCache(email));
            readAllExcelRows(sheet, excelUploadContext);
            return excelUploadContext;""", """        StopWatch stopWatch = new StopWatch("Excel Upload Process");
        stopWatch.start("1");
        try (Workbook workbook = openWorkbook(file)) {
            Sheet sheet = workbook.getSheetAt(0);
            stopWatch.stop();
            stopWatch.start("2");
            ExcelUploadContext excelUploadContext = new ExcelUploadContext(email,loadMyCampaignCache(email),loadMyMFCCache(email));
            stopWatch.stop();
            stopWatch.start("3");
            readAllExcelRows(sheet, excelUploadContext);
            stopWatch.stop();
            log.info("EXCEL_STAGE parse email={} {}", email, java.util.Arrays.stream(stopWatch.getTaskInfo())
                    .map(t -> t.getTaskName() + "=" + t.getTimeNanos()).collect(Collectors.joining(" ")));
            return excelUploadContext;""")
WRITE_START = (variants.SERVICE, "        deleteOldMfcNotExistExcel(excelUploadContext);\n",
               """        StopWatch stopWatch = new StopWatch("Excel Upload Process");
        stopWatch.start("4");
        deleteOldMfcNotExistExcel(excelUploadContext);
        stopWatch.stop();
        stopWatch.start("5");
""")
WRITE_END = (variants.SERVICE, "        return excelUploadContext.getResult();\n",
             """        marginForCampaignRepository.flush();
        stopWatch.stop();
        log.info("EXCEL_STAGE write email={} {}", excelUploadContext.getEmail(), Arrays.stream(stopWatch.getTaskInfo())
                .map(t -> t.getTaskName() + "=" + t.getTimeNanos()).collect(java.util.stream.Collectors.joining(" ")));
        return excelUploadContext.getResult();
""")
STOPWATCH = [PARSE_IMPORT, PARSE_SW, WRITE_START, WRITE_END]
BUILDS = {"v0-sw": variants.VARIANTS["v0"] + STOPWATCH, "v2-sw": STOPWATCH}
STAGES = ["1", "2", "3", "4", "5"]
STAGE_NAMES = {"1": "① MultipartFile → Sheet", "2": "② Context 생성", "3": "③ 파싱",
               "4": "④ 데이터 제거", "5": "⑤ 삽입·수정"}


def build():
    files = {variants.PARSING, variants.SERVICE, variants.CONTROLLER}
    originals = {f: f.read_text(encoding="utf-8") for f in files}
    for edits in BUILDS.values():
        texts = dict(originals)
        for f, old, new in edits:
            assert texts[f].count(old) == 1, f"코드가 바뀌어 패치를 적용할 수 없습니다: {f.name}: {old[:40]}"
            texts[f] = texts[f].replace(old, new)
    variants.OUT.mkdir(parents=True, exist_ok=True)
    gradlew = str(variants.API / ("gradlew.bat" if shutil.which("cmd") else "gradlew"))
    try:
        for name, edits in BUILDS.items():
            texts = dict(originals)
            for f, old, new in edits:
                texts[f] = texts[f].replace(old, new)
            for f, text in texts.items():
                f.write_text(text, encoding="utf-8", newline="")
            if name == "v2-sw":
                diff = subprocess.run(["git", "diff", "--", *map(str, files)], cwd=variants.ROOT,
                                      capture_output=True, text=True, encoding="utf-8").stdout
                (variants.PATCHES / "stopwatch.patch").write_text(diff, encoding="utf-8", newline="")
            subprocess.run([gradlew, "bootJar", "-q"], cwd=variants.API, check=True)
            jar = next(p for p in (variants.API / "build" / "libs").glob("*-SNAPSHOT.jar") if "plain" not in p.name)
            shutil.copy(jar, variants.OUT / f"{name}.jar")
            print(f"{name}: {variants.OUT / (name + '.jar')}")
    finally:
        for f, text in originals.items():
            f.write_text(text, encoding="utf-8", newline="")
        subprocess.run([gradlew, "bootJar", "-q"], cwd=variants.API, check=True)


LINE = re.compile(r"^(\S+ \S+) .*EXCEL_STAGE (parse|write) email=(\S+) (.*)$")


def requests(path):
    """stage-timing.txt 에서 요청 단위로 (시각, email, {단계: ms}) 를 만든다. 한 사용자의 요청은 순서대로 처리된다."""
    pending, out = {}, []
    for line in path.read_text(encoding="utf-8").splitlines():
        m = LINE.match(line)
        if not m:
            continue
        when, kind, email, rest = m.groups()
        stages = {k: int(v) / 1e6 for k, v in (p.split("=") for p in rest.split())}
        if kind == "parse":
            pending[email] = (datetime.strptime(when, "%Y-%m-%d %H:%M:%S.%f"), stages)
        elif email in pending:
            start, parsed = pending.pop(email)
            out.append((start, email, {**parsed, **stages}))
    return out


def table(title, reqs):
    print(f"\n{title} (요청 {len(reqs)}건, 단계별 중앙값)")
    if not reqs:
        return
    print("| 단계 | 중앙값 | 비중 |")
    print("|---|---|---|")
    medians = {s: statistics.median(r[2].get(s, 0.0) for r in reqs) for s in STAGES}
    total = sum(medians.values())
    for s in STAGES:
        print(f"| {STAGE_NAMES[s]} | {medians[s]:.1f}ms | {medians[s] / total * 100:.0f}% |")
    totals = [sum(r[2].get(s, 0.0) for s in STAGES) for r in reqs]
    print(f"| 합계 (요청별 합의 중앙값) | {statistics.median(totals):.1f}ms | |")


MEASURED = {f"user{n:02d}@test.com" for n in range(1, 11)}  # excel-upload-stage.js 본 측정 대상(예열은 user41~45)


def summary(root):
    # 결과 이름: <구성>-u<동시 사용자 수>-r<회차> (예: v0-u1-r1)
    runs = defaultdict(list)
    for run in sorted(root.iterdir()):
        if (run / "stage-timing.txt").exists():
            runs[re.sub(r"-r\d+$", "", run.name)].append(run)
    for name, dirs in sorted(runs.items()):
        inserts, updates = [], []
        for d in dirs:
            per_user = defaultdict(list)
            for r in requests(d / "stage-timing.txt"):
                if r[1] in MEASURED:
                    per_user[r[1]].append(r)
            counts = sorted({len(v) for v in per_user.values()})
            print(f"{d.name}: 측정 사용자 {len(per_user)}명, 사용자별 업로드 {counts}회")
            for reqs in per_user.values():
                inserts += reqs[:1]  # 사용자별 첫 업로드는 INSERT, 이후 B·A 업로드는 값이 바뀐 UPDATE
                updates += reqs[1:]
        table(f"## {name}: 첫 업로드(INSERT 100행)", inserts)
        table(f"## {name}: 재업로드(UPDATE 100행, 값 변경)", updates)


if __name__ == "__main__":
    if sys.argv[1:2] == ["build"]:
        build()
    elif sys.argv[1:2] == ["summary"]:
        summary(pathlib.Path(sys.argv[2]) if len(sys.argv) > 2 else
                HERE.parent / "results" / "2026-10-30-stage-timing")
    else:
        print(__doc__)
