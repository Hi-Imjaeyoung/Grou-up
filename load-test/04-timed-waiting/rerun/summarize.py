"""
③ TIMED_WAITING 재검증 결과를 요약한다.
  - 회차별: TIMED_WAITING 부하 전 / 최고 / 유휴 후 값, 최고값이 유지된 시간, 스레드 덤프의 Tomcat 워커 수와 TaskQueue.poll 대기 수
  - 구성별 시간대별 TIMED_WAITING 그래프(timed-waiting.png, matplotlib 필요)
사용: python summarize.py [결과 폴더]
"""
import json
import pathlib
import re
import sys
from collections import defaultdict

ROOT = pathlib.Path(sys.argv[1]) if len(sys.argv) > 1 else \
    pathlib.Path(__file__).resolve().parent.parent / "results" / "2026-10-rerun"
LABELS = {"A": "A. 기본 설정 (max-threads 200)", "B": "B. max-threads 50", "C": "C. 로그 WARN"}


def workers(dump_path):
    """스레드 덤프에서 Tomcat 워커(http-nio-*-exec-*) 수, TIMED_WAITING 수, TaskQueue.poll 에서 대기 중인 수."""
    blocks = re.split(r"\n(?=\")", dump_path.read_text(encoding="utf-8", errors="replace"))
    tomcat = [b for b in blocks if re.match(r'"http-nio-\d+-exec-\d+"', b)]
    timed = [b for b in tomcat if "TIMED_WAITING" in b]
    polling = [b for b in timed if "org.apache.tomcat.util.threads.TaskQueue.poll" in b]
    return len(tomcat), len(timed), len(polling)


def plateau_seconds(series, burst_end, peak):
    """부하가 끝난 뒤 TIMED_WAITING 이 최고값의 90% 이상으로 유지된 시간(5초 수집 간격 단위)."""
    after = [(t, v) for t, v in series if t >= burst_end]
    held = [t for t, v in after if v >= peak * 0.9]
    return (max(held) - burst_end) if held else 0


runs = sorted(p for p in ROOT.iterdir() if (p / "prometheus.json").exists())
print("| 회차 | 부하 전 | 최고 | 유휴 90초 후 | 부하 후 최고값 유지 | 덤프(부하 직후): 워커 / TIMED_WAITING / TaskQueue.poll | 덤프(유휴 75초): 워커 |")
print("|---|---|---|---|---|---|---|")
by_config = defaultdict(list)
for run in runs:
    meta = json.load(open(run / "meta.json"))
    tw = json.load(open(run / "prometheus.json"))["series"]["timed_waiting"]
    before = [v for t, v in tw if t < meta["start"]]
    peak = max(v for _, v in tw)
    after_burst = workers(run / "thread-dump-after-burst.txt")
    idle = workers(run / "thread-dump-idle-75s.txt")
    print(f"| {run.name} | {before[-1]:.0f} | {peak:.0f} | {tw[-1][1]:.0f} | 약 {plateau_seconds(tw, meta['burst_end'], peak)}초 | "
          f"{after_burst[0]} / {after_burst[1]} / {after_burst[2]} | {idle[0]} |")
    by_config[run.name.split("-")[0]].append((meta, tw))

try:
    import matplotlib
    matplotlib.use("Agg")
    import matplotlib.pyplot as plt
except ImportError:
    sys.exit("matplotlib 이 없어 그래프는 건너뜁니다")

from matplotlib import font_manager
installed = {f.name for f in font_manager.fontManager.ttflist}
# 한글 글꼴 중 설치된 것을 쓴다(없으면 기본 글꼴, 한글은 깨질 수 있음)
plt.rcParams["font.family"] = next((f for f in ("Malgun Gothic", "AppleGothic", "NanumGothic") if f in installed),
                                   "DejaVu Sans")
plt.rcParams["axes.unicode_minus"] = False  # 한글 글꼴에 유니코드 마이너스 기호가 없어 깨지는 것을 막는다
fig, ax = plt.subplots(figsize=(9, 4.5))
colors = {"A": "#2563eb", "B": "#dc2626", "C": "#16a34a"}
for config, items in sorted(by_config.items()):
    for i, (meta, tw) in enumerate(items):
        xs = [t - meta["start"] for t, _ in tw]
        ys = [v for _, v in tw]
        ax.plot(xs, ys, color=colors.get(config, "gray"), alpha=0.8, linewidth=1.5,
                label=LABELS.get(config, config) if i == 0 else None)
ax.axvspan(0, 21, color="#f59e0b", alpha=0.12, label="k6 부하 (예열 5초 + 300명 15초)")
ax.axhline(200, color="gray", linestyle=":", linewidth=1)
ax.set_xlabel("부하 시작 후 경과 시간 (초)")
ax.set_ylabel("TIMED_WAITING 스레드 수")
ax.set_title("스캐너 요청 버스트 후 TIMED_WAITING 스레드 (구성별 3회)")
ax.legend(loc="center right", fontsize=9)
ax.grid(alpha=0.3)
fig.tight_layout()
out = ROOT / "timed-waiting.png"
fig.savefig(out, dpi=120)
print(f"\n그래프: {out}")
