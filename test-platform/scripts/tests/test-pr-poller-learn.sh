#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
POLLER_SCRIPT="${SCRIPT_DIR}/../pr-poller-learn.sh"
TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT

cat > "${TMP_DIR}/.env.ci" <<'EOF'
GITEE_OWNER=demo-owner
GITEE_REPO=demo-repo
GITEE_TOKEN=demo-token
JENKINS_URL=http://jenkins.example.com
JENKINS_INTERNAL_URL=http://jenkins.internal
JENKINS_USER=demo-user
JENKINS_TOKEN=demo-jenkins-token
JENKINS_JOB=test-platform-learn
EOF

cat > "${TMP_DIR}/mock-curl" <<'EOF'
#!/bin/bash
set -euo pipefail

OUTPUT_FILE=""
PREVIOUS_ARG=""
for arg in "$@"; do
  if [ "$PREVIOUS_ARG" = "-o" ]; then
    OUTPUT_FILE="$arg"
  fi
  PREVIOUS_ARG="$arg"
done

printf '%s\n' "$*" >> "$MOCK_CURL_ARGS_FILE"

case " $* " in
  *'/pulls?state=open'*)
    cat "$MOCK_PR_LIST_FILE"
    ;;
  *'/comments'*)
    cat "$MOCK_COMMENTS_FILE"
    ;;
  *'buildWithParameters'*)
    cat "$MOCK_TRIGGER_CODE_FILE"
    ;;
  *'/commits/'*'/check-runs'*)
    printf '%s' '{"total_count":0,"check_runs":[]}' > "$OUTPUT_FILE"
    printf '200\n'
    ;;
  *'/check-runs?'*)
    printf '{"id":9001}' > "$OUTPUT_FILE"
    printf '201\n'
    ;;
  *'/check-runs/'*)
    printf '{"id":9001}' > "$OUTPUT_FILE"
    printf '200\n'
    ;;
  *)
    printf '201\n'
    ;;
esac
EOF
chmod +x "${TMP_DIR}/mock-curl"

run_poller() {
  : > "${TMP_DIR}/curl-args.txt"
  ENV_FILE="${TMP_DIR}/.env.ci" \
    STATE_FILE="${TMP_DIR}/state.txt" \
    LOG_FILE="${TMP_DIR}/poller.log" \
    CURL_BIN="${TMP_DIR}/mock-curl" \
    MOCK_CURL_ARGS_FILE="${TMP_DIR}/curl-args.txt" \
    MOCK_PR_LIST_FILE="${TMP_DIR}/prs.json" \
    MOCK_COMMENTS_FILE="${TMP_DIR}/comments.json" \
    MOCK_TRIGGER_CODE_FILE="${TMP_DIR}/trigger-code.txt" \
    bash "$POLLER_SCRIPT"
}

trigger_count() {
  grep -c 'buildWithParameters' "${TMP_DIR}/curl-args.txt" || true
}

printf '201\n' > "${TMP_DIR}/trigger-code.txt"
printf '%s\n' '[{"id":18150115,"number":2,"head":{"sha":"abc123","user":{"login":"head-author"}},"user":{"login":"pr-author"}}]' > "${TMP_DIR}/prs.json"
printf '%s\n' '[]' > "${TMP_DIR}/comments.json"

run_poller
if [ "$(trigger_count)" -ne 0 ]; then
  echo "FAIL: PR without comments should not trigger Jenkins"
  exit 1
fi
if [ -s "${TMP_DIR}/state.txt" ]; then
  echo "FAIL: PR without comments should not record a processed comment"
  exit 1
fi
if grep -q 'PR #2 comment  author is not allowed' "${TMP_DIR}/poller.log"; then
  echo "FAIL: empty comment list should not be treated as one empty comment"
  exit 1
fi

printf '%s\n' '[{"id":101,"body":"  start build  ","user":{"login":"head-author"}}]' > "${TMP_DIR}/comments.json"
run_poller
if [ "$(trigger_count)" -ne 1 ]; then
  echo "FAIL: exact start build from head author should trigger one build"
  exit 1
fi
if ! grep -qxF 'manual:2:101' "${TMP_DIR}/state.txt"; then
  echo "FAIL: processed comment was not recorded by comment ID"
  exit 1
fi
if ! grep -q 'PR_SHA=abc123' "${TMP_DIR}/curl-args.txt"; then
  echo "FAIL: triggered build did not use current PR SHA"
  exit 1
fi
if ! grep -q '"pull_request_id":18150115' "${TMP_DIR}/curl-args.txt"; then
  echo "FAIL: pending check-run did not use the Gitee PR internal ID"
  exit 1
fi

run_poller
if [ "$(trigger_count)" -ne 0 ]; then
  echo "FAIL: duplicate start build comment should not trigger again"
  exit 1
fi

printf '%s\n' '[{"id":102,"body":"start build","user":{"login":"visitor"}}]' > "${TMP_DIR}/comments.json"
run_poller
if [ "$(trigger_count)" -ne 0 ]; then
  echo "FAIL: start build from non-author should not trigger Jenkins"
  exit 1
fi
if grep -qxF 'manual:2:102' "${TMP_DIR}/state.txt"; then
  echo "FAIL: rejected comment was incorrectly recorded as processed"
  exit 1
fi

printf '%s\n' '[{"id":103,"body":"please start build","user":{"login":"head-author"}},{"id":104,"body":"start build now","user":{"login":"pr-author"}}]' > "${TMP_DIR}/comments.json"
run_poller
if [ "$(trigger_count)" -ne 0 ]; then
  echo "FAIL: non-exact command should not trigger Jenkins"
  exit 1
fi

printf '%s\n' '[{"id":18150115,"number":2,"head":{"sha":"def456","user":{"login":"head-author"}},"user":{"login":"pr-author"}}]' > "${TMP_DIR}/prs.json"
printf '%s\n' '[{"id":101,"body":"  start build  ","user":{"login":"head-author"}}]' > "${TMP_DIR}/comments.json"
run_poller
if [ "$(trigger_count)" -ne 0 ]; then
  echo "FAIL: new SHA without a new start build comment should not trigger Jenkins"
  exit 1
fi

printf '%s\n' '[{"id":105,"body":"start build","user":{"login":"head-author"}}]' > "${TMP_DIR}/comments.json"
run_poller
if [ "$(trigger_count)" -ne 1 ]; then
  echo "FAIL: new SHA with a new start build comment should trigger one build"
  exit 1
fi
if ! grep -qxF 'manual:2:105' "${TMP_DIR}/state.txt"; then
  echo "FAIL: new valid comment was not recorded"
  exit 1
fi

if grep -q 'demo-token\|demo-jenkins-token' "${TMP_DIR}/poller.log"; then
  echo "FAIL: token leaked into poller log"
  exit 1
fi

printf '500\n' > "${TMP_DIR}/trigger-code.txt"
printf '%s\n' '[{"id":106,"body":"start build","user":{"login":"head-author"}}]' > "${TMP_DIR}/comments.json"
run_poller
if [ "$(trigger_count)" -ne 1 ]; then
  echo "FAIL: Jenkins failure scenario should attempt one trigger"
  exit 1
fi
if grep -qxF 'manual:2:106' "${TMP_DIR}/state.txt"; then
  echo "FAIL: failed Jenkins trigger was recorded as processed"
  exit 1
fi

printf '%s\n' '[{"id":18150115,"number":2,"head":{"sha":"abc123","user":{"login":"head-author"}},"user":{"login":"pr-author"}}]' > "${TMP_DIR}/prs.json"
printf '%s\n' '[{"id":107,"body":"start build","user":{"login":"head-author"}}]' > "${TMP_DIR}/comments.json"
run_poller
if [ "$(trigger_count)" -ne 1 ]; then
  echo "FAIL: failed trigger should retry once on the same SHA"
  exit 1
fi
if ! grep -qxF 'attempt:2:107:abc123' "${TMP_DIR}/state.txt"; then
  echo "FAIL: trigger attempt was not bound to its SHA"
  exit 1
fi

printf '201\n' > "${TMP_DIR}/trigger-code.txt"
printf '%s\n' '[{"id":18150115,"number":2,"head":{"sha":"def456","user":{"login":"head-author"}},"user":{"login":"pr-author"}}]' > "${TMP_DIR}/prs.json"
run_poller
if [ "$(trigger_count)" -ne 0 ]; then
  echo "FAIL: old unprocessed comment should not trigger a new SHA"
  exit 1
fi
if grep -qxF 'manual:2:107' "${TMP_DIR}/state.txt"; then
  echo "FAIL: stale comment was recorded as processed"
  exit 1
fi

printf '%s\n' '[{"id":108,"body":"start build","user":{"login":"head-author"}}]' > "${TMP_DIR}/comments.json"
run_poller
if [ "$(trigger_count)" -ne 1 ]; then
  echo "FAIL: new comment should trigger the new SHA"
  exit 1
fi
if ! grep -qxF 'manual:2:108' "${TMP_DIR}/state.txt"; then
  echo "FAIL: new comment was not recorded after success"
  exit 1
fi

echo "PASS: pr-poller only triggers exact authorized start build comments once"
