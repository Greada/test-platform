#!/bin/bash
# =============================================================================
# pr-report.sh — 回写 Gitee Pull Request Commit Status
#
# 由 Jenkinsfile 中 Report PR Status 阶段调用。
# 参数（通过环境变量传入）：
#   PR_NUMBER     — PR 编号
#   PR_SHA        — 最新 commit SHA
#   CI_STATUS     — success / failure
#   BUILD_URL     — Jenkins 构建链接
#   CI_DESC       — 描述文字（可选，默认自动生成）
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
    echo "[ERROR] .env.ci 未找到"
    exit 1
fi

: "${GITEE_OWNER:?未设置 GITEE_OWNER}"
: "${GITEE_REPO:?未设置 GITEE_REPO}"
: "${GITEE_TOKEN:?未设置 GITEE_TOKEN}"
: "${PR_SHA:?未设置 PR_SHA}"
: "${CI_STATUS:?未设置 CI_STATUS}"

GITEE_API="https://gitee.com/api/v5"
JENKINS_JOB="test-platform-pr-build"

# 参数默认值
PR_NUMBER="${PR_NUMBER:-unknown}"
BUILD_URL="${BUILD_URL:-${JENKINS_URL}/job/${JENKINS_JOB}/}"

# 根据状态生成描述
if [ "$CI_STATUS" = "success" ]; then
    CI_DESC="${CI_DESC:-CI 构建通过 ✓}"
elif [ "$CI_STATUS" = "failure" ]; then
    CI_DESC="${CI_DESC:-CI 构建失败 ✗}"
elif [ "$CI_STATUS" = "pending" ]; then
    CI_DESC="${CI_DESC:-CI 构建中…}"
else
    CI_DESC="${CI_DESC:-CI 状态: ${CI_STATUS}}"
fi

echo "[pr-report] PR #${PR_NUMBER} SHA=${PR_SHA} → ${CI_STATUS} (repo=${GITEE_OWNER}/${GITEE_REPO})"

# 回写 Gitee commit status
STATUS_PAYLOAD=$(printf '{"state":"%s","target_url":"%s","description":"%s","context":"ci/jenkins"}' \
    "$CI_STATUS" "$BUILD_URL" "$CI_DESC")

HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST \
    "${GITEE_API}/repos/${GITEE_OWNER}/${GITEE_REPO}/statuses/${PR_SHA}?access_token=${GITEE_TOKEN}" \
    -H 'Content-Type: application/json' \
    -d "$STATUS_PAYLOAD")

if [ "$HTTP_CODE" = "201" ] || [ "$HTTP_CODE" = "200" ]; then
    echo "[pr-report] ✅ Commit status 回写成功"
else
    echo "[pr-report] ⚠️  Commit status 回写失败 (HTTP ${HTTP_CODE}) — 不阻塞流水线"
fi
