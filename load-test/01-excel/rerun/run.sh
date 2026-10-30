#!/usr/bin/env bash
# ① 엑셀 업로드 재측정 실행기 (2026-10-27).
# 회차마다 margin_for_campaign 을 비우고 api-server 를 새로 띄운 뒤 k6 를 돌리고,
# 측정 구간의 HikariCP·GC·메모리 할당 지표를 결과 폴더에 저장한다.
#
# 사용: run.sh <결과이름> [스프링 추가 인자...]
# 환경변수:
#   APP_JAR   실행할 jar (기본: apps/api-server/build/libs/ 의 bootJar)
#   DB_PORT   MariaDB 호스트 포트 (기본 3306), DB_PASSWORD (기본 change-me)
#   DB_URL_PARAMS  JDBC URL 뒤에 붙일 옵션 (예: ?useBulkStmts=false)
#   PROM_URL  Prometheus 주소 (기본 http://localhost:9090)
#   RESULT_SET  결과 상위 폴더 (기본 2026-10-28-100rows, 500행 측정은 2026-10-rerun)
set -euo pipefail

NAME=$1; shift
ROOT=$(cd "$(dirname "$0")/../../.." && pwd)
OUT="$ROOT/load-test/01-excel/results/${RESULT_SET:-2026-10-28-100rows}/$NAME"
APP_JAR=${APP_JAR:-$(ls "$ROOT"/apps/api-server/build/libs/*-SNAPSHOT.jar | grep -v plain | head -1)}
PROM_URL=${PROM_URL:-http://localhost:9090}
DB="docker exec -i grouup-mariadb mariadb -uroot -p${DB_PASSWORD:-change-me} grouup"
mkdir -p "$OUT"

export DATASOURCE_URL="jdbc:mariadb://localhost:${DB_PORT:-3306}/grouup${DB_URL_PARAMS:-}"
export DATASOURCE_USERNAME=root DATASOURCE_PASSWORD=${DB_PASSWORD:-change-me}
# bottleNeckTest 프로필이 요구하는 OAuth·JWT 값. 측정에는 쓰이지 않는다.
export KAKAO_CLIENT_ID=x KAKAO_CLIENT_SECRET=x KAKAO_URL=http://localhost
export GOOGLE_CLIENT_ID=x GOOGLE_CLIENT_SECRET=x GOOGLE_URL=http://localhost
export JWT_EXPIRATION=30 JWT_SECRET_KEY=dlrjtdlxptmxmtlzmfltzldlqslekwhsskrlfrpTjdigodydlrp
export RE_DIRECT_URL_KAKAO=http://localhost RE_DIRECT_URL_GOOGLE=http://localhost

if curl -s -o /dev/null localhost:8080/actuator/prometheus; then
  echo "8080 에 이미 서버가 떠 있습니다. 먼저 종료하세요." >&2; exit 1
fi

# 매 회차 같은 시작 상태: 옵션 0건 (첫 업로드는 INSERT, 이후 같은 파일은 UPDATE)
$DB -e "DELETE FROM margin_for_campaign"

java -jar "$APP_JAR" --spring.profiles.active=bottleNeckTest "$@" > "$OUT/app.log" 2>&1 &
APP_PID=$!
trap 'kill $APP_PID 2>/dev/null || true; wait $APP_PID 2>/dev/null || true' EXIT
for _ in $(seq 1 60); do
  grep -q "Started SpringServerApplication" "$OUT/app.log" && break
  if ! kill -0 $APP_PID 2>/dev/null; then echo "서버 기동 실패: $OUT/app.log" >&2; exit 1; fi
  sleep 2
done
sleep 10  # Prometheus 가 기동 직후 값을 한 번 수집하도록 기다린다

START=$(date +%s)
k6 run --summary-export "$OUT/k6-summary.json" "$ROOT/load-test/01-excel/scripts/excel-upload.js" > "$OUT/k6.log" 2>&1 || true
sleep 10  # 마지막 구간 지표 수집
END=$(date +%s)
$DB -e "SELECT COUNT(*) AS mfc_rows FROM margin_for_campaign" > "$OUT/mfc-rows.txt"

python - "$PROM_URL" "$START" "$END" "$OUT/prometheus.json" <<'EOF'
import json, sys, urllib.parse, urllib.request
prom, start, end, out = sys.argv[1], sys.argv[2], sys.argv[3], sys.argv[4]
queries = {
    "active": "hikaricp_connections_active",
    "pending": "hikaricp_connections_pending",
    "acquire_max": "hikaricp_connections_acquire_seconds_max",
    "usage_max": "hikaricp_connections_usage_seconds_max",
    "allocated_bytes_total": "sum(jvm_gc_memory_allocated_bytes_total)",
    "gc_pause_count_total": "sum(jvm_gc_pause_seconds_count)",
    "gc_pause_seconds_total": "sum(jvm_gc_pause_seconds_sum)",
    "process_cpu": "process_cpu_usage",
    "system_cpu": "system_cpu_usage",
}
result = {"start": int(start), "end": int(end), "series": {}}
for name, q in queries.items():
    url = f"{prom}/api/v1/query_range?" + urllib.parse.urlencode({"query": q, "start": start, "end": end, "step": "5"})
    data = json.load(urllib.request.urlopen(url))["data"]["result"]
    result["series"][name] = [[int(t), float(v)] for t, v in data[0]["values"] if v not in ("NaN", "+Inf")] if data else []
json.dump(result, open(out, "w"), indent=1)
EOF
echo "$NAME 완료: $OUT"
