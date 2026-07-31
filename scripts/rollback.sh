#!/usr/bin/env bash
# Docker Compose 一键回滚到上一稳定镜像标签
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR/docker"

PREV_TAG="${PREV_TAG:-previous}"
echo "==> Rollback backend image tag to: $PREV_TAG"

# 约定：发布时同时推送 latest 与 previous；回滚将 clinic-backend 指回 previous
export BACKEND_IMAGE_TAG="$PREV_TAG"
docker compose pull backend || true
docker compose up -d --no-deps backend

echo "==> Waiting health..."
sleep 5
curl -fsS "${BASE_URL:-http://localhost:8081/api/v1}/canary/probe" >/dev/null
echo "Rollback completed"
