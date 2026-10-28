"""
run.sh 결과 폴더(results/2026-10-rerun/*)를 읽어 회차별 요약 표(Markdown)를 출력한다.
사용: python summarize.py [결과 폴더]
"""
import json
import pathlib
import sys

ROOT = pathlib.Path(sys.argv[1]) if len(sys.argv) > 1 else \
    pathlib.Path(__file__).resolve().parent.parent / "results" / "2026-10-rerun"


def ms(v):
    return "—" if v is None else (f"{v / 1000:.2f}s" if v >= 1000 else f"{v:.0f}ms")


def metric(summary, name):
    return summary["metrics"].get(name, {})


def peak(series):
    return max((v for _, v in series), default=None)


def last(series):
    return series[-1][1] if series else None


def prom_cols(prom):
    s = prom["series"]
    sec = lambda v: None if v is None else v * 1000
    return [
        f"{peak(s['pending']):.0f}" if s["pending"] else "—",
        ms(sec(peak(s["acquire_max"]))),
        ms(sec(peak(s["usage_max"]))),
        f"{ms(sec(peak(s['acquire_cumulative_avg'])))} → {ms(sec(last(s['acquire_cumulative_avg'])))}",
        f"{ms(sec(peak(s['usage_cumulative_avg'])))} → {ms(sec(last(s['usage_cumulative_avg'])))}",
    ]


print("| 회차 | 구간 | 요청 수 | P95 | 평균 | 최대 | Pending 최대 | Acquire 최대 | Usage 최대 | Acquire 누적평균(최고 → 끝) | Usage 누적평균(최고 → 끝) |")
print("|---|---|---|---|---|---|---|---|---|---|---|")
for run in sorted(p for p in ROOT.iterdir() if (p / "k6-summary.json").exists()):
    summary = json.load(open(run / "k6-summary.json", encoding="utf-8"))
    prom = json.load(open(run / "prometheus.json", encoding="utf-8"))
    rows = []
    steps = sorted(k for k in summary["metrics"] if k.startswith("http_req_duration{step:"))
    if steps:
        for k in steps:
            label = k[len("http_req_duration{step:"):-1]
            rows.append((label, metric(summary, k), metric(summary, "http_reqs{" + k.split("{")[1]).get("count")))
    else:
        rows.append(("측정", metric(summary, "http_req_duration{api_version:v2_dsl}"), None))
    total = metric(summary, "http_reqs").get("count")
    for i, (label, m, count) in enumerate(rows):
        cols = prom_cols(prom) if i == 0 else [""] * 5
        print(f"| {run.name if i == 0 else ''} | {label} | {count if count is not None else total} | "
              f"{ms(m.get('p(95)'))} | {ms(m.get('avg'))} | {ms(m.get('max'))} | " + " | ".join(cols) + " |")
