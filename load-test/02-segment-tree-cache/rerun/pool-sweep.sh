#!/usr/bin/env bash
# Phase 4 풀 크기 튜닝 (2026-10-28).
# HikariCP maximum-pool-size 와 ioExecutor 크기를 같은 값 N 으로 올려 가며 09 시나리오를 반복한다.
# 회차 순서는 N 을 돌아가며(r1: 10→50, r2: 10→50, …) 섞어 시간대에 따른 편차가 한 구성에 몰리지 않게 한다.
# Acquire·Usage P95 를 보려고 HikariCP 타이머의 히스토그램을 켠다.
#
# 사용: DB_PORT=3307 pool-sweep.sh [반복 횟수(기본 3)] [N...(기본 10 20 30 40 50)]
#   결과: results/2026-10-rerun/phase4-cp<N>-io<N>-r<회차>, 요약: python aggregate.py
set -euo pipefail

DIR=$(cd "$(dirname "$0")" && pwd)
REPEAT=${1:-3}; shift || true
SIZES=${*:-10 20 30 40 50}

for r in $(seq 1 "$REPEAT"); do
  for n in $SIZES; do
    "$DIR/run.sh" "phase4-cp$n-io$n-r$r" 09-drilldown-lazycache.js \
      --spring.datasource.hikari.maximum-pool-size="$n" \
      --thread-pool.io.size="$n" \
      --management.metrics.distribution.percentiles-histogram.hikaricp.connections=true
  done
done
