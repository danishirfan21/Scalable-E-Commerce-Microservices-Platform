# E-Commerce Microservices Platform

[![CI](https://github.com/danishirfan21/Scalable-E-Commerce-Microservices-Platform/actions/workflows/ci-cd.yml/badge.svg)](https://github.com/danishirfan21/Scalable-E-Commerce-Microservices-Platform/actions/workflows/ci-cd.yml)

A Java 17 / Spring Boot microservices e-commerce backend (product catalog, inventory, orders,
users) with a Kafka-driven, event-driven order-confirmation flow and concurrency-safe inventory,
a React/TypeScript frontend, and a Docker Compose stack (PostgreSQL, Kafka, Prometheus, Grafana)
that actually builds and runs end to end.

**Verified, not just described:** this project was built, then audited and repaired, then proven
against a real Docker/Kafka/PostgreSQL stack - both in a live GitHub Codespace and in the GitHub
Actions run linked by the badge above, which builds every service, runs the Testcontainers
integration suite, and runs the full `docker compose up` + order-flow verification script end to
end - not unit tests in isolation, not a static review. See
[`docs/VERIFICATION_REPORT.md`](docs/VERIFICATION_REPORT.md) for exactly what was broken, what was
fixed, and the full command-by-command evidence log. This README describes the system as it exists
now, not as originally advertised.

The CI badge reflects exactly what the linked workflow does and no more: it proves the build,
tests, and `docker compose` verification pass on GitHub's runners. The SonarCloud, Docker Hub
push, and AWS ECS deploy jobs in that same workflow are intentionally gated off (see "Known
limitations" below) since this repo doesn't ship the external accounts they'd need - the badge
does not claim those jobs ran.

## What this actually demonstrates

- A real multi-service Spring Boot backend (6 services) with Eureka service discovery and a
  Spring Cloud Config Server, each independently buildable and deployable.
- An API Gateway that terminates JWT auth once and forwards trusted identity headers downstream
  (edge authentication, not per-service JWT re-parsing).
- A genuine asynchronous, Kafka-driven order flow: `OrderCreated` → inventory reservation attempt
  → `InventoryReservationResult` → order status becomes `CONFIRMED` or `REJECTED` - with retries,
  a dead-letter topic, and an idempotent consumer (verified by integration tests, not just
  described).
- A concurrency-safe inventory decrement (atomic conditional SQL update) that prevents overselling
  under concurrent requests for the same stock - proven by a Testcontainers-backed test that races
  two orders against a stock of 1.
- A React/TypeScript frontend that actually builds, lints clean, and has its type contracts
  (auth response shape, product/order fields) aligned with what the backend really returns.
- Docker Compose that builds all 6 backend images + frontend + Kafka + Postgres from a clean
  checkout (previously 4 of 6 backend Dockerfiles could not build at all - see the verification
  report).
- Prometheus scraping real `/actuator/prometheus` metrics from every service, plus custom business
  metrics (`orders_created_total`, `orders_confirmed_total`, `orders_rejected_total`,
  `inventory_reservation_rejected_total`, `kafka_consumer_failures_total`) and a Grafana dashboard
  wired to them.

## What this does NOT claim

- **Not "production-ready."** No mTLS between services, no rate limiting, no WAF, no multi-region
  failover, demo-grade secrets committed for local dev.
- **No live cloud deployment.** `docs/AWS_DEPLOYMENT.md` / `docs/GCP_DEPLOYMENT.md` are deployment
  *guides*; there is no Terraform/CloudFormation in this repo and nothing has been deployed to AWS
  or GCP as part of this work.
- **Kubernetes manifests are statically validated, not runtime-tested.** See
  [`k8s/README.md`](k8s/README.md) - no image has been pushed to a registry and no cluster has run
  these manifests.
- **No Saga orchestrator, no CQRS, no event sourcing.** The Kafka order flow is a genuine
  two-step choreography (reserve → confirm/reject) with compensation on partial failure, which is
  worth being specific about rather than reaching for heavier pattern names it doesn't implement.

## Architecture

```
                          ┌─────────────┐
                          │   Frontend  │  (React, nginx, :3000)
                          └──────┬──────┘
                                 │ REST (JWT bearer)
                          ┌──────▼──────┐
                          │ API Gateway │  (:8080) - validates JWT once,
                          └──────┬──────┘   forwards X-User-Id/X-User-Roles
                 ┌───────────────┼────────────────┐
          ┌──────▼─────┐  ┌──────▼──────┐  ┌───────▼──────┐
          │User Service│  │Product Svc  │  │ Order Service│
          │   (:8081)  │  │  (:8082)    │  │   (:8083)    │
          └──────┬─────┘  └──────┬──────┘  └───────┬──────┘
                 │               │                  │
          ┌──────▼─────┐  ┌──────▼──────┐  ┌───────▼──────┐
          │  userdb    │  │ productdb   │  │  orderdb     │
          └────────────┘  └─────────────┘  └──────────────┘

  Eureka (:8761) - service discovery, all 6 services register
  Config Server (:8888) - centralized config (native/classpath backend)
  Kafka (:9092)  - order.created, inventory.reservation.result (+ .DLT topics)
  Prometheus (:9090) / Grafana (:3001) - metrics + dashboard
```

### Kafka order flow

```
POST /api/orders (order-service)
    → order persisted, status = PENDING
    → OrderCreatedEvent published to "order.created"
    → product-service consumes it, atomically reserves stock per item
        (UPDATE products SET quantity = quantity - :n WHERE quantity >= :n)
    → if all items reserved: InventoryReservationResultEvent(approved=true)
      if any item fails: already-reserved items in this order are rolled back,
                          InventoryReservationResultEvent(approved=false, reason)
    → published to "inventory.reservation.result"
    → order-service consumes it, order status → CONFIRMED or REJECTED
      (idempotent: only transitions if the order is still PENDING)
```

Both consumers use `DefaultErrorHandler` + `FixedBackOff` (3 attempts, 1s apart) and a
`DeadLetterPublishingRecoverer` that republishes to `<topic>.DLT` on exhausted retries - no
infinite retry loops, no silently dropped messages. Failures increment a
`kafka_consumer_failures_total` counter.

**Idempotency**: `product-service` keeps a `processed_order_events` table keyed by order ID, so a
redelivered `OrderCreatedEvent` (at-least-once Kafka delivery) is recognized as a duplicate and
skipped rather than double-reserving stock. `order-service`'s status transition is an atomic
conditional UPDATE (`WHERE status = 'PENDING'`), so a redelivered result event is a no-op once the
order has already moved past PENDING.

## Concurrency: preventing overselling

Stock reservation is a single conditional UPDATE, not a read-check-write:

```sql
UPDATE products SET quantity = quantity - :amount
WHERE id = :id AND quantity >= :amount
```

Two concurrent requests for the last unit of stock cannot both succeed - the database's row lock
serializes them, and the loser's UPDATE affects zero rows. This is proven (not just asserted) by
`ProductConcurrencyIT`: stock=1, two threads race a decrement, exactly one succeeds, final stock
is 0. Run it with `mvn -pl backend/product-service verify` against a real Postgres
(Testcontainers).

## Authentication

JWT is issued by `user-service` (`POST /api/auth/register`, `POST /api/auth/login`) and validated
**once**, at the API Gateway. The gateway then forwards `X-User-Id` and `X-User-Roles` headers to
downstream services, which trust them rather than re-parsing the JWT on every hop. This is a
deliberate simplification for a demo running on a single Docker network - in a real deployment,
those internal ports would not be publicly reachable (or you'd add mTLS/network policy so only
the gateway can reach them). `order-service` calling `product-service`/`user-service` internally
stamps a `ROLE_ORDER_SERVICE` identity on its own outgoing requests (see `FeignConfig`), so
service-to-service calls aren't limited to the end user's own permissions.

## Quick start

```bash
git clone <this-repo>
cd "Scalable E-Commerce Microservices Platform"
docker compose up --build -d
bash scripts/verify.sh          # or scripts/verify_codespaces.sh in a Codespace
```

`scripts/verify.sh` builds everything, starts the stack, registers a user, creates a product,
places an order, polls it to `CONFIRMED`, checks stock actually decreased, tests the
insufficient-stock rejection path, and checks Prometheus + Kafka topics. It exits non-zero on any
failure - it is not just a health-endpoint ping.

Service URLs once running:

| Service | URL |
|---|---|
| Frontend | http://localhost:3000 |
| API Gateway | http://localhost:8080 |
| Eureka dashboard | http://localhost:8761 |
| Config Server | http://localhost:8888 |
| Prometheus | http://localhost:9090 |
| Grafana | http://localhost:3001 (admin/admin) |
| user-service (direct) | http://localhost:8081 |
| product-service (direct) | http://localhost:8082 |
| order-service (direct) | http://localhost:8083 |

## Running tests

```bash
# Backend unit tests (no Docker needed)
cd backend && mvn test

# Backend integration tests (needs a Docker daemon - Testcontainers Postgres + embedded Kafka)
cd backend/product-service && mvn verify
cd backend/order-service && mvn verify

# Frontend
cd frontend && npm ci && npm run lint && npm test -- --watchAll=false && npm run build
```

## Stack

- **Backend**: Java 17, Spring Boot 3.2, Spring Cloud 2023.0 (Gateway, Eureka, Config Server,
  OpenFeign, Resilience4j circuit breakers), Spring Data JPA, Spring Kafka, PostgreSQL 15
- **Frontend**: React 18, TypeScript, Redux Toolkit, Material UI, Formik/Yup, Axios
- **Messaging**: Apache Kafka 3.7 (KRaft mode, single broker - no ZooKeeper)
- **Observability**: Micrometer + Prometheus + Grafana
- **Infra**: Docker Compose (primary, verified), Kubernetes manifests (statically validated only)

## What is actually verified

A full live run of `docker compose up --build` + `scripts/verify_codespaces.sh` against a real
GitHub Codespace (Linux, JDK 17, Docker-in-Docker) confirmed all of the following:

- `docker compose up --build` succeeds and all 13 containers reach a healthy state
- Auth works end to end (register + login, both admin and customer roles)
- Product creation and inventory (`PATCH .../inventory`) work
- Placing an order publishes `OrderCreated` and product-service consumes it
- A successful order reaches `CONFIRMED` via the real Kafka round trip (observed in ~4s)
- Stock decreases by exactly the reserved amount after confirmation
- An over-quantity order is correctly `REJECTED`, and stock is left unchanged
- `/actuator/prometheus` exposes real metrics on all three domain services
- Both Kafka topics (`order.created`, `inventory.reservation.result`) exist and are used

Full command-by-command evidence, including the exact defects this live run surfaced, is in
[`docs/VERIFICATION_REPORT.md`](docs/VERIFICATION_REPORT.md).

## Interesting bugs found during live verification

Runtime verification against real infrastructure caught defects that static review and unit tests
missed entirely:

- **Frontend `npm ci` dependency conflict** - `typescript@5.x` vs. `react-scripts@5.0.1`'s stale
  peer-dependency metadata (last updated for TS ^4). Fixed with `legacy-peer-deps=true`.
- **Docker build silently skipped that fix** - the frontend `Dockerfile` copied `package*.json`
  but not `.npmrc` before `npm ci`, so the host build worked but the containerized one didn't.
- **`order-service` crashed on every startup** - a Spring Data derived query,
  `findByOrderId(Long)`, failed because `OrderItem` has a plain `getOrderId()` convenience method
  (not a mapped attribute) that shadowed the parser's usual `order.id` nested-path fallback.
- **The verification script's own polling helper was broken** - it echoed progress lines to
  stdout, which got captured into the same variable as its actual return value, so the
  CONFIRMED/REJECTED check always failed even when the order flow worked correctly.
- **`curl | grep -q` under `set -o pipefail`** - `grep -q`'s early exit on match sent `curl`
  a SIGPIPE mid-response on a 100KB+ body, failing the pipeline even though the metric was there.
- **JDK version drift across Codespaces sessions** - the default Codespaces image ships JDK 11;
  the backend requires 17. A `.devcontainer/devcontainer.json` now pins JDK 17.

## Known limitations

- No transactional outbox for the Kafka publish in `createOrder` - the DB write and the Kafka send
  are not atomic. At-least-once delivery + the idempotent consumer cover the common failure modes,
  but a crash between DB commit and publish would leave an order permanently PENDING. A real
  outbox pattern would close this gap.
- Single-broker Kafka (no replication) and single-instance Postgres per domain - fine for a demo,
  not for HA.
- Frontend test coverage is intentionally light (2 test files) - it proves the build/type/lint
  pipeline works, not full UI coverage.
- No rate limiting, no WAF, no mTLS between services - see "What this does NOT claim" above.
- Authentication is self-issued JWT from `user-service`, not a real identity provider (no
  Okta/Auth0/Keycloak/OAuth2 integration) - fine for a demo, not for production auth.
- No load testing or benchmarking has been done - no throughput/latency numbers are claimed
  anywhere in this repo, deliberately.
- CI builds and runs `scripts/verify.sh` against the Compose stack, but does not screenshot-verify
  Grafana panels - the Grafana dashboard is provisioned and reachable, not visually asserted in CI.
- SonarCloud and AWS ECS deploy CI jobs are present but disabled by default (gated behind repo
  variables) since this repo doesn't ship the SonarCloud org or AWS infra they'd need - see
  `.github/workflows/ci-cd.yml`.

## Documentation

- [`docs/VERIFICATION_REPORT.md`](docs/VERIFICATION_REPORT.md) - the actual audit: what was
  broken, root causes, fixes, and exactly what was run to verify each claim
- [`docs/DATABASE_SCHEMA.md`](docs/DATABASE_SCHEMA.md), [`docs/ER_DIAGRAM.md`](docs/ER_DIAGRAM.md)
- [`docs/AWS_DEPLOYMENT.md`](docs/AWS_DEPLOYMENT.md), [`docs/GCP_DEPLOYMENT.md`](docs/GCP_DEPLOYMENT.md) -
  deployment guides (not IaC, not deployed)
- [`k8s/README.md`](k8s/README.md) - Kubernetes manifest status
- [`docs/postman_collection.json`](docs/postman_collection.json) - API collection
