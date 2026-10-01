#!/usr/bin/env bash
#
# Kilivana backend smoke test.
#
# Exercises every API group against a running backend and prints a pass/fail table.
# Bash and curl only: no Python, no test framework, nothing to install.
#
#   ./scripts/smoke-test.sh [base-url]
#
# Requires: curl, jq
# Run the backend first (mvn spring-boot:run).

set -uo pipefail

BASE="${1:-http://localhost:8080}"
PASS_COUNT=0
FAIL_COUNT=0
RESULTS=()

# ---------------------------------------------------------------- helpers

login() {
  local email="$1" password="$2" attempt=1 out=""
  while [ "$attempt" -le "$RETRIES" ]; do
    out=$(curl -s -X POST "$BASE/api/v1/auth/login" \
            -H 'Content-Type: application/json' \
            -d "{\"email\":\"$email\",\"password\":\"$password\"}" \
            2>/dev/null | jq -r '.data.accessToken // empty' 2>/dev/null)
    [ -n "$out" ] && { printf '%s' "$out"; return 0; }
    sleep 2
    attempt=$((attempt + 1))
  done
  return 1
}

# User id resolved from a token, with retries so one dropped /auth/me does not silently
# leave an empty id in a path and produce a confusing 400 later.
user_id_for() {
  local token="$1" attempt=1 out=""
  while [ "$attempt" -le "$RETRIES" ]; do
    out=$(curl -s "$BASE/api/v1/auth/me" -H "Authorization: Bearer $token" \
            2>/dev/null | jq -r '.data.id // empty' 2>/dev/null)
    [ -n "$out" ] && { printf '%s' "$out"; return 0; }
    sleep 2
    attempt=$((attempt + 1))
  done
  echo "0"
}

# The free ngrok tier intermittently drops connections, which surfaces as HTTP code 000
# with no body at all. Retrying distinguishes tunnel flakiness from a real failure, and
# keeps a dropped request from being reported as a broken endpoint.
RETRIES="${KV_SMOKE_RETRIES:-3}"

# curl_get_code <curl args...> -> echoes the status code
# Retries only on 000 (no response) or a curl transport error.
curl_with_retry() {
  local attempt=1 code rc
  while :; do
    code=$("$@" 2>/dev/null)
    rc=$?
    # 000 means curl never got a status line: dropped connection, DNS or timeout.
    if [ "$code" != "000" ] && [ $rc -eq 0 ]; then
      printf '%s' "$code"
      return 0
    fi
    if [ "$attempt" -ge "$RETRIES" ]; then
      printf '%s' "$code"
      return 1
    fi
    sleep 2
    attempt=$((attempt + 1))
  done
}

# check <expected> <method> <path> <token> [body] [content-type]
check() {
  local expected="$1" method="$2" path="$3" token="$4"
  local body="${5:-}" ctype="${6:-application/json}"

  local args=(-s -o /tmp/kv_smoke_body -w '%{http_code}'
              -X "$method" "$BASE$path")
  [ -n "$token" ] && args+=(-H "Authorization: Bearer $token")
  if [ -n "$body" ]; then
    args+=(-H "Content-Type: $ctype" -d "$body")
  fi

  local code
  code=$(curl_with_retry curl "${args[@]}")
  if [ "$code" != "$expected" ]; then
    FAIL_COUNT=$((FAIL_COUNT + 1))
    RESULTS+=("FAIL|$method $path|expected $expected got ${code:-none}|$(head -c 160 /tmp/kv_smoke_body | tr -d '\n')")
    return
  fi
  PASS_COUNT=$((PASS_COUNT + 1))
  RESULTS+=("PASS|$method $path|$code|")
}

section() {
  RESULTS+=("---|$1||")
}

# check_form <expected> <path> <token> <field>=@<file>
#
# Uses curl -F, not -d. -d sends the value as a literal string, so "@file" would arrive as
# the text "@file" inside a multipart body that Tomcat then fails to parse. File uploads
# have to go through -F or the request is malformed and the 500 is the test's fault, not
# the server's.
check_form() {
  local expected="$1" path="$2" token="$3" form="$4"

  local code curl_rc
  # Must be a single command: an unescaped newline inside $(...) ends the curl invocation
  # and turns -X/-H/-F into separate commands, which fails with exit 127.
  local fargs=(-s -o /tmp/kv_smoke_body -w '%{http_code}'
               -X POST "$BASE$path"
               -H "Authorization: Bearer $token"
               -F "$form")
  code=$(curl_with_retry curl "${fargs[@]}")
  curl_rc=$?

  if [ "$code" != "$expected" ]; then
    FAIL_COUNT=$((FAIL_COUNT + 1))
    RESULTS+=("FAIL|POST $path|expected $expected got '${code}'|curl exit ${curl_rc}: $(head -c 160 /tmp/kv_smoke_body | tr -d '\n')")
    return
  fi
  PASS_COUNT=$((PASS_COUNT + 1))
  RESULTS+=("PASS|POST $path|$code|")
}

# ---------------------------------------------------------------- tokens

printf 'Waiting for %s ... ' "$BASE"
for _ in $(seq 1 30); do
  curl -sf "$BASE/actuator/health" -o /dev/null && break
  sleep 2
done
echo 'up'

ADMIN=$(login "admin.test@kilivana.local" "${DEV_PASSWORD:-Kilivana#2026}")
SUPER=$(login "superadmin.test@kilivana.local" "${DEV_PASSWORD:-Kilivana#2026}")
FARMER=$(login "farmer.test@kilivana.local" "${DEV_PASSWORD:-Kilivana#2026}")
BUYER=$(login "buyer.test@kilivana.local" "${DEV_PASSWORD:-Kilivana#2026}")
SUPPLIER=$(login "supplier.test@kilivana.local" "${DEV_PASSWORD:-Kilivana#2026}")
DRIVER=$(login "driver.test@kilivana.local" "${DEV_PASSWORD:-Kilivana#2026}")
INSPECTOR=$(login "inspector.test@kilivana.local" "${DEV_PASSWORD:-Kilivana#2026}")

ME=$(user_id_for "$DRIVER")
DRIVER_ID="$ME"
FARMER_ID=$(user_id_for "$FARMER")
BUYER_ID=$(user_id_for "$BUYER")
ADMIN_ID=$(user_id_for "$ADMIN")
SUPPLIER_ID=$(user_id_for "$SUPPLIER")
INSPECTOR_ID=$(user_id_for "$INSPECTOR")

if [ -z "$DRIVER" ] || [ -z "$ADMIN" ]; then
  echo "Cannot log in as a seeded account. Is DEV_SEED_ENABLED=true and the password correct?"
  exit 1
fi

# ---------------------------------------------------------------- tests

section "Health & system"
check 200 GET "/" ""
check 200 GET "/api-docs" ""

section "Authentication"
check 200 POST "/api/v1/auth/login" "" '{"email":"driver.test@kilivana.local","password":"Kilivana#2026"}'
check 401 POST "/api/v1/auth/login" "" '{"email":"driver.test@kilivana.local","password":"wrong"}'
check 401 GET "/api/v1/auth/me" ""
check 200 GET "/api/v1/auth/me" "$DRIVER"

section "Role enforcement"
check 403 GET "/api/v1/admin/dashboard/stats" "$DRIVER"
check 200 GET "/api/v1/admin/dashboard/stats" "$ADMIN"
check 200 GET "/api/v1/admin/dashboard/stats" "$SUPER"

section "Administration — users"
check 200 GET "/api/v1/admin/users" "$ADMIN"
check 200 GET "/api/v1/admin/users/role/FARMER" "$ADMIN"
check 200 GET "/api/v1/admin/users/status/ACTIVE" "$ADMIN"

section "Administration — reporting"
check 200 GET "/api/v1/admin/dashboard" "$ADMIN"
check 200 GET "/api/v1/admin/dashboard/stats" "$ADMIN"
check 200 GET "/api/v1/admin/orders" "$ADMIN"
check 200 GET "/api/v1/admin/payments" "$ADMIN"
check 200 GET "/api/v1/admin/logistics/jobs" "$ADMIN"
check 200 GET "/api/v1/admin/reports" "$ADMIN"
check 200 GET "/api/v1/admin/audit-logs" "$ADMIN"

section "Administration — notifications"
check 200 GET "/api/v1/admin/notifications" "$ADMIN"

section "E-commerce — catalog"
check 200 GET "/api/v1/categories" "$ADMIN"
check 200 GET "/api/v1/categories/active" "$ADMIN"

section "E-commerce — products"
check 200 GET "/api/v1/products/search" "$ADMIN"
check 200 GET "/api/v1/sellers/1/products" "$ADMIN"

section "E-commerce — orders"
check 200 GET "/api/v1/orders" "$ADMIN"
check 200 GET "/api/v1/orders/search" "$ADMIN"

section "E-commerce — cart & checkout"
check 200 GET "/api/v1/cart" "$BUYER"

section "Logistics — driver profiles"
check 200 GET "/api/v1/profiles/drivers/$DRIVER_ID" "$DRIVER"
check 403 GET "/api/v1/profiles/drivers/$FARMER_ID" "$DRIVER"
check 200 GET "/api/v1/profiles/drivers/$DRIVER_ID/images" "$DRIVER"

section "Logistics — jobs"
check 200 GET "/api/v1/logistics/jobs" "$ADMIN"
check 200 GET "/api/v1/logistics/jobs/driver/$DRIVER_ID" "$DRIVER"
check 200 GET "/api/v1/logistics/jobs/status/PENDING_ASSIGNMENT" "$ADMIN"

section "Logistics — proof of delivery & tracking"
check 200 GET "/api/v1/logistics/proof-of-delivery/driver/$DRIVER_ID" "$DRIVER"
check 200 GET "/api/v1/logistics/tracking-events/driver/$DRIVER_ID" "$DRIVER"

section "Notifications (user)"
check 200 GET "/api/v1/notifications/user/$BUYER_ID" "$BUYER"

section "Addresses"
check 200 GET "/api/v1/addresses" "$BUYER"

section "Role profiles — other roles"
check 200 GET "/api/v1/profiles/farmers/$FARMER_ID" "$FARMER"
check 200 GET "/api/v1/profiles/buyers/$BUYER_ID" "$BUYER"
check 200 GET "/api/v1/profiles/suppliers/$SUPPLIER_ID" "$SUPPLIER"
check 200 GET "/api/v1/profiles/inspectors/$INSPECTOR_ID" "$INSPECTOR"

section "Email"
check 400 POST "/api/v1/admin/mail/test" "$ADMIN" '{"to":""}'
# An authenticated non-staff caller is 403 FORBIDDEN, not 401: the token is valid, the
# role is not. 401 is reserved for a missing or bad token.
check 403 POST "/api/v1/admin/mail/test" "$DRIVER" '{"to":"someone@example.com"}'
check 401 POST "/api/v1/admin/mail/test" "" '{"to":"someone@example.com"}'

section "Delivery OTP lifecycle"
JOB=$(curl -s -X POST "$BASE/api/v1/logistics/jobs" \
      -H "Authorization: Bearer $DRIVER" -H 'Content-Type: application/json' \
      -d '{"orderId":1,"pickupAddress":"Smoke Test Pickup","destinationAddress":"Smoke Test Destination","cargoDescription":"Test cargo","quantity":5,"payoutAmount":1500.00}' \
      | jq -r '.data.id // empty')
if [ -n "$JOB" ]; then
  check 400 POST "/api/v1/logistics/jobs/$JOB/otp/verify?otp=000000" "$DRIVER"
  check 400 POST "/api/v1/logistics/jobs/$JOB/proof-of-delivery" "$DRIVER" \
        "{\"logisticsJobId\":$JOB,\"recipientName\":\"Nobody\"}"
else
  RESULTS+=("FAIL|POST /api/v1/logistics/jobs|could not create a job||")
  FAIL_COUNT=$((FAIL_COUNT + 1))
fi

section "Image upload"
# mktemp requires the X template at the very end of the name, so build the suffix separately.
PNG=$(mktemp -d)/kv_smoke.png
printf '\211PNG\r\n\032\n' > "$PNG"
# A real multipart request with the wrong part name must be a 400, not a 500.
check_form 400 "/api/v1/profiles/drivers/$DRIVER_ID/images" "$DRIVER" "photo=@$PNG;type=image/png"
# Correctly formed upload reaches storage, which is unconfigured until CLOUDINARY_* is set.
check_form 503 "/api/v1/profiles/drivers/$DRIVER_ID/images" "$DRIVER" "image=@$PNG;type=image/png"
rm -rf "$(dirname "$PNG")"

# ---------------------------------------------------------------- report

echo
printf '%-6s %-58s %-22s %s\n' "RESULT" "ENDPOINT" "STATUS" "DETAIL"
printf '%-6s %-58s %-22s %s\n' "------" "----------------------------------------------------------" "----------------------" "------"
for r in "${RESULTS[@]}"; do
  IFS='|' read -r ok method status detail <<< "$r"
  printf '%-6s %-58s %-22s %s\n' "$ok" "$method" "$status" "$detail"
done

echo
echo "passed: $PASS_COUNT   failed: $FAIL_COUNT"
[ "$FAIL_COUNT" -eq 0 ] || exit 1