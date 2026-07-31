#!/usr/bin/env bash
# 冒烟测试：核心接口可用性
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8081/api/v1}"
echo "==> Smoke against $BASE_URL"

curl -fsS "$BASE_URL/canary/probe" | grep -q '"status":"UP\|"status": "UP"' || {
  echo "canary probe failed"
  exit 1
}
echo "[ok] canary/probe"

curl -fsS "$BASE_URL/home/dashboard" | grep -q '"code":200\|"code": 200' || {
  echo "home dashboard failed"
  exit 1
}
echo "[ok] home/dashboard"

LOGIN=$(curl -fsS -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}')
echo "$LOGIN" | grep -q 'token' || {
  echo "login failed"
  exit 1
}
echo "[ok] auth/login"

echo "Smoke test passed"
