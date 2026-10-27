#!/usr/bin/env bash
# ③ TIMED_WAITING 재검증 실행기 (2026-10-27).
# 서버를 새로 띄워 스캐너 버스트(k6)를 보낸 뒤 유휴 상태로 90초 두고,
# 부하 직후와 유휴 75초 후 스레드 덤프를 뜬다. 전체 구간의 스레드 상태 지표를 Prometheus 에서 받아 저장한다.
#
# 사용: run.sh <결과이름> [스프링 추가 인자...]
#   예) run.sh A-r1
#       run.sh B-r1 --server.tomcat.threads.max=50
#       run.sh C-r1 --logging.level.root=WARN
# 환경변수: APP_JAR, DB_PORT(기본 3306), DB_PASSWORD(기본 change-me), PROM_URL(기본 http://localhost:9090)
set -euo pipefail

NAME=$1; shift
ROOT=$(cd "$(dirname "$0")/../../.." && pwd)
OUT="$ROOT/load-test/04-timed-waiting/results/2026-10-rerun/$NAME"
APP_JAR=${APP_JAR:-$(ls "$ROOT"/apps/api-server/build/libs/*-SNAPSHOT.jar | grep -v plain | head -1)}
PROM_URL=${PROM_URL:-http://localhost:9090}
IDLE_SECONDS=90
mkdir -p "$OUT"

export DATASOURCE_URL="jdbc:mariadb://localhost:${DB_PORT:-3306}/grouup"
export DATASOURCE_USERNAME=root DATASOURCE_PASSWORD=${DB_PASSWORD:-change-me}
# bottleNeckTest 프로필이 요구하는 OAuth·JWT 값. 측정에는 쓰이지 않는다.
export KAKAO_CLIENT_ID=x KAKAO_CLIENT_SECRET=x KAKAO_URL=http://localhost
export GOOGLE_CLIENT_ID=x GOOGLE_CLIENT_SECRET=x GOOGLE_URL=http://localhost
export JWT_EXPIRATION=30 JWT_SECRET_KEY=dlrjtdlxptmxmtlzmfltzldlqslekwhsskrlfrpTjdigodydlrp
export RE_DIRECT_URL_KAKAO=http://localhost RE_DIRECT_URL_GOOGLE=http://localhost

if curl -s -o /dev/null localhost:8080/actuator/prometheus; then
  echo "8080 에 이미 서버가 떠 있습니다. 먼저 종료하세요." >&2; exit 1
fi

java -jar "$APP_JAR" --spring.profiles.active=bottleNeckTest "$@" > "$OUT/app.log" 2>&1 &
APP_PID=$!
trap 'kill $APP_PID 2>/dev/null || true; wait $APP_PID 2>/dev/null || true' EXIT
for _ in $(seq 1 60); do
  grep -q "Started SpringServerApplication" "$OUT/app.log" && break
  if ! kill -0 $APP_PID 2>/dev/null; then echo "서버 기동 실패: $OUT/app.log" >&2; exit 1; fi
  sleep 2
done
# jcmd 는 OS(Windows) 프로세스 번호가 필요하다. 셸의 $! 와 다를 수 있어 jar 경로로 찾는다.
JVM_PID=$(jcmd -l | grep "$(basename "$APP_JAR")" | awk '{print $1}' | head -1)
sleep 15  # Prometheus 가 기동 직후(부하 전) 값을 몇 번 수집하도록 기다린다

START=$(date +%s)
k6 run --summary-export "$OUT/k6-summary.json" "$ROOT/load-test/04-timed-waiting/scripts/scanner-burst.js" > "$OUT/k6.log" 2>&1 || true
BURST_END=$(date +%s)
jcmd "$JVM_PID" Thread.print > "$OUT/thread-dump-after-burst.txt"
sleep 75
jcmd "$JVM_PID" Thread.print > "$OUT/thread-dump-idle-75s.txt"
sleep $((IDLE_SECONDS - 75))
END=$(date +%s)
echo "{\"start\": $START, \"burst_end\": $BURST_END, \"end\": $END, \"args\": \"$*\"}" > "$OUT/meta.json"

python - "$PROM_URL" "$((START - 15))" "$END" "$OUT/prometheus.json" <<'EOF'
import json, sys, urllib.parse, urllib.request
prom, start, end, out = sys.argv[1], sys.argv[2], sys.argv[3], sys.argv[4]
queries = {
    "timed_waiting": 'jvm_threads_states_threads{state="timed-waiting"}',
    "waiting": 'jvm_threads_states_threads{state="waiting"}',
    "runnable": 'jvm_threads_states_threads{state="runnable"}',
    "live": "jvm_threads_live_threads",
    "http_max": "max(http_server_requests_seconds_max)",
    "process_cpu": "process_cpu_usage",
    "system_cpu": "system_cpu_usage",
}
result = {"series": {}}
for name, q in queries.items():
    url = f"{prom}/api/v1/query_range?" + urllib.parse.urlencode({"query": q, "start": start, "end": end, "step": "5"})
    data = json.load(urllib.request.urlopen(url))["data"]["result"]
    result["series"][name] = [[int(t), float(v)] for t, v in data[0]["values"] if v not in ("NaN", "+Inf")] if data else []
json.dump(result, open(out, "w"), indent=1)
EOF
echo "$NAME 완료: $OUT"
