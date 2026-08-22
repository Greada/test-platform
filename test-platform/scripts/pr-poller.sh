#!/bin/bash
# =============================================================================
# pr-poller.sh — Gitee Pull Request 轮询 & 触发 Jenkins 构建
#
# 功能：
#   1. 调用 Gitee API 获取所有 open PR
#   2. 对每个 PR 的最新 commit，检查 ci/jenkins context 状态
#   3. 未构建过 → 触发 Jenkins pr-build Job + 设置 pending 状态
#
# 被 Jenkins 定时 Job 调用（每 2 分钟）
# 依赖：/opt/.env.ci（或脚本同目录 .env.ci）
# =============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# ---- 加载配置 ----------------------------------------------------------------
if [ -f /opt/.env.ci ]; then
    source /opt/.env.ci
elif [ -f "${SCRIPT_DIR}/.env.ci" ]; then
    source "${SCRIPT_DIR}/.env.ci"
elif [ -f /var/jenkins_home/.env.ci ]; then
    source /var/jenkins_home/.env.ci
else
    echo "[ERROR] .env.ci 未找到，请创建在 /opt/ 或脚本同目录"
    exit 1
fi

: "${GITEE_OWNER:?未设置 GITEE_OWNER}"
: "${GITEE_REPO:?未设置 GITEE_REPO}"
: "${GITEE_TOKEN:?未设置 GITEE_TOKEN}"
: "${JENKINS_URL:?未设置 JENKINS_URL}"
: "${JENKINS_USER:?未设置 JENKINS_USER}"
: "${JENKINS_TOKEN:?未设置 JENKINS_TOKEN}"

GITEE_API="https://gitee.com/api/v5"
JENKINS_JOB="test-platform-pr-build"
# 容器内访问 Jenkins 用 8080（宿主机映射 8088→容器 8080）
JENKINS_INTERNAL_URL="${JENKINS_INTERNAL_URL:-http://localhost:8080}"
DONE_LOG="/tmp/pr-poller-done.log"
LOG_FILE="/tmp/pr-poller.log"

log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $*" >> "$LOG_FILE"
}

log "===== PR Poller 开始 ====="

# ---- Step 1: 获取所有 open PR ------------------------------------------------
PR_LIST=$(curl -sS "${GITEE_API}/repos/${GITEE_OWNER}/${GITEE_REPO}/pulls?state=open&access_token=${GITEE_TOKEN}")
PR_COUNT=$(echo "$PR_LIST" | grep -o '"number":[0-9]*' | wc -l) || true
log "发现 ${PR_COUNT} 个 open PR"

# ---- Step 1.5: 没有 open PR 时直接退出 ---------------------------------------
if [ "$PR_COUNT" -eq 0 ]; then
    log "没有 open PR，退出"
    exit 0
fi

# ---- Step 2: 遍历每个 PR ----------------------------------------------------
PR_NUMBERS=$(echo "$PR_LIST" | grep -o '"number":[0-9]*' | grep -o '[0-9]*') || true

for PR_NUMBER in $PR_NUMBERS; do
    # 获取完整 PR 信息
    PR_DETAIL=$(curl -sS "${GITEE_API}/repos/${GITEE_OWNER}/${GITEE_REPO}/pulls/${PR_NUMBER}?access_token=${GITEE_TOKEN}")
    HEAD_SHA=$(echo "$PR_DETAIL" | grep -o '"sha":"[^"]*"' | head -1 | sed 's/"sha":"//;s/"//') || true
    HEAD_REF=$(echo "$PR_DETAIL" | grep -P -o '"head":\{"label":"[^"]*","ref":"[^"]*"' | sed 's/.*"ref":"//;s/"//') || true
    BASE_REF=$(echo "$PR_DETAIL" | grep -P -o '"base":\{"label":"[^"]*","ref":"[^"]*"' | sed 's/.*"ref":"//;s/"//') || true
    PR_TITLE=$(echo "$PR_DETAIL" | grep -o '"title":"[^"]*"' | head -1 | sed 's/"title":"//;s/"//') || true

    log "PR #${PR_NUMBER}: ${PR_TITLE} (${HEAD_REF} → ${BASE_REF}) SHA=${HEAD_SHA}"

    if [ -z "$HEAD_SHA" ]; then
        log "  ⚠️  无法获取 SHA，跳过"
        continue
    fi

    # ---- Step 3: 检查是否已处理过（避免重复触发）-------------------------------
    KEY="${PR_NUMBER}:${HEAD_SHA}"
    if [ -f "$DONE_LOG" ] && grep -qF "$KEY" "$DONE_LOG" 2>/dev/null; then
        log "  已处理过，跳过"
        continue
    fi

    # ---- Step 4: 检查 Gitee commit status（用主仓库，refs/pull/N/head 的 SHA 在主仓库可查）---
    STATUS_JSON=$(curl -sS "${GITEE_API}/repos/${GITEE_OWNER}/${GITEE_REPO}/commits/${HEAD_SHA}/status?access_token=${GITEE_TOKEN}")
    EXISTING_STATUS=$(echo "$STATUS_JSON" | grep -o '"ci/jenkins"' | head -1) || true

    if [ -n "$EXISTING_STATUS" ]; then
        CI_STATE=$(echo "$STATUS_JSON" | grep -o '"ci/jenkins","state":"[^"]*"' | sed 's/.*"state":"//;s/"//') || true
        if [ "$CI_STATE" = "pending" ]; then
            log "  构建进行中（pending），跳过"
            echo "$KEY" >> "$DONE_LOG" || true
            continue
        fi
        log "  已有状态: ${CI_STATE}，跳过"
        echo "$KEY" >> "$DONE_LOG" || true
        continue
    fi

    # ---- Step 5: 设置 pending 状态（用主仓库）--------------------------------
    PENDING_PAYLOAD=$(printf '{"state":"pending","target_url":"%s/job/%s/","description":"CI 构建中…","context":"ci/jenkins"}' \
        "$JENKINS_URL" "$JENKINS_JOB")
    curl -sS -X POST "${GITEE_API}/repos/${GITEE_OWNER}/${GITEE_REPO}/statuses/${HEAD_SHA}?access_token=${GITEE_TOKEN}" \
        -H 'Content-Type: application/json' \
        -d "$PENDING_PAYLOAD" > /dev/null || true
    log "  已设置 pending 状态"

    # ---- Step 6: 触发 Jenkins PR 构建 ------------------------------------------
    TRIGGER_URL="${JENKINS_INTERNAL_URL}/job/${JENKINS_JOB}/buildWithParameters"
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -u "${JENKINS_USER}:${JENKINS_TOKEN}" \
        -X POST "$TRIGGER_URL" \
        --data-urlencode "PR_NUMBER=${PR_NUMBER}" \
        --data-urlencode "PR_SHA=${HEAD_SHA}")

    if [ "$HTTP_CODE" = "201" ]; then
        log "  ✅ Jenkins 构建已触发 (HTTP 201)"
    else
        log "  ❌ Jenkins 触发失败 (HTTP ${HTTP_CODE})"
    fi

    echo "$KEY" >> "$DONE_LOG" || true
done

log "===== PR Poller 结束 ====="
exit 0
