# L8 状态回写实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**目标：** 实现学习版 Pipeline 的 Gitee PR 评论触发与 Check Run 状态回写闭环。

**架构：** `pr-poller-learn.sh` 发现有效 `start build` 评论后先写 in_progress 并触发 Jenkins；`Jenkinsfile-learn` 在 PR 模式中捕获/校验 SHA，并由 Pipeline 级 `post.success/post.failure` 调用 `pr-report.sh` 更新 Check Run。

**2026-08-23 二修订：** Gitee 真实环境验证 `/statuses/{sha}` 返回 `405 Not Allowed`，最终方案改为 Check Runs API。本文后续代码块保留第一版学习记录，实际实现以 `scripts/pr-report.sh` 与 `scripts/pr-poller-learn.sh` 为准。

**技术栈：** Jenkins Declarative Pipeline、Bash、curl、Gitee API v5、Docker Compose。

---

### Task 1：为 `pr-report.sh` 建立可离线测试

**Files:**
- Create: `test-platform/scripts/tests/test-pr-report.sh`

- [x] **Step 1：创建失败测试**

```bash
#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPORT_SCRIPT="${SCRIPT_DIR}/../pr-report.sh"
TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT

cat > "${TMP_DIR}/.env.ci" <<'EOF'
GITEE_OWNER=demo-owner
GITEE_REPO=demo-repo
GITEE_TOKEN=demo-token
JENKINS_URL=http://localhost:8088
EOF

cat > "${TMP_DIR}/mock-curl" <<'EOF'
#!/bin/bash
set -euo pipefail
printf '%s\n' "$@" > "$MOCK_CURL_ARGS_FILE"
printf '201\n'
EOF
chmod +x "${TMP_DIR}/mock-curl"

set +e
OUTPUT="$(
  ENV_FILE="${TMP_DIR}/.env.ci" \
  CURL_BIN="${TMP_DIR}/mock-curl" \
  MOCK_CURL_ARGS_FILE="${TMP_DIR}/curl-args.txt" \
  PR_NUMBER=2 \
  PR_SHA=abc123 \
  CI_STATUS=success \
  BUILD_URL=http://localhost:8088/job/test-platform-learn/2/ \
  bash "$REPORT_SCRIPT" 2>&1
)"
STATUS=$?
set -e

if [ "$STATUS" -eq 0 ]; then
  echo "FAIL: test ran before ENV_FILE/CURL_BIN support was implemented"
  exit 1
fi

echo "PASS: pr-report test fails before implementation"
```

- [x] **Step 2：运行失败测试**

Run: `wsl.exe bash test-platform/scripts/tests/test-pr-report.sh`  
Expected: `PASS: pr-report test fails before implementation`

### Task 2：重写 `pr-report.sh`

**Files:**
- Modify: `test-platform/scripts/pr-report.sh`

- [x] **Step 1：实现最小可用脚本**

```bash
#!/bin/bash
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
BUILD_URL="${BUILD_URL:-${JENKINS_URL:-}/job/${JENKINS_JOB}/}"

case "$CI_STATUS" in
  pending) DEFAULT_DESC="CI 构建中…" ;;
  success) DEFAULT_DESC="CI 构建通过 ✓" ;;
  failure) DEFAULT_DESC="CI 构建失败 ✗" ;;
esac
CI_DESC="${CI_DESC:-$DEFAULT_DESC}"

echo "[pr-report] PR #${PR_NUMBER} SHA=${PR_SHA} → ${CI_STATUS}"

PAYLOAD="$(printf '{"state":"%s","target_url":"%s","description":"%s","context":"ci/jenkins"}' \
  "$CI_STATUS" "$BUILD_URL" "$CI_DESC")"

HTTP_CODE="$("$CURL_BIN" -sS -o /dev/null -w '%{http_code}' -X POST \
  "${GITEE_API}/repos/${GITEE_OWNER}/${GITEE_REPO}/statuses/${PR_SHA}?access_token=${GITEE_TOKEN}" \
  -H 'Content-Type: application/json' \
  -d "$PAYLOAD")"

if [ "$HTTP_CODE" = "200" ] || [ "$HTTP_CODE" = "201" ]; then
  echo "[pr-report] Commit status write succeeded (HTTP ${HTTP_CODE})"
  exit 0
fi

echo "[pr-report] WARNING: Commit status write failed (HTTP ${HTTP_CODE}); build result is unchanged" >&2
exit 0
```

- [x] **Step 2：扩展测试断言**

在 `test-pr-report.sh` 中把失败测试替换为完整断言：

```bash
if [ "$STATUS" -ne 0 ]; then
  echo "FAIL: pr-report.sh exited with $STATUS"
  echo "$OUTPUT"
  exit 1
fi

grep -q 'repos/demo-owner/demo-repo/statuses/abc123' "${TMP_DIR}/curl-args.txt"
grep -q '"state":"success"' "${TMP_DIR}/curl-args.txt"
grep -q '"context":"ci/jenkins"' "${TMP_DIR}/curl-args.txt"
if printf '%s' "$OUTPUT" | grep -q 'demo-token'; then
  echo "FAIL: token leaked into log"
  exit 1
fi
echo "PASS: pr-report success payload"
```

- [x] **Step 3：运行测试**

Run: `wsl.exe bash test-platform/scripts/tests/test-pr-report.sh`  
Expected: `PASS: pr-report success payload`

### Task 3：Pipeline 接入 PR_SHA 与 post 回写

**Files:**
- Modify: `test-platform/Jenkinsfile-learn`

- [x] **Step 1：新增 PR_SHA 参数**

在 `parameters {}` 中加入：

```groovy
string(name: 'PR_SHA', defaultValue: '', description: 'PR head SHA(手动 PR 构建可留空,评论触发时由 Poller 传入)')
```

- [x] **Step 2：Checkout 后固化并校验 SHA**

PR 分支检出成功后：

```groovy
env.PR_SHA = params.PR_SHA ?: ''
if (!env.PR_SHA) {
  env.PR_SHA = sh(returnStdout: true, script: 'git rev-parse HEAD', trim: true)
}
sh 'test "$PR_SHA" = "$(git rev-parse HEAD)"'
```

- [x] **Step 3：Pipeline post 增加回写**

```groovy
post {
  success {
    script {
      if (env.IS_PR == 'true' && env.PR_SHA) {
        withEnv(['CI_STATUS=success']) {
          sh 'bash test-platform/scripts/pr-report.sh || true'
        }
      }
    }
  }
  failure {
    script {
      if (env.IS_PR == 'true' && env.PR_SHA) {
        withEnv(['CI_STATUS=failure']) {
          sh 'bash test-platform/scripts/pr-report.sh || true'
        }
      }
    }
  }
}
```

- [x] **Step 4：静态校验**

Run: `git diff --check`  
Expected: no output

Run: `rg "PR_SHA|CI_STATUS=success|CI_STATUS=failure" test-platform/Jenkinsfile-learn`  
Expected: parameter, SHA capture, and both post blocks are present.

### Task 4：新增学习版 PR 评论 Poller

**Files:**
- Create: `test-platform/scripts/pr-poller-learn.sh`
- Create: `test-platform/scripts/tests/test-pr-poller-learn.sh`

**2026-08-23 修订：采用方案 B，`start build` 评论是唯一触发方式。**

- [x] **Step 1：实现评论唯一触发 Poller**

当前实现规则：

1. 查询 open PR 的 `number`、`head.sha`、`user.login`、`head.user.login`。
2. 查询 PR 评论的 `id`、`body`、`user.login`。
3. 只有 `body.strip() == "start build"` 才算命令。
4. 只有 PR 创建者或 head 分支提交者可以触发。
5. 状态键使用 `manual:PR_NUMBER:COMMENT_ID`。
6. 触发尝试记录使用 `attempt:PR_NUMBER:COMMENT_ID:HEAD_SHA`，同一 SHA 可重试，换 SHA 后旧评论跳过。
7. pending 写成功后才触发 Jenkins。
8. Jenkins 返回 `201` 后才记录评论已处理。
9. 新 SHA 本身不触发，新 SHA 后必须重新评论 `start build`。

下面是历史初稿代码，记录当时的 SHA 自动触发设计；它已被上述方案 B 取代，实际实现以 `scripts/pr-poller-learn.sh` 为准。

```bash
#!/bin/bash
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
STATE_FILE="${STATE_FILE:-/tmp/test-platform-pr-poller.state}"
LOG_FILE="${LOG_FILE:-/tmp/test-platform-pr-poller.log}"
CURL_BIN="${CURL_BIN:-curl}"

log() {
  echo "[$(date '+%Y-%m-%d %H:%M:%S')] $*" >> "$LOG_FILE"
}

touch "$STATE_FILE"
log "===== PR poller started ====="

PR_LIST_JSON="$("$CURL_BIN" -sS "${GITEE_API}/repos/${GITEE_OWNER}/${GITEE_REPO}/pulls?state=open&access_token=${GITEE_TOKEN}")"

PR_LINES="$(printf '%s' "$PR_LIST_JSON" | python3 -c '
import json
import sys

items = json.load(sys.stdin)
for item in items:
    number = item.get("number")
    sha = item.get("head", {}).get("sha", "")
    if number is not None and sha:
        print(f"{number} {sha}")
')"

if [ -z "$PR_LINES" ]; then
  log "No open PR with a valid head SHA"
  exit 0
fi

while read -r PR_NUMBER PR_SHA; do
  KEY="${PR_NUMBER}:${PR_SHA}"
  if grep -qxF "$KEY" "$STATE_FILE"; then
    log "PR #${PR_NUMBER} ${PR_SHA} already processed, skip"
    continue
  fi

  log "PR #${PR_NUMBER} new SHA ${PR_SHA}, writing pending status"
  if ! ENV_FILE="$ENV_FILE" PR_NUMBER="$PR_NUMBER" PR_SHA="$PR_SHA" CI_STATUS=pending \
    BUILD_URL="${JENKINS_URL}/job/${JENKINS_JOB}/" \
    bash "$REPORT_SCRIPT" >> "$LOG_FILE" 2>&1; then
    log "PR #${PR_NUMBER} pending write failed, retry next cycle"
    continue
  fi

  HTTP_CODE="$("$CURL_BIN" -sS -o /dev/null -w '%{http_code}' \
    -u "${JENKINS_USER}:${JENKINS_TOKEN}" \
    -X POST "${JENKINS_INTERNAL_URL}/job/${JENKINS_JOB}/buildWithParameters" \
    --data-urlencode "PR_NUMBER=${PR_NUMBER}" \
    --data-urlencode "PR_SHA=${PR_SHA}")"

  if [ "$HTTP_CODE" = "201" ]; then
    echo "$KEY" >> "$STATE_FILE"
    log "PR #${PR_NUMBER} Jenkins build triggered (HTTP 201)"
  else
    log "PR #${PR_NUMBER} Jenkins trigger failed (HTTP ${HTTP_CODE}), retry next cycle"
  fi
done <<< "$PR_LINES"

log "===== PR poller finished ====="
```

- [x] **Step 2：语法校验**

- [x] **Step 3：离线行为测试**

Run: `wsl.exe bash test-platform/scripts/tests/test-pr-poller-learn.sh`  
Expected: `PASS: pr-poller only triggers exact authorized start build comments once`

Run: `wsl.exe bash -n test-platform/scripts/pr-poller-learn.sh`  
Expected: exit code 0

### Task 5：配置本机凭据与定时任务

**Files:**
- Create outside repository: `/opt/.env.ci`
- Copy inside Jenkins container: `/var/jenkins_home/.env.ci`

- [ ] **Step 1：创建本机配置**

```bash
sudo touch /opt/.env.ci
sudo chmod 600 /opt/.env.ci
sudo nano /opt/.env.ci
```

配置内容：

```text
GITEE_OWNER=greada
GITEE_REPO=test-platform
GITEE_TOKEN=你的Gitee私人令牌
JENKINS_URL=http://localhost:8088
JENKINS_USER=你的Jenkins用户名
JENKINS_TOKEN=你的JenkinsAPI Token
JENKINS_JOB=test-platform-learn
JENKINS_INTERNAL_URL=http://localhost:8088
```

- [ ] **Step 2：复制到 Jenkins 容器**

```bash
docker cp /opt/.env.ci jenkins:/var/jenkins_home/.env.ci
docker exec jenkins chmod 600 /var/jenkins_home/.env.ci
```

- [ ] **Step 3：配置 crontab**

```bash
crontab -e
```

加入：

```text
* * * * * /mnt/d/Java_all/code/test-platform/test-platform/scripts/pr-poller-learn.sh
```

### Task 6：构建与状态验收

- [ ] **Step 1：普通模式回归**

Parameters: `PR_NUMBER` empty, `PR_SHA` empty.  
Expected: existing full pipeline remains SUCCESS and no `[pr-report]` log appears.

- [ ] **Step 2：PR 成功回写**

Parameters: `PR_NUMBER=2`, `PR_SHA=20df4fb2a0e723c42db55954bada569cf39cdb49`.  
Expected: build SUCCESS, `[pr-report] Commit status write succeeded`, Gitee shows `ci/jenkins` success.

- [ ] **Step 3：PR 失败演练**

Use Jenkins Replay on the PR build and temporarily insert:

```groovy
error 'L8 failure drill'
```

Expected: build FAILURE, Gitee shows `ci/jenkins` failure.

- [ ] **Step 4：重复评论防重**

Run poller twice without a new `start build` comment.  
Expected: second run logs `already processed, skip` and no new Jenkins build starts.

- [ ] **Step 5：新 commit 不自动触发**

Push one new commit to PR #2 source branch.  
Expected: no new Jenkins build starts because the old comment cannot represent the new code.

- [ ] **Step 6：新 commit 后重新评论触发**

Push one new commit, then have the PR creator or head author comment exact `start build`.  
Expected: one new build starts and updates the new SHA status.
