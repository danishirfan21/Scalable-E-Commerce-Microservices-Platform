#!/usr/bin/env bash
#
# End-to-end verification script for the E-Commerce Microservices Platform.
#
# This does NOT just check /actuator/health. It proves the real retail workflow end to end
# against the actual running stack: register a user, create a product, place an order, and poll
# until Kafka-driven async inventory reservation confirms (or rejects) it - then checks the
# concurrency-safe stock decrement actually happened.
#
# Usage:
#   bash scripts/verify.sh
#
# Environment variables:
#   BASE_URL              Gateway base URL (default: http://localhost:8080)
#   SKIP_BUILD             Set to "true" to skip the Maven/npm build steps (default: false)
#   SKIP_COMPOSE_UP        Set to "true" if the stack is already running (default: false)
#
# Exits non-zero on any failure.

set -uo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
SKIP_BUILD="${SKIP_BUILD:-false}"
SKIP_COMPOSE_UP="${SKIP_COMPOSE_UP:-false}"
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
FAILED=0
STEP=0

log()  { echo -e "\n\033[1;34m[$(( ++STEP ))] $*\033[0m"; }
pass() { echo -e "\033[1;32m  OK: $*\033[0m"; }
fail() { echo -e "\033[1;31m  FAIL: $*\033[0m"; FAILED=1; }
die()  { fail "$*"; echo -e "\033[1;31mAborting - this failure blocks the rest of the verification.\033[0m"; exit 1; }

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || die "required command '$1' not found on PATH"
}

cd "$REPO_ROOT"

require_cmd curl
require_cmd jq
require_cmd docker

# ---------------------------------------------------------------------------
log "Building backend modules (mvn clean install -DskipTests)"
if [ "$SKIP_BUILD" = "true" ]; then
  echo "  skipped (SKIP_BUILD=true)"
else
  if (cd backend && mvn -B clean install -DskipTests); then pass "backend build"; else die "backend build failed"; fi
fi

# ---------------------------------------------------------------------------
log "Running backend unit tests (mvn test)"
if [ "$SKIP_BUILD" = "true" ]; then
  echo "  skipped (SKIP_BUILD=true)"
else
  if (cd backend && mvn -B test); then pass "backend unit tests"; else die "backend unit tests failed"; fi
fi

# ---------------------------------------------------------------------------
log "Building frontend (npm ci, lint, build)"
if [ "$SKIP_BUILD" = "true" ]; then
  echo "  skipped (SKIP_BUILD=true)"
else
  # CI=false: GitHub Actions sets CI=true by default, which makes CRA's build treat every
  # ESLint *warning* as a build-blocking error - including the pre-existing no-explicit-any
  # warnings already accepted as a known baseline (see README "Known limitations"). The lint
  # step just above already runs separately and would fail on any real error.
  if (cd frontend && npm ci && npm run lint && CI=false npm run build); then pass "frontend build"; else die "frontend build failed"; fi
fi

# ---------------------------------------------------------------------------
log "Validating docker-compose.yml"
if docker compose config > /dev/null; then pass "docker compose config is valid"; else die "docker compose config is invalid"; fi

# ---------------------------------------------------------------------------
log "Starting infrastructure (docker compose up --build -d)"
if [ "$SKIP_COMPOSE_UP" = "true" ]; then
  echo "  skipped (SKIP_COMPOSE_UP=true) - assuming the stack is already up"
else
  if docker compose up --build -d; then pass "docker compose up"; else die "docker compose up failed"; fi
fi

# ---------------------------------------------------------------------------
log "Waiting for services to become healthy"
wait_for_health() {
  local name="$1" url="$2" attempts=60
  for ((i = 1; i <= attempts; i++)); do
    health_body=$(curl -sf "$url" 2>/dev/null)
    if [[ "$health_body" == *'"status":"UP"'* ]]; then
      pass "$name healthy"
      return 0
    fi
    sleep 5
  done
  fail "$name did not become healthy within $((attempts * 5))s"
  return 1
}
wait_for_health "config-server"   "http://localhost:8888/actuator/health" || die "config-server never came up"
wait_for_health "eureka-server"   "http://localhost:8761/actuator/health" || die "eureka-server never came up"
wait_for_health "user-service"    "http://localhost:8081/actuator/health" || die "user-service never came up"
wait_for_health "product-service" "http://localhost:8082/actuator/health" || die "product-service never came up"
wait_for_health "order-service"   "http://localhost:8083/actuator/health" || die "order-service never came up"
wait_for_health "api-gateway"     "$BASE_URL/actuator/health" || die "api-gateway never came up"

# ---------------------------------------------------------------------------
log "Verifying gateway health endpoint"
HEALTH_JSON=$(curl -sf "$BASE_URL/actuator/health")
if echo "$HEALTH_JSON" | jq -e '.status == "UP"' > /dev/null; then
  pass "gateway reports UP"
else
  die "gateway health check failed: $HEALTH_JSON"
fi

# ---------------------------------------------------------------------------
log "Registering an admin user"
ADMIN_USERNAME="verify_admin_$$"
ADMIN_PASSWORD="Verify123Pass"
ADMIN_REGISTER_RESPONSE=$(curl -sf -X POST "$BASE_URL/api/auth/register" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"$ADMIN_USERNAME\",\"email\":\"$ADMIN_USERNAME@example.com\",\"password\":\"$ADMIN_PASSWORD\",\"firstName\":\"Verify\",\"lastName\":\"Admin\",\"roles\":[\"ROLE_ADMIN\"]}")
ADMIN_TOKEN=$(echo "$ADMIN_REGISTER_RESPONSE" | jq -r '.token // empty')
if [ -n "$ADMIN_TOKEN" ]; then pass "admin registered, token received"; else die "admin registration failed: $ADMIN_REGISTER_RESPONSE"; fi

log "Logging in as the admin user (proves /api/auth/login independently of register)"
ADMIN_LOGIN_RESPONSE=$(curl -sf -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"usernameOrEmail\":\"$ADMIN_USERNAME\",\"password\":\"$ADMIN_PASSWORD\"}")
ADMIN_TOKEN=$(echo "$ADMIN_LOGIN_RESPONSE" | jq -r '.token // empty')
if [ -n "$ADMIN_TOKEN" ]; then pass "admin login succeeded"; else die "admin login failed: $ADMIN_LOGIN_RESPONSE"; fi

log "Registering a customer user"
CUSTOMER_USERNAME="verify_customer_$$"
CUSTOMER_PASSWORD="Verify123Pass"
CUSTOMER_REGISTER_RESPONSE=$(curl -sf -X POST "$BASE_URL/api/auth/register" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"$CUSTOMER_USERNAME\",\"email\":\"$CUSTOMER_USERNAME@example.com\",\"password\":\"$CUSTOMER_PASSWORD\",\"firstName\":\"Verify\",\"lastName\":\"Customer\"}")
CUSTOMER_TOKEN=$(echo "$CUSTOMER_REGISTER_RESPONSE" | jq -r '.token // empty')
if [ -n "$CUSTOMER_TOKEN" ]; then pass "customer registered, token received"; else die "customer registration failed: $CUSTOMER_REGISTER_RESPONSE"; fi

# ---------------------------------------------------------------------------
log "Creating a product (admin) with initial stock"
SKU="VERIFY-SKU-$$"
PRODUCT_RESPONSE=$(curl -sf -X POST "$BASE_URL/api/products" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d "{\"name\":\"Verify Widget\",\"description\":\"Created by verify.sh\",\"price\":19.99,\"quantity\":10,\"category\":\"test\",\"sku\":\"$SKU\"}")
PRODUCT_ID=$(echo "$PRODUCT_RESPONSE" | jq -r '.id // empty')
if [ -n "$PRODUCT_ID" ]; then pass "product created (id=$PRODUCT_ID, stock=10)"; else die "product creation failed: $PRODUCT_RESPONSE"; fi

log "Setting stock explicitly via PATCH /api/products/{id}/inventory"
SET_STOCK_RESPONSE=$(curl -sf -X PATCH "$BASE_URL/api/products/$PRODUCT_ID/inventory?quantity=10" \
  -H "Authorization: Bearer $ADMIN_TOKEN")
STOCK_AFTER_SET=$(echo "$SET_STOCK_RESPONSE" | jq -r '.quantity // empty')
if [ "$STOCK_AFTER_SET" = "10" ]; then pass "stock set to 10"; else die "setting stock failed: $SET_STOCK_RESPONSE"; fi

# ---------------------------------------------------------------------------
log "Creating an order for 3 units (customer)"
# order-service's first request here is also its first Feign call to user-service - Eureka
# clients only refresh their local peer registry every registry-fetch-interval-seconds (30s by
# default), so a service's own /actuator/health reporting UP does not guarantee its peers'
# Eureka caches have picked it up yet. Retry a few times rather than failing on the first
# ConnectException from a Feign call racing that cache propagation window.
ORDER_ID=""
for attempt in 1 2 3 4 5 6; do
  ORDER_RESPONSE=$(curl -sf -X POST "$BASE_URL/api/orders" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $CUSTOMER_TOKEN" \
    -d "{\"orderItems\":[{\"productId\":$PRODUCT_ID,\"quantity\":3}]}")
  ORDER_ID=$(echo "$ORDER_RESPONSE" | jq -r '.id // empty')
  [ -n "$ORDER_ID" ] && break
  sleep 5
done
ORDER_STATUS=$(echo "$ORDER_RESPONSE" | jq -r '.status // empty')
if [ -n "$ORDER_ID" ]; then pass "order created (id=$ORDER_ID, status=$ORDER_STATUS)"; else die "order creation failed: $ORDER_RESPONSE"; fi

log "Polling order $ORDER_ID until it reaches CONFIRMED (Kafka OrderCreated -> InventoryReserved -> CONFIRMED)"
poll_order_status() {
  local order_id="$1" token="$2" attempts=30
  for ((i = 1; i <= attempts; i++)); do
    local status
    status=$(curl -sf "$BASE_URL/api/orders/$order_id" -H "Authorization: Bearer $token" | jq -r '.status // empty')
    echo "  attempt $i/$attempts: status=$status" >&2
    if [ "$status" = "$3" ]; then
      echo "$status"
      return 0
    fi
    if [ "$status" = "REJECTED" ] && [ "$3" != "REJECTED" ]; then
      echo "$status"
      return 1
    fi
    sleep 2
  done
  echo "$status"
  return 1
}
FINAL_STATUS=$(poll_order_status "$ORDER_ID" "$CUSTOMER_TOKEN" "CONFIRMED")
if [ "$FINAL_STATUS" = "CONFIRMED" ]; then
  pass "order $ORDER_ID reached CONFIRMED"
else
  die "order $ORDER_ID did not reach CONFIRMED (last status: $FINAL_STATUS) - the Kafka order-event flow is broken"
fi

log "Verifying stock decreased by the reserved amount (10 -> 7)"
PRODUCT_AFTER=$(curl -sf "$BASE_URL/api/products/$PRODUCT_ID")
STOCK_AFTER=$(echo "$PRODUCT_AFTER" | jq -r '.quantity // empty')
if [ "$STOCK_AFTER" = "7" ]; then
  pass "stock correctly reduced to 7"
else
  fail "expected stock=7 after reserving 3 of 10, got: $STOCK_AFTER"
fi

# ---------------------------------------------------------------------------
log "Testing insufficient-stock rejection path (ordering more than remains)"
REJECT_ORDER_RESPONSE=$(curl -sf -X POST "$BASE_URL/api/orders" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" \
  -d "{\"orderItems\":[{\"productId\":$PRODUCT_ID,\"quantity\":9999}]}")
REJECT_ORDER_ID=$(echo "$REJECT_ORDER_RESPONSE" | jq -r '.id // empty')
if [ -z "$REJECT_ORDER_ID" ]; then die "could not create the over-quantity order to test rejection: $REJECT_ORDER_RESPONSE"; fi

REJECT_FINAL_STATUS=$(poll_order_status "$REJECT_ORDER_ID" "$CUSTOMER_TOKEN" "REJECTED")
if [ "$REJECT_FINAL_STATUS" = "REJECTED" ]; then
  pass "over-quantity order $REJECT_ORDER_ID correctly REJECTED"
else
  fail "expected order $REJECT_ORDER_ID to be REJECTED, last status: $REJECT_FINAL_STATUS"
fi

STOCK_AFTER_REJECT=$(curl -sf "$BASE_URL/api/products/$PRODUCT_ID" | jq -r '.quantity // empty')
if [ "$STOCK_AFTER_REJECT" = "7" ]; then
  pass "stock unchanged after rejection (still 7)"
else
  fail "expected stock to remain 7 after a rejected order, got: $STOCK_AFTER_REJECT"
fi

# ---------------------------------------------------------------------------
log "Verifying order status query returns the correct final result for both orders"
CONFIRMED_CHECK=$(curl -sf "$BASE_URL/api/orders/$ORDER_ID" -H "Authorization: Bearer $CUSTOMER_TOKEN" | jq -r '.status')
REJECTED_CHECK=$(curl -sf "$BASE_URL/api/orders/$REJECT_ORDER_ID" -H "Authorization: Bearer $CUSTOMER_TOKEN" | jq -r '.status')
[ "$CONFIRMED_CHECK" = "CONFIRMED" ] && pass "order $ORDER_ID query returns CONFIRMED" || fail "order $ORDER_ID query returned $CONFIRMED_CHECK"
[ "$REJECTED_CHECK" = "REJECTED" ] && pass "order $REJECT_ORDER_ID query returns REJECTED" || fail "order $REJECT_ORDER_ID query returned $REJECTED_CHECK"

# ---------------------------------------------------------------------------
log "Verifying Prometheus metrics endpoints are exposed"
for svc_port in "user-service:8081" "product-service:8082" "order-service:8083"; do
  svc="${svc_port%%:*}"; port="${svc_port##*:}"
  # Pure bash substring match - no `| grep` at all. With `set -o pipefail`, piping this large
  # (100KB+) body into `grep -q` (directly, or via an intermediate `echo`/herestring) lets grep
  # close the pipe as soon as it matches, SIGPIPE-ing the writer mid-stream and failing the
  # pipeline even though the match was genuinely found. A plain bash pattern test has no
  # subprocess and no pipe, so there's nothing for grep to SIGPIPE.
  # Retry a few times: under heavy host load right after the order-flow steps, a service's
  # first scrape can hit a transient connection blip even though it's otherwise healthy.
  metrics_ok=0
  for attempt in 1 2 3 4 5 6 7 8 9 10; do
    metrics_body=$(curl -sf "http://localhost:$port/actuator/prometheus" 2>/dev/null)
    if [[ "$metrics_body" == *"jvm_memory_used_bytes"* ]]; then
      metrics_ok=1
      break
    fi
    sleep 2
  done
  if [ "$metrics_ok" = "1" ]; then
    pass "$svc /actuator/prometheus exposes real metrics"
  else
    fail "$svc /actuator/prometheus did not return expected metrics"
  fi
done

# ---------------------------------------------------------------------------
log "Verifying Kafka topics were created (order.created, inventory.reservation.result)"
KAFKA_TOPICS=$(docker compose exec -T kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list 2>/dev/null || true)
topic_exists() {
  local target="$1" line
  while IFS= read -r line; do
    [ "$line" = "$target" ] && return 0
  done <<< "$KAFKA_TOPICS"
  return 1
}
if topic_exists "order.created"; then pass "topic order.created exists"; else fail "topic order.created not found"; fi
if topic_exists "inventory.reservation.result"; then pass "topic inventory.reservation.result exists"; else fail "topic inventory.reservation.result not found"; fi

# ---------------------------------------------------------------------------
if [ "$FAILED" -eq 0 ]; then
  echo -e "\n\033[1;32m=== ALL VERIFICATION STEPS PASSED ===\033[0m"
  exit 0
else
  echo -e "\n\033[1;31m=== ONE OR MORE VERIFICATION STEPS FAILED - see FAIL lines above ===\033[0m"
  exit 1
fi
