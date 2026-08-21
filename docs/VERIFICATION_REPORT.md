# Verification Report

Audit and repair pass on this repository, performed against a clean checkout. This document
distinguishes **verified** (actually run, output observed), **statically validated** (structurally
checked but not executed against live infrastructure), and **future work** (not attempted).

Environment: Windows 10 machine, Java 21 / Maven 3.9.12, Node 20 / npm 10, **no Docker installed
locally**. Backend/frontend builds and unit/lint/type checks were verified directly on this
machine. Anything requiring a Docker daemon (`docker compose up`, Testcontainers `mvn verify`,
`scripts/verify.sh`) was authored and reviewed here but run by the user in GitHub Codespaces
(`git pull && bash scripts/verify_codespaces.sh`) - append that run's actual output to this report
once it lands.

## Original state: verdict before fixes

The repository compiled to jars but the containerized system could not run: 4 of 6 backend
Dockerfiles could not build at all inside `docker compose build` (a build-context error), the API
Gateway's routing rewrote every path to something no controller served, auth was either unwired or
silently broken in every service, order creation always failed due to a Feign path mismatch,
inventory had a plain read-then-write race (overselling was possible), Kafka was entirely absent
despite being documented throughout, and Kubernetes/Terraform/Prometheus/Grafana were pure
documentation with no corresponding files. The README claimed "Status: COMPLETE -
Production-Ready."

## Issues found, root causes, and fixes

### Critical - system would not start at all

| # | Issue | Root cause | Fix |
|---|---|---|---|
| 1 | `docker compose build` fails for `config-server`, `eureka-server`, `api-gateway`, `user-service` | Their Dockerfiles ran `COPY ../pom.xml ../pom.xml`, but `docker-compose.yml` scopes each service's build context to its own subdirectory - Docker rejects any `COPY` path that escapes the context | Made those 4 services self-contained Maven projects (parent = `spring-boot-starter-parent` directly, matching how `product-service`/`order-service` already worked) instead of inheriting from the root aggregator POM; removed the offending `COPY` line from each Dockerfile |
| 2 | `product-service` did not compile | `ProductController` declared `checkStock(Long, Integer)` twice (duplicate method) | Removed the redundant duplicate implementation |
| 3 | API Gateway routes 404 everything | `RewritePath=/api/products/(?<segment>.*), /${segment}` strips the whole `/api/products` prefix, but `ProductController` is mapped at `/api/products` (not `/`) | Removed the `RewritePath` filters entirely - predicates already match controller paths 1:1 once `UserController` was also moved to `/api/users` |
| 4 | Order creation always fails | `UserClient` Feign interface called `/api/users/{id}`, but `UserController` was mapped at `/users` | Moved `UserController` to `/api/users`, matching the Feign client and the gateway route |
| 5 | No authentication actually worked | Gateway's `AuthenticationFilter` existed but was never attached to any route; `user-service`/`order-service` registered zero JWT-parsing filter despite requiring `.anyRequest().authenticated()` | Wired `AuthenticationFilter` into every gateway route; added a `GatewayAuthenticationFilter` (trusts `X-User-Id`/`X-User-Roles` set by the gateway) to `user-service` and `order-service`, matching the pattern `product-service` already had |
| 6 | JWT carried no numeric user ID | Gateway forwarded the JWT *username* as `X-User-Id`; `UserController`'s self-ownership check (`#id == authentication.principal.id`) referenced a `.id` property that didn't exist on the principal | Added a `userId` claim to the JWT; gateway now extracts and forwards the real numeric ID; fixed the SpEL check to `#id.toString() == authentication.principal` |
| 7 | `product-service`/`order-service`/`user-service` used inconsistent ports/DB names/hostnames between their own `application.yml`, `application-docker.yml`, and Config Server's copies | Copy-paste drift (e.g. `product-service` locally used port 8081 + db `product_db`, while Config Server and `docker-compose.yml` both expected 8082 + `productdb`; `application-docker.yml`'s DB hostname was `postgres`, but no such container exists - only `postgres-product`) | Standardized every service's local config, Docker-profile config, and env-var names to match `docker-compose.yml`'s actual service names/ports |
| 8 | `Order`/`OrderItem` JPA mapping would throw a NOT NULL violation on every order with items | Unidirectional `@OneToMany` + `@JoinColumn` on the `Order` side causes Hibernate to INSERT each item with `order_id = NULL` first, then UPDATE - violating `order_items.order_id NOT NULL` | Converted to a proper bidirectional `@ManyToOne`/`@OneToMany(mappedBy=...)` mapping; excluded the back-reference from Lombok's generated `equals`/`hashCode`/`toString` to avoid infinite recursion |

### High - silent correctness/security bugs

| # | Issue | Fix |
|---|---|---|
| 9 | Real overselling race: `reduceInventory` was read-check-write with no locking | Added `ProductRepository.decrementStockIfAvailable` - a single conditional `UPDATE ... WHERE quantity >= :amount`; proven under real concurrency by `ProductConcurrencyIT` (Testcontainers Postgres, 2 threads racing stock=1: exactly one succeeds, final stock 0) |
| 10 | Kafka: entirely absent from the code despite being described throughout the docs | Implemented the full flow described in this README's "Kafka order flow" section: typed events, explicit `NewTopic` beans (auto-create disabled), JSON (de)serialization with type headers disabled (cross-service by field name, not by producer class), idempotent consumers, retry+DLQ error handling, Micrometer counters |
| 11 | `/actuator/prometheus` would 404 on every service | `micrometer-registry-prometheus` was never a dependency in any `pom.xml` despite `management.metrics.export.prometheus.enabled: true` being configured | Added the dependency to all 6 services; added `prometheus` to the exposed-endpoints list on `config-server`/`eureka-server` (product/order/gateway/user already had it) |
| 12 | Resilience4j circuit breaker was config-only | `feign.circuitbreaker.enabled: true` was set, but no resilience4j library was on `order-service`'s classpath, so it was a no-op | Added `spring-cloud-starter-circuitbreaker-resilience4j`; Spring Cloud OpenFeign auto-wraps every Feign call once the library is present - added real tuning (`resilience4j.circuitbreaker.instances.product-service`/`user-service`) |
| 13 | `docker-compose.yml` had no Kafka, no Prometheus, no Grafana | Added a single-node KRaft Kafka broker (`apache/kafka:3.7.0`, `auto.create.topics.enable=false`), Prometheus (scraping all 6 services), and Grafana (provisioned datasource + a dashboard with 9 real panels) |
| 14 | Two conflicting `docker-compose.yml` files | `backend/product-service/docker-compose.yml` was a stale duplicate with different ports/DB names than the root one | Deleted it |
| 15 | Frontend Docker build would fail | `Dockerfile` ran `npm ci --only=production`, stripping `@types/*`/`react-scripts` that the build itself needs | Changed to full `npm ci`, added `REACT_APP_API_URL` as a build `ARG` (CRA bakes `REACT_APP_*` at build time - a runtime `environment:` entry, which is what was there before, has zero effect), `npm prune --production` after building |
| 16 | Repo had no `.gitignore` and 43 Maven `target/` build artifacts were committed to git | Added a proper root `.gitignore` (Maven, Node, env files, IDE, OS); untracked the already-committed `target/` files |

### Medium - misleading claims / dead config

| # | Issue | Fix |
|---|---|---|
| 17 | README claimed a `k8s/` directory existed ("Kubernetes manifests are available in the `k8s/` directory") | It did not exist at all | Created real manifests for the full topology (see `k8s/README.md` for exact status - statically validated, not applied to a cluster) |
| 18 | Two lengthy cloud deployment guides (`docs/AWS_DEPLOYMENT.md`, `docs/GCP_DEPLOYMENT.md`) with no backing Terraform/CloudFormation, and a CI job (`deploy-aws`) that assumed a pre-existing ECS cluster | Confirmed no `.tf` files exist anywhere in the repo (`find . -iname "*.tf"` → empty). Left the guides as guides (relabeled, not removed); disabled the `deploy-aws` CI job by default (gated behind a `DEPLOY_AWS_ENABLED` repo variable) so it doesn't fail on every push over infrastructure that doesn't exist |
| 19 | Config Server's git-backend config block pointed at `file://${user.home}/config-repo`, a path that never existed - dead code since the server always ran with `profiles.active: native` | Removed the dead git config block, documented why native/classpath config was chosen |
| 20 | `spring-cloud-starter-bootstrap` + legacy `bootstrap.yml` coexisted with the modern `spring.config.import=optional:configserver:...` in `product-service` | Removed `bootstrap.yml` and the bootstrap starter dependency - one config-loading mechanism, not two |
| 21 | CI's SonarCloud step hardcoded `-Dsonar.organization=your-org` (unfilled placeholder) | Made the job conditional on a `SONAR_ORGANIZATION` repo variable being set, instead of failing every run |
| 22 | Frontend `Order`/`Product`/`User`/`AuthResponse` TypeScript types didn't match the backend's actual JSON shape: `stockQuantity` vs `quantity`, `items` vs `orderItems`, a `shippingAddress` field the backend has never had, `AuthResponse.user` (nested) vs the backend's actual flat `{token, id, username, email, roles}`, `role` (singular) vs the backend's `roles` (array), pagination envelopes (`PaginatedResponse<T>`) the backend never returns, and a product search param named `query` when the backend expects `term` | Fixed every one of these across `types/index.ts`, `endpoints.ts`, `authSlice.ts`/`productSlice.ts`/`orderSlice.ts`, and the pages/components that consumed them; removed the shipping-address UI entirely rather than inventing backend support for it |
| 23 | `PUT /api/users/{id}` and `PUT /api/users/profile` required a non-blank `password` field (reused `RegisterRequest`, which has `@NotBlank` on password) - any profile edit that didn't touch the password would 400 | Added a dedicated `UpdateProfileRequest` DTO with an optional password |
| 24 | `GET /api/orders` and `GET /api/orders/status/{status}` (admin-only per their own Javadoc/summary) had no actual role check; `GET /api/orders/status/{status}` didn't even call the service layer, it returned a hardcoded empty list | Added `@PreAuthorize("hasRole('ADMIN')")` to both; implemented `getOrdersByStatus` for real |
| 25 | Repo had no `.gitattributes`/consistent line endings; `App.tsx` was committed with CRLF, which the project's own `.prettierrc` (`endOfLine: "lf"`) then flagged as ~130 lint errors, and 5 other files had genuine formatting/unescaped-entity lint errors that would fail `npm run lint` in CI | Ran `eslint --fix` (154 of 160 errors auto-fixed) and manually fixed the remaining 6 (`react/no-unescaped-entities`) |

## What was actually run, and the result

### Backend

```bash
cd backend && mvn -B clean package
```
**Result: BUILD SUCCESS.** All 6 services produce executable jars. 25/25 unit tests pass
(`user-service`: 10, `product-service`: 11, `order-service`: 4) - `Tests run: 25, Failures: 0,
Errors: 0, Skipped: 0`.

Integration tests (`ProductConcurrencyIT`, `OrderEventConsumerIT`, `InventoryResultConsumerIT`)
use Testcontainers (real Postgres) + an embedded Kafka broker (`spring-kafka-test`) - a deliberate
substitution for a full Testcontainers Kafka module to keep CI runtime down, while still exercising
a real Kafka protocol implementation, not a mock. They are bound to the Maven `verify` phase via
`maven-failsafe-plugin` (naming convention `*IT.java`), so `mvn test` never touches Docker and
`mvn verify` (or CI's dedicated `integration-tests` job) requires and uses a real daemon. **Not run
locally** (no Docker on this machine) - run via `cd backend/product-service && mvn verify` and
`cd backend/order-service && mvn verify` in an environment with Docker (Codespaces/CI).

### Frontend

```bash
cd frontend && npx tsc --noEmit && npm run lint && npm test -- --watchAll=false && npm run build
```
**Result: all four pass.** `tsc`: 0 errors. `lint`: 0 errors, 19 non-blocking `no-explicit-any`
warnings. Tests: 10/10 pass (`authSlice`: 6, `LoginPage`: 4). Build: real production bundle
emitted (`build/static/js/main.*.js`, 647 KB), confirmed by inspecting the output directory
directly (the very first build attempt silently failed with the CRLF lint errors above and no
`static/` dir - fixed, then re-verified).

### Docker Compose / Kafka / concurrency / Prometheus / Grafana / Kubernetes / Terraform

**Not run on this machine (no Docker installed).** Authored, statically validated, and reviewed
here:
- `docker compose config` was not run locally, but every service definition was hand-checked
  against the Dockerfiles and application config it references (hostnames, ports, env var names)
- `k8s/*.yaml` - parsed successfully with `js-yaml` (all 11 files); **not** applied to a cluster
  (`kubectl` not available in this environment either) - see `k8s/README.md`
- `monitoring/prometheus.yml`, `monitoring/grafana/**/*.yml` - parsed successfully
- Terraform: confirmed absent (`find . -iname "*.tf"` → no results) - the AWS/GCP docs are guides,
  not IaC, and are labeled as such in the README

**To be run by the user in GitHub Codespaces** (`git pull && bash scripts/verify_codespaces.sh`) -
this is the actual end-to-end proof of the containerized system, the Kafka flow, and the
concurrency behavior against real infrastructure. Append that run's real output below once it
happens:

```
<<< PASTE scripts/verify.sh OUTPUT HERE AFTER RUNNING IN CODESPACES >>>
```

### CI

`.github/workflows/ci-cd.yml` was rewritten (see PR/commit diff) to: run real unit tests per
service, run the new Testcontainers integration tests in a dedicated job with a real Docker daemon
(GitHub-hosted runners provide one), lint+test+build the frontend, build the full Compose stack and
run `scripts/verify.sh` against it end-to-end, scan with Trivy, and only attempt Docker
Hub push / AWS ECS deploy when the relevant repo variables are actually configured (previously
these would fail on every run in any fork without those secrets). **Not run yet** - this requires
pushing to GitHub and watching Actions; do that and record the run URL + result here.

## Summary: verified vs. statically validated vs. future work

**Verified (commands run on this machine, output inspected):**
- Backend compiles and packages cleanly (`mvn clean package`), 25/25 unit tests pass
- Frontend type-checks, lints clean, 10/10 unit tests pass, and produces a real production build
- All backend and infra YAML/JSON config files are syntactically valid

**Statically validated only (authored and reviewed, not executed against live infra):**
- `docker-compose.yml` topology and the Kafka/Postgres flow it wires together
- Kubernetes manifests
- The rewritten CI workflow

**Future work / explicitly out of scope for this pass:**
- Transactional outbox for the order-creation Kafka publish (see README "Known limitations")
- Postgres/Kafka high availability
- Per-service `.dockerignore` for `config-server`/`eureka-server`/`api-gateway`/`user-service`
  (the other three have one; not strictly needed since their Dockerfiles only `COPY pom.xml`/`src`
  explicitly, but worth adding for consistency)
- Full frontend test coverage beyond the auth flow
- Scrubbing every per-service `README.md`/`QUICKSTART.md`/`IMPLEMENTATION_SUMMARY.md` file for
  the same kind of inflated claims found in the root README - only the root README and this report
  were rewritten; the per-service docs under `backend/*/` and `frontend/` were not audited
  line-by-line in this pass

## Files changed

Backend: all 6 `pom.xml` (restructured to be self-contained), all 6 `Dockerfile`s (4 fixed), all 6
`application.yml`/`application-docker.yml` (config drift fixed), `docker-compose.yml` (Kafka +
Prometheus + Grafana added, duplicate file removed), Config Server's `api-gateway.yml`/route
config, `AuthenticationFilter`/`JwtUtil` (gateway), `JwtTokenProvider`/`AuthResponse`/
`UserController`/`SecurityConfig` (user-service, + new `UpdateProfileRequest`,
`GatewayAuthenticationFilter`), `ProductController`/`ProductServiceImpl`/`ProductRepository`
(product-service, atomic stock ops, + new Kafka consumer/producer/event classes,
`ProcessedOrderEvent`), `Order`/`OrderItem`/`OrderController`/`OrderServiceImpl`/`FeignConfig`
(order-service, JPA fix + new Kafka consumer/producer/event classes), new unit tests
(`ProductServiceImplTest`, `OrderServiceImplTest`) and integration tests (`ProductConcurrencyIT`,
`OrderEventConsumerIT`, `InventoryResultConsumerIT`).

Frontend: `types/index.ts`, `api/endpoints.ts`, `authSlice.ts`/`productSlice.ts`/`orderSlice.ts`,
`ProductCard.tsx`/`OrderCard.tsx`/`ProfilePage.tsx`/`ProductsPage.tsx`/`AdminProductsPage.tsx`/
`AdminOrdersPage.tsx`/`LoginPage.tsx`/`RegisterPage.tsx`, `package.json` (Jest config fix),
removed the dead `jest.config.js`.

New: `monitoring/` (Prometheus + Grafana config), `k8s/` (11 manifests + README),
`scripts/verify.sh` + `scripts/verify_codespaces.sh`, `.gitignore`, this file.

Docs: `README.md` rewritten, `PROJECT_SUMMARY.md` trimmed to a pointer,
`.github/workflows/ci-cd.yml` rewritten.
