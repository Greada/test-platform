#!/bin/bash
# pr-poller-learn.sh — 轮询 Gitee Open PR，仅由有效 start build 评论触发学习版 Jenkins Job。

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPORT_SCRIPT="${SCRIPT_DIR}/pr-report.sh"
ENV_FILE="${ENV_FILE:-/opt/.env.ci}"

if [ ! -f "$ENV_FILE" ]; then
  echo "[pr-poller] ERROR: env file not found: $ENV_FILE" >&2
  exit 1
fi

set -a
# shellcheck disable=SC1090
source "$ENV_FILE"
set +a

: "${GITEE_OWNER:?GITEE_OWNER is required}"
: "${GITEE_REPO:?GITEE_REPO is required}"
: "${GITEE_TOKEN:?GITEE_TOKEN is required}"
: "${JENKINS_URL:?JENKINS_URL is required}"
: "${JENKINS_USER:?JENKINS_USER is required}"
: "${JENKINS_TOKEN:?JENKINS_TOKEN is required}"

GITEE_API="${GITEE_API:-https://gitee.com/api/v5}"
JENKINS_JOB="${JENKINS_JOB:-test-platform-learn}"
JENKINS_INTERNAL_URL="${JENKINS_INTERNAL_URL:-$JENKINS_URL}"
STATE_FILE="${STATE_FILE:-/var/tmp/test-platform-pr-poller.state}"
LOG_FILE="${LOG_FILE:-/var/tmp/test-platform-pr-poller.log}"
CURL_BIN="${CURL_BIN:-curl}"

log() {
  echo "[$(date '+%Y-%m-%d %H:%M:%S')] $*" >> "$LOG_FILE"
}

touch "$STATE_FILE" "$LOG_FILE"
log "===== PR poller started ====="

if ! PR_LIST_JSON="$("$CURL_BIN" -sS \
  "${GITEE_API}/repos/${GITEE_OWNER}/${GITEE_REPO}/pulls?state=open&access_token=${GITEE_TOKEN}")"; then
  log "Gitee API request failed, retry next cycle"
  exit 0
fi

if ! PR_LINES="$(printf '%s' "$PR_LIST_JSON" | python3 -c '
import json
import sys

try:
    items = json.load(sys.stdin)
except json.JSONDecodeError:
    raise SystemExit(1)

for item in items:
    number = item.get("number")
    pr_id = item.get("id")
    sha = item.get("head", {}).get("sha", "")
    pr_author = item.get("user", {}).get("login", "")
    head_author = item.get("head", {}).get("user", {}).get("login", "")
    if number is not None and pr_id is not None and sha:
        print(f"{number} {pr_id} {sha} {pr_author} {head_author}")
')"; then
  log "Failed to parse Gitee PR response, retry next cycle"
  exit 0
fi

if [ -z "$PR_LINES" ]; then
  log "No open PR with a valid head SHA"
  exit 0
fi

while read -r PR_NUMBER PR_ID PR_SHA PR_AUTHOR HEAD_AUTHOR; do
  if ! COMMENTS_JSON="$("$CURL_BIN" -sS \
    "${GITEE_API}/repos/${GITEE_OWNER}/${GITEE_REPO}/pulls/${PR_NUMBER}/comments?access_token=${GITEE_TOKEN}")"; then
    log "PR #${PR_NUMBER} comments request failed, retry next cycle"
    continue
  fi

  if ! COMMENT_LINES="$(printf '%s' "$COMMENTS_JSON" | python3 -c '
import json
import sys

try:
    comments = json.load(sys.stdin)
except json.JSONDecodeError:
    raise SystemExit(1)

if not isinstance(comments, list):
    raise SystemExit(1)

for comment in comments:
    comment_id = comment.get("id")
    body = comment.get("body", "")
    login = comment.get("user", {}).get("login", "")
    if comment_id is not None and str(body).strip() == "start build":
        print(f"{comment_id} {login}")
')"; then
    log "PR #${PR_NUMBER} failed to parse comments response, retry next cycle"
    continue
  fi

  if [ -n "$COMMENT_LINES" ]; then
    while read -r COMMENT_ID COMMENT_AUTHOR; do
      KEY="manual:${PR_NUMBER}:${COMMENT_ID}"
      if grep -qxF "$KEY" "$STATE_FILE"; then
        log "PR #${PR_NUMBER} comment ${COMMENT_ID} already processed, skip"
        continue
      fi

      if [ "$COMMENT_AUTHOR" != "$PR_AUTHOR" ] && [ "$COMMENT_AUTHOR" != "$HEAD_AUTHOR" ]; then
        log "PR #${PR_NUMBER} comment ${COMMENT_ID} author is not allowed, skip"
        continue
      fi

      ATTEMPT_PREFIX="attempt:${PR_NUMBER}:${COMMENT_ID}:"
      SAME_SHA_ATTEMPT=0
      STALE_SHA_ATTEMPT=0
      while read -r STATE_LINE; do
        case "$STATE_LINE" in
          "$ATTEMPT_PREFIX"*)
            ATTEMPTED_SHA="${STATE_LINE#"$ATTEMPT_PREFIX"}"
            if [ "$ATTEMPTED_SHA" = "$PR_SHA" ]; then
              SAME_SHA_ATTEMPT=1
            else
              STALE_SHA_ATTEMPT=1
            fi
            ;;
        esac
      done < "$STATE_FILE"

      if [ "$STALE_SHA_ATTEMPT" -ne 0 ]; then
        log "PR #${PR_NUMBER} comment ${COMMENT_ID} was requested for another SHA, skip"
        continue
      fi

      if [ "$SAME_SHA_ATTEMPT" -eq 0 ]; then
        echo "${ATTEMPT_PREFIX}${PR_SHA}" >> "$STATE_FILE"
      fi

      log "PR #${PR_NUMBER} comment ${COMMENT_ID} accepted for ${PR_SHA}, writing pending status"
      if ! PENDING_OUTPUT="$(ENV_FILE="$ENV_FILE" PR_REPORT_STRICT=1 PR_NUMBER="$PR_NUMBER" PR_ID="$PR_ID" PR_SHA="$PR_SHA" \
        CI_STATUS=pending BUILD_URL="${JENKINS_URL}/job/${JENKINS_JOB}/" \
        bash "$REPORT_SCRIPT" 2>&1)"; then
        printf '%s\n' "$PENDING_OUTPUT" >> "$LOG_FILE"
        log "PR #${PR_NUMBER} pending write failed, retry next cycle"
        continue
      fi
      printf '%s\n' "$PENDING_OUTPUT" >> "$LOG_FILE"
      CHECK_RUN_ID="$(printf '%s\n' "$PENDING_OUTPUT" | sed -n 's/^\[pr-report\] CHECK_RUN_ID=//p' | tail -n 1)"
      if [ -z "$CHECK_RUN_ID" ]; then
        log "PR #${PR_NUMBER} pending write did not return check run ID, retry next cycle"
        continue
      fi

      HTTP_CODE="$("$CURL_BIN" -sS -o /dev/null -w '%{http_code}' \
        -u "${JENKINS_USER}:${JENKINS_TOKEN}" \
        -X POST "${JENKINS_INTERNAL_URL}/job/${JENKINS_JOB}/buildWithParameters" \
        --data-urlencode "PR_NUMBER=${PR_NUMBER}" \
        --data-urlencode "PR_SHA=${PR_SHA}" \
        --data-urlencode "CHECK_RUN_ID=${CHECK_RUN_ID}")"

      if [ "$HTTP_CODE" = "201" ]; then
        echo "$KEY" >> "$STATE_FILE"
        log "PR #${PR_NUMBER} comment ${COMMENT_ID} Jenkins build triggered (HTTP 201)"
      else
        log "PR #${PR_NUMBER} comment ${COMMENT_ID} Jenkins trigger failed (HTTP ${HTTP_CODE}), retry next cycle"
      fi
    done <<< "$COMMENT_LINES"
  fi
done <<< "$PR_LINES"

log "===== PR poller finished ====="
