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
DONE_LOG="/tmp/pr-poller-done.log"
LOG_FILE="/tmp/pr-poller.log"

log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $*" >> "$LOG_FILE"
}

log "===== PR Poller 开始 ====="

# ---- Step 1: 获取所有 open PR ------------------------------------------------
PR_LIST=$(curl -sS "${GITEE_API}/repos/${GITEE_OWNER}/${GITEE_REPO}/pulls?state=open&access_token=${GITEE_TOKEN}")
PR_COUNT=$(echo "$PR_LIST" | grep -o '"number":[0-9]*' | wc -l)
log "发现 ${PR_COUNT} 个 open PR"

# ---- Step 2: 遍历每个 PR ----------------------------------------------------
echo "$PR_LIST" | grep -o '"number":[0-9]*,"title":"[^"]*","head":[^}]*}' | while IFS= read -r pr_item; do
    PR_NUMBER=$(echo "$pr_item" | sed 's/.*"number":\([0-9]*\).*/\1/')
    # 获取完整 PR 信息
    PR_DETAIL=$(curl -sS "${GITEE_API}/repos/${GITEE_OWNER}/${GITEE_REPO}/pulls/${PR_NUMBER}?access_token=${GITEE_TOKEN}")
    HEAD_SHA=$(echo "$PR_DETAIL" | grep -o '"sha":"[^"]*"' | head -1 | sed 's/"sha":"//;s/"//')
    HEAD_REF=$(echo "$PR_DETAIL" | grep -o '"ref":"[^"]*"' | head -1 | sed 's/"ref":"//;s/"//')
    BASE_REF=$(echo "$PR_DETAIL" | grep -o '"base":[^}]*}' | grep -o '"ref":"[^"]*"' | sed 's/"ref":"//;s/"//')
    PR_TITLE=$(echo "$PR_DETAIL" | grep -o '"title":"[^"]*"' | head -1 | sed 's/"title":"//;s/"//')

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

    # ---- Step 4: 检查 Gitee commit status ------------------------------------
    STATUS_JSON=$(curl -sS "${GITEE_API}/repos/${GITEE_OWNER}/${GITEE_REPO}/commits/${HEAD_SHA}/status?access_token=${GITEE_TOKEN}")
    EXISTING_STATUS=$(echo "$STATUS_JSON" | grep -o '"ci/jenkins"' | head -1)

    if [ -n "$EXISTING_STATUS" ]; then
        # 已有 ci/jenkins 状态，检查是否还在 pending
        CI_STATE=$(echo "$STATUS_JSON" | grep -o '"ci/jenkins","state":"[^"]*"' | sed 's/.*"state":"//;s/"//')
        if [ "$CI_STATE" = "pending" ]; then
            log "  构建进行中（pending），跳过"
            # 仍然写入 done log 避免重复触发
            echo "$KEY" >> "$DONE_LOG"
            continue
        fi
        log "  已有状态: ${CI_STATE}，跳过"
        echo "$KEY" >> "$DONE_LOG"
        continue
    fi

    # ---- Step 5: 设置 pending 状态 -------------------------------------------
    PENDING_PAYLOAD=$(printf '{"state":"pending","target_url":"%s/job/%s/","description":"CI 构建中…","context":"ci/jenkins"}' \
        "$JENKINS_URL" "$JENKINS_JOB")
    curl -sS -X POST "${GITEE_API}/repos/${GITEE_OWNER}/${GITEE_REPO}/statuses/${HEAD_SHA}?access_token=${GITEE_TOKEN}" \
        -H 'Content-Type: application/json' \
        -d "$PENDING_PAYLOAD" > /dev/null
    log "  已设置 pending 状态"

    # ---- Step 6: 触发 Jenkins PR 构建 ------------------------------------------
    TRIGGER_URL="${JENKINS_URL}/job/${JENKINS_JOB}/buildWithParameters"
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -u "${JENKINS_USER}:${JENKINS_TOKEN}" \
        -X POST "$TRIGGER_URL" \
        --data-urlencode "PR_NUMBER=${PR_NUMBER}" \
        --data-urlencode "PR_SHA=${HEAD_SHA}" \
        --data-urlencode "PR_HEAD_REF=${HEAD_REF}" \
        --data-urlencode "PR_BASE_REF=${BASE_REF}")

    if [ "$HTTP_CODE" = "201" ]; then
        log "  ✅ Jenkins 构建已触发 (HTTP 201)"
    else
        log "  ❌ Jenkins 触发失败 (HTTP ${HTTP_CODE})"
    fi

    echo "$KEY" >> "$DONE_LOG"
done

log "===== PR Poller 结束 ====="
