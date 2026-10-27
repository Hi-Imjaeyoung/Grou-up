"""
반복 측정(<구성>-r1, -r2, …) 결과를 구성별로 묶어 중앙값과 범위를 출력한다(09 드릴다운 시나리오용).
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
STEPS = ["1.Base_Half_Year", "2.Subset_Q3", "3.Subset_August", "4.Overlap_June_July"]


def peak(series):
    return max((v for _, v in series), default=0.0)


groups = defaultdict(list)
for run in ROOT.iterdir():
    m = re.fullmatch(r"(.+)-r\d+", run.name)
    if m and (run / "k6-summary.json").exists():
        groups[m.group(1)].append(run)


def fmt(values, unit_ms=True):
    med = statistics.median(values)
    f = (lambda v: f"{v / 1000:.2f}s" if v >= 1000 else f"{v:.0f}ms") if unit_ms else (lambda v: f"{v:.0f}")
    return f"{f(med)} ({f(min(values))}~{f(max(values))})"


print("| 구성 | 회수 | " + " | ".join(f"{s} P95" for s in STEPS) + " | Acquire 최대 | Usage 최대 | Pending 최대 |")
print("|---|---|" + "---|" * (len(STEPS) + 3))
for name in sorted(groups):
    runs = groups[name]
    cols = []
    for step in STEPS:
        vals = [json.load(open(r / "k6-summary.json", encoding="utf-8"))["metrics"]
                [f"http_req_duration{{step:{step}}}"]["p(95)"] for r in runs]
        cols.append(fmt(vals))
    proms = [json.load(open(r / "prometheus.json", encoding="utf-8"))["series"] for r in runs]
    cols.append(fmt([peak(p["acquire_max"]) * 1000 for p in proms]))
    cols.append(fmt([peak(p["usage_max"]) * 1000 for p in proms]))
    cols.append(fmt([peak(p["pending"]) for p in proms], unit_ms=False))
    print(f"| {name} | {len(runs)} | " + " | ".join(cols) + " |")
