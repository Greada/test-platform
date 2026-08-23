#!/bin/bash
# pr-test-review.sh — 将 Gitee PR 审核项「测试」标记为当前 token 用户通过。

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
  echo "[pr-test-review] ERROR: .env.ci not found" >&2
  exit 1
fi

set -a
# shellcheck disable=SC1090
source "$ENV_FILE"
set +a

: "${GITEE_OWNER:?GITEE_OWNER is required}"
: "${GITEE_REPO:?GITEE_REPO is required}"
: "${GITEE_TOKEN:?GITEE_TOKEN is required}"
: "${PR_NUMBER:?PR_NUMBER is required}"

if ! [[ "$PR_NUMBER" =~ ^[1-9][0-9]*$ ]]; then
  echo "[pr-test-review] ERROR: PR_NUMBER must be a positive integer" >&2
  exit 1
fi

GITEE_API="${GITEE_API:-https://gitee.com/api/v5}"
CURL_BIN="${CURL_BIN:-curl}"
TMP_DIR="$(mktemp -d)"
RESPONSE_FILE="${TMP_DIR}/response.json"
trap 'rm -rf "$TMP_DIR"' EXIT

TEST_URL="${GITEE_API}/repos/${GITEE_OWNER}/${GITEE_REPO}/pulls/${PR_NUMBER}/test?access_token=${GITEE_TOKEN}"

echo "[pr-test-review] PR #${PR_NUMBER} 测试审核项 → 通过"

fail() {
  echo "[pr-test-review] WARNING: test review write failed (${1-}); build result is unchanged" >&2
  if [ "${PR_TEST_REVIEW_STRICT:-0}" = "1" ]; then
    exit 1
  fi
  exit 0
}

if ! HTTP_CODE="$("$CURL_BIN" -sS -o "$RESPONSE_FILE" -w '%{http_code}' -X POST "$TEST_URL")"; then
  fail "cannot call test review API"
fi

if [ "$HTTP_CODE" = "200" ] || [ "$HTTP_CODE" = "201" ] || [ "$HTTP_CODE" = "204" ]; then
  echo "[pr-test-review] Test review write succeeded (HTTP ${HTTP_CODE})"
  exit 0
fi

fail "write HTTP ${HTTP_CODE}"
