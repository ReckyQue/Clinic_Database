#!/usr/bin/env bash
# 灰度后巡检：连续调用 canary/probe，错误率超阈值则失败
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8081/api/v1}"
ROUNDS="${ROUNDS:-20}"
THRESHOLD="${THRESHOLD:-0.01}"
fail=0

for i in $(seq 1 "$ROUNDS"); do
  if ! curl -fsS "$BASE_URL/canary/probe" >/dev/null; then
    fail=$((fail + 1))
  fi
  sleep 0.2
done

rate=$(awk "BEGIN { printf \"%.4f\", $fail / $ROUNDS }")
echo "error_rate=$rate (fail=$fail/$ROUNDS) threshold=$THRESHOLD"
awk -v r="$rate" -v t="$THRESHOLD" 'BEGIN { exit (r > t) ? 1 : 0 }'
echo "Canary check passed"
