"""
① 엑셀 업로드 재측정 결과(<구성>-r1, -r2, …)를 구성별 중앙값과 범위로 묶어 출력한다.
본 측정 구간(phase:main, 50명 30초)만 집계한다.
사용: python aggregate.py [결과 폴더]
"""
import json
import pathlib
import re
import statistics
import sys
from collections import defaultdict

ROOT = pathlib.Path(sys.argv[1]) if len(sys.argv) > 1 else \
    pathlib.Path(__file__).resolve().parent.parent / "results" / "2026-10-rerun"
VUS = 50

groups = defaultdict(list)
for run in ROOT.iterdir():
    m = re.fullmatch(r"(.+)-r\d+", run.name)
    if m and (run / "k6-summary.json").exists():
        groups[m.group(1)].append(run)


def delta(series):
    return series[-1][1] - series[0][1] if len(series) > 1 else 0.0


def peak(series):
    return max((v for _, v in series), default=0.0)


def ms(v):
    return f"{v / 1000:.2f}s" if v >= 1000 else f"{v:.0f}ms"


def fmt(values, f):
    return f"{f(statistics.median(values))} ({f(min(values))}~{f(max(values))})"


print("| 구성 | 회수 | P95 | 평균 | 처리량(추정) | 실패율 | 요청당 할당 | GC 횟수 | GC 정지 합 | Acquire 최대 | Usage 최대 | 시스템 CPU 최대 |")
print("|---|---|---|---|---|---|---|---|---|---|---|---|")
for name in sorted(groups):
    cols = defaultdict(list)
    for run in groups[name]:
        metrics = json.load(open(run / "k6-summary.json", encoding="utf-8"))["metrics"]
        prom = json.load(open(run / "prometheus.json", encoding="utf-8"))["series"]
        duration = metrics["http_req_duration{phase:main}"]
        total = metrics["http_reqs"]["count"]
        failed = metrics["http_req_failed{phase:main}"]
        cols["p95"].append(duration["p(95)"])
        cols["avg"].append(duration["avg"])
        # 요약 파일에는 본 측정 구간만의 요청 수가 없어, VU 50개가 "응답 + 1초 대기"를 반복하는 것으로 처리량을 추정한다
        cols["rps"].append(VUS / (duration["avg"] / 1000 + 1))
        cols["fail"].append(failed.get("value", failed.get("rate", 0)) * 100)
        cols["alloc"].append(delta(prom["allocated_bytes_total"]) / 1e6 / total)
        cols["gc_count"].append(delta(prom["gc_pause_count_total"]))
        cols["gc_sec"].append(delta(prom["gc_pause_seconds_total"]) * 1000)
        cols["acquire"].append(peak(prom["acquire_max"]) * 1000)
        cols["usage"].append(peak(prom["usage_max"]) * 1000)
        cols["cpu"].append(peak(prom["system_cpu"]) * 100)
    print(f"| {name} | {len(groups[name])} | {fmt(cols['p95'], ms)} | {fmt(cols['avg'], ms)} | "
          f"{fmt(cols['rps'], lambda v: f'{v:.1f}/s')} | {fmt(cols['fail'], lambda v: f'{v:.1f}%')} | "
          f"{fmt(cols['alloc'], lambda v: f'{v:.1f}MB')} | {fmt(cols['gc_count'], lambda v: f'{v:.0f}')} | "
          f"{fmt(cols['gc_sec'], ms)} | {fmt(cols['acquire'], ms)} | {fmt(cols['usage'], ms)} | "
          f"{fmt(cols['cpu'], lambda v: f'{v:.0f}%')} |")
