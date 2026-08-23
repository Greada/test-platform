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
  *'/commits/'*'/check-runs'*)
    cat "$MOCK_CHECK_RUNS_FILE" > "$OUTPUT_FILE"
    printf '200\n'
    ;;
  *'/check-runs/'*)
    printf '{"id":9001}' > "$OUTPUT_FILE"
    printf '200\n'
    ;;
  *'/check-runs?'*)
    printf '{"id":9001}' > "$OUTPUT_FILE"
    printf '201\n'
    ;;
  *)
    printf '500\n'
    ;;
esac
EOF
chmod +x "${TMP_DIR}/mock-curl"

run_report() {
  : > "${TMP_DIR}/curl-args.txt"
  set +e
  OUTPUT="$(
    ENV_FILE="${TMP_DIR}/.env.ci" \
    CURL_BIN="${TMP_DIR}/mock-curl" \
    MOCK_CURL_ARGS_FILE="${TMP_DIR}/curl-args.txt" \
    MOCK_CHECK_RUNS_FILE="${TMP_DIR}/check-runs.json" \
    PR_NUMBER=2 \
    PR_ID=18150115 \
    PR_SHA=abc123 \
    CI_STATUS="$1" \
    BUILD_URL=http://localhost:8088/job/test-platform-learn/2/ \
    bash "$REPORT_SCRIPT" 2>&1
  )"
  STATUS=$?
  set -e

  if [ "$STATUS" -ne 0 ]; then
    echo "FAIL: pr-report.sh exited with $STATUS"
    echo "$OUTPUT"
    exit 1
  fi
}

printf '%s' '{"total_count":0,"check_runs":[]}' > "${TMP_DIR}/check-runs.json"
run_report pending
grep -q 'repos/demo-owner/demo-repo/commits/abc123/check-runs' "${TMP_DIR}/curl-args.txt"
grep -q 'repos/demo-owner/demo-repo/check-runs?access_token=demo-token' "${TMP_DIR}/curl-args.txt"
grep -q '"name":"ci/jenkins"' "${TMP_DIR}/curl-args.txt"
grep -q '"head_sha":"abc123"' "${TMP_DIR}/curl-args.txt"
grep -q '"pull_request_id":18150115' "${TMP_DIR}/curl-args.txt"
grep -q '"status":"in_progress"' "${TMP_DIR}/curl-args.txt"

printf '%s' '{"total_count":1,"check_runs":[{"id":9001,"name":"ci/jenkins","head_sha":"abc123"}]}' > "${TMP_DIR}/check-runs.json"
run_report success
grep -q 'repos/demo-owner/demo-repo/commits/abc123/check-runs' "${TMP_DIR}/curl-args.txt"
grep -q 'repos/demo-owner/demo-repo/check-runs/9001?access_token=demo-token' "${TMP_DIR}/curl-args.txt"
grep -q '"status":"completed"' "${TMP_DIR}/curl-args.txt"
grep -q '"conclusion":"success"' "${TMP_DIR}/curl-args.txt"
grep -q '"completed_at":' "${TMP_DIR}/curl-args.txt"

run_report failure
grep -q '"conclusion":"failure"' "${TMP_DIR}/curl-args.txt"

if printf '%s' "$OUTPUT" | grep -q 'demo-token'; then
  echo "FAIL: token leaked into log"
  exit 1
fi

echo "PASS: pr-report writes Gitee Check Runs"
