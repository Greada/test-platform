#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
TEST_REVIEW_SCRIPT="${SCRIPT_DIR}/../pr-test-review.sh"
TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT

cat > "${TMP_DIR}/.env.ci" <<'EOF'
GITEE_OWNER=demo-owner
GITEE_REPO=demo-repo
GITEE_TOKEN=demo-token
EOF

cat > "${TMP_DIR}/mock-curl" <<'EOF'
#!/bin/bash
set -euo pipefail

printf '%s\n' "$*" >> "$MOCK_CURL_ARGS_FILE"
cat "$MOCK_RESPONSE_FILE" > "$MOCK_OUTPUT_FILE"
printf '%s\n' "$MOCK_HTTP_CODE"
EOF
chmod +x "${TMP_DIR}/mock-curl"

run_review() {
  : > "${TMP_DIR}/curl-args.txt"
  set +e
  OUTPUT="$(
    ENV_FILE="${TMP_DIR}/.env.ci" \
    CURL_BIN="${TMP_DIR}/mock-curl" \
    MOCK_CURL_ARGS_FILE="${TMP_DIR}/curl-args.txt" \
    MOCK_OUTPUT_FILE="${TMP_DIR}/response-body" \
    MOCK_HTTP_CODE="$1" \
    PR_NUMBER=2 \
    bash "$TEST_REVIEW_SCRIPT" 2>&1
  )"
  STATUS=$?
  set -e
}

printf '' > "${TMP_DIR}/response-body"
run_review 200
if [ "$STATUS" -ne 0 ]; then
  echo "FAIL: pr-test-review.sh exited with $STATUS"
  echo "$OUTPUT"
  exit 1
fi

grep -q 'repos/demo-owner/demo-repo/pulls/2/test?access_token=demo-token' "${TMP_DIR}/curl-args.txt"
grep -q -- '-X POST' "${TMP_DIR}/curl-args.txt"
if grep -q 'force' "${TMP_DIR}/curl-args.txt"; then
  echo "FAIL: normal tester write should not force pass"
  exit 1
fi
if printf '%s' "$OUTPUT" | grep -q 'demo-token'; then
  echo "FAIL: token leaked into log"
  exit 1
fi

run_review 403
if [ "$STATUS" -ne 0 ]; then
  echo "FAIL: review write failure should not fail the Jenkins build"
  echo "$OUTPUT"
  exit 1
fi
if ! printf '%s' "$OUTPUT" | grep -q 'WARNING'; then
  echo "FAIL: review write failure should emit warning"
  exit 1
fi

echo "PASS: pr-test-review marks the Gitee PR test item"
