#!/bin/bash
# pr-report.sh — 向 Gitee Pull Request 写入 Jenkins Check Run。

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ENV_FILE="${ENV_FILE:-}"

if [ -z "$ENV_FILE" ]; then
  for candidate in /opt/.env.ci "${SCRIPT_DIR}/.env.ci" /var/jenkins_home/.env.ci; do
    if [ -f "$candidate" ]; then
      ENV_FILE="$candidate"
      break
    fi
  done
fi

if [ -z "$ENV_FILE" ] || [ ! -f "$ENV_FILE" ]; then
  echo "[pr-report] ERROR: .env.ci not found" >&2
  exit 1
fi

set -a
# shellcheck disable=SC1090
source "$ENV_FILE"
set +a

: "${GITEE_OWNER:?GITEE_OWNER is required}"
: "${GITEE_REPO:?GITEE_REPO is required}"
: "${GITEE_TOKEN:?GITEE_TOKEN is required}"
: "${PR_SHA:?PR_SHA is required}"
: "${CI_STATUS:?CI_STATUS is required}"

case "$CI_STATUS" in
  pending|success|failure) ;;
  *)
    echo "[pr-report] ERROR: CI_STATUS must be pending, success, or failure" >&2
    exit 1
    ;;
esac

GITEE_API="${GITEE_API:-https://gitee.com/api/v5}"
JENKINS_JOB="${JENKINS_JOB:-test-platform-learn}"
CURL_BIN="${CURL_BIN:-curl}"
PR_NUMBER="${PR_NUMBER:-unknown}"
PR_ID="${PR_ID:-}"
BUILD_URL="${BUILD_URL:-${JENKINS_URL:-}/job/${JENKINS_JOB}/}"
CHECK_NAME="ci/jenkins"
TMP_DIR="$(mktemp -d)"
RESPONSE_FILE="${TMP_DIR}/response.json"
trap 'rm -rf "$TMP_DIR"' EXIT

case "$CI_STATUS" in
  pending) DEFAULT_DESC="CI 构建中…" ;;
  success) DEFAULT_DESC="CI 构建通过 ✓" ;;
  failure) DEFAULT_DESC="CI 构建失败 ✗" ;;
esac
CI_DESC="${CI_DESC:-$DEFAULT_DESC}"
NOW="$(date -u '+%Y-%m-%dT%H:%M:%SZ')"

echo "[pr-report] PR #${PR_NUMBER} SHA=${PR_SHA} → ${CI_STATUS}"

fail() {
  echo "[pr-report] WARNING: Check Run write failed (${1-}); build result is unchanged" >&2
  if [ "${PR_REPORT_STRICT:-0}" = "1" ]; then
    exit 1
  fi
  exit 0
}

CHECK_RUNS_URL="${GITEE_API}/repos/${GITEE_OWNER}/${GITEE_REPO}/commits/${PR_SHA}/check-runs?access_token=${GITEE_TOKEN}&check_name=ci%2Fjenkins"
if ! HTTP_CODE="$("$CURL_BIN" -sS -o "$RESPONSE_FILE" -w '%{http_code}' "$CHECK_RUNS_URL")"; then
  fail "cannot query existing check runs"
fi
if [ "$HTTP_CODE" != "200" ]; then
  fail "query HTTP ${HTTP_CODE}"
fi

CHECK_RUN_ID="$(sed -n 's/.*"id"[[:space:]]*:[[:space:]]*\([0-9][0-9]*\).*/\1/p' "$RESPONSE_FILE" | head -n 1)"

if [ "$CI_STATUS" = "pending" ]; then
  GITEE_STATUS="in_progress"

  if [ -n "$CHECK_RUN_ID" ]; then
    REQUEST_URL="${GITEE_API}/repos/${GITEE_OWNER}/${GITEE_REPO}/check-runs/${CHECK_RUN_ID}?access_token=${GITEE_TOKEN}"
    REQUEST_METHOD="PATCH"
    PAYLOAD="$(printf '{"name":"%s","details_url":"%s","status":"%s","started_at":"%s","output":{"title":"Jenkins CI","summary":"%s"}}' \
      "$CHECK_NAME" "$BUILD_URL" "$GITEE_STATUS" "$NOW" "$CI_DESC")"
  else
    if [ -z "$PR_ID" ]; then
      fail "PR_ID is required when creating a PR check run"
    fi
    REQUEST_URL="${GITEE_API}/repos/${GITEE_OWNER}/${GITEE_REPO}/check-runs?access_token=${GITEE_TOKEN}"
    REQUEST_METHOD="POST"
    PAYLOAD="$(printf '{"name":"%s","head_sha":"%s","pull_request_id":%s,"details_url":"%s","status":"%s","started_at":"%s","output":{"title":"Jenkins CI","summary":"%s"}}' \
      "$CHECK_NAME" "$PR_SHA" "$PR_ID" "$BUILD_URL" "$GITEE_STATUS" "$NOW" "$CI_DESC")"
  fi
else
  if [ -z "$CHECK_RUN_ID" ]; then
    fail "no existing check run for ${PR_SHA}"
  fi
  REQUEST_URL="${GITEE_API}/repos/${GITEE_OWNER}/${GITEE_REPO}/check-runs/${CHECK_RUN_ID}?access_token=${GITEE_TOKEN}"
  REQUEST_METHOD="PATCH"
  PAYLOAD="$(printf '{"name":"%s","details_url":"%s","status":"completed","conclusion":"%s","completed_at":"%s","output":{"title":"Jenkins CI","summary":"%s"}}' \
    "$CHECK_NAME" "$BUILD_URL" "$CI_STATUS" "$NOW" "$CI_DESC")"
fi

if ! HTTP_CODE="$("$CURL_BIN" -sS -o "$RESPONSE_FILE" -w '%{http_code}' -X "$REQUEST_METHOD" \
  "$REQUEST_URL" -H 'Content-Type: application/json' -d "$PAYLOAD")"; then
  fail "cannot write check run"
fi

if [ "$HTTP_CODE" = "200" ] || [ "$HTTP_CODE" = "201" ]; then
  echo "[pr-report] Check Run write succeeded (HTTP ${HTTP_CODE})"
  exit 0
fi

fail "write HTTP ${HTTP_CODE}"
