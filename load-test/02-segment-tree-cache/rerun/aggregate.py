"""
반복 측정(<구성>-r1, -r2, …) 결과를 구성별로 묶어 중앙값과 범위를 출력한다(09 드릴다운 시나리오용).
사용: python aggregate.py [결과 폴더] [구성 이름 접두어]
  예) python aggregate.py "" phase4-cp   # 풀 크기 튜닝(pool-sweep.sh) 결과만
풀 크기 튜닝 열(Acquire·Usage P95, IO 스레드)은 그 지표를 수집한 회차에만 값이 있고 나머지는 "—"다.
"""
import json
import pathlib
import re
import statistics
import sys
from collections import defaultdict

ROOT = pathlib.Path(sys.argv[1]) if len(sys.argv) > 1 and sys.argv[1] else \
    pathlib.Path(__file__).resolve().parent.parent / "results" / "2026-10-rerun"
PREFIX = sys.argv[2] if len(sys.argv) > 2 else ""
STEPS = ["1.Base_Half_Year", "2.Subset_Q3", "3.Subset_August", "4.Overlap_June_July"]
EXTRA = ["Acquire P95 최대", "Usage P95 최대", "IO 활성 최대", "IO 큐 최대"]


def peak(series):
    return max((v for _, v in series), default=0.0)


def optional_peak(series, name, scale=1):
    return peak(series[name]) * scale if series.get(name) else None


groups = defaultdict(list)
for run in ROOT.iterdir():
    m = re.fullmatch(r"(.+)-r\d+", run.name)
    if m and m.group(1).startswith(PREFIX) and (run / "k6-summary.json").exists():
        groups[m.group(1)].append(run)


def fmt(values, unit_ms=True):
    values = [v for v in values if v is not None]
    if not values:
        return "—"
    med = statistics.median(values)
    f = (lambda v: f"{v / 1000:.2f}s" if v >= 1000 else f"{v:.0f}ms") if unit_ms else (lambda v: f"{v:.0f}")
    return f"{f(med)} ({f(min(values))}~{f(max(values))})"


print("| 구성 | 회수 | " + " | ".join(f"{s} P95" for s in STEPS)
      + " | Acquire 최대 | Usage 최대 | Pending 최대 | " + " | ".join(EXTRA) + " |")
print("|---|---|" + "---|" * (len(STEPS) + 3 + len(EXTRA)))
for name in sorted(groups, key=lambda n: [int(t) if t.isdigit() else t for t in re.split(r"(\d+)", n)]):
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
    cols.append(fmt([optional_peak(p, "acquire_p95", 1000) for p in proms]))
    cols.append(fmt([optional_peak(p, "usage_p95", 1000) for p in proms]))
    cols.append(fmt([optional_peak(p, "io_active") for p in proms], unit_ms=False))
    cols.append(fmt([optional_peak(p, "io_queued") for p in proms], unit_ms=False))
    print(f"| {name} | {len(runs)} | " + " | ".join(cols) + " |")
