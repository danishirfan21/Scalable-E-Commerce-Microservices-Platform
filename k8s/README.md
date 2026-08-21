# Kubernetes Manifests

**Status: statically validated, not runtime-tested.** These manifests are structurally valid
Kubernetes YAML (parsed and reviewed against the actual service topology in
`docker-compose.yml`), and correspond to the real services/ports/env vars this repo builds. They
have **not** been applied to a real cluster (no kind/minikube/EKS/AKS run happened as part of
this work) and no image registry push has happened for the `ecommerce/*:latest` image names used
here - **do not read this as a claim of a working, deployed cluster.**

## What's here

- `00-namespace.yaml` - the `ecommerce` namespace
- `01-config-and-secrets.yaml` - shared config + a **placeholder** secret (same demo values as
  `docker-compose.yml`, not real secrets - replace before pointing at anything real)
- `02-postgres.yaml` - one Postgres Deployment+PVC+Service per service (user/product/order)
- `03-kafka.yaml` - single-node KRaft Kafka (mirrors the docker-compose broker config)
- `04-config-server.yaml`, `05-eureka-server.yaml` - infrastructure services
- `06-user-service.yaml`, `07-product-service.yaml`, `08-order-service.yaml` - the three domain
  services, 2 replicas each
- `09-api-gateway.yaml` - gateway, exposed via a NodePort (`30080`) for clusters without an
  ingress controller
- `10-frontend.yaml` - static frontend, NodePort `30000`

## Before actually applying these

1. Build and push real images (the CI workflow's `docker-build-push` job does this) and update
   the `image:` field in each manifest to point at your registry/tag instead of
   `ecommerce/<service>:latest`.
2. Replace the placeholder `Secret` in `01-config-and-secrets.yaml` with real values.
3. `kubectl apply -f k8s/` (apply in the numbered order shown, or apply the whole directory -
   Kubernetes will retry failed dependencies as pods restart, but numbered order avoids the
   initial CrashLoopBackOff noise).
4. If you want to actually validate structure locally: `kubectl apply --dry-run=client -f k8s/`
   or `kind create cluster && kubectl apply -f k8s/`.

## Known gaps versus the docker-compose setup

- No Prometheus/Grafana manifests here (out of scope for this pass - the docker-compose stack has
  them; add `kube-prometheus-stack` via Helm for a real cluster instead of hand-rolling it).
- Postgres runs as a plain Deployment+PVC, not a StatefulSet - fine for a demo, not for a real
  multi-replica/HA database story.
