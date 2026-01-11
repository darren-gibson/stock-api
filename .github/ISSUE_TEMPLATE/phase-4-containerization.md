---
name: "Phase 4: Containerization & Kubernetes Readiness"
about: "Prepare for container deployment and Kubernetes orchestration"
title: "[Phase 4] Containerization & Kubernetes Readiness"
labels: ["architecture", "cluster-migration", "phase-4", "kubernetes", "devops"]
---

# Phase 4: Containerization & Kubernetes Readiness

## Overview
Prepare the application for container deployment and Kubernetes orchestration. This phase creates multi-role Docker images, Kubernetes manifests, and health checks necessary for production cloud deployments.

**Recommended Feature Branch:** `feat/phase-4-containerization-k8s`

**Depends On:** Phase 3 (Actor Replication & Load Distribution)

## Goals
- Single Dockerfile supporting all node roles
- Kubernetes manifests for multi-node deployments
- Health checks for container orchestration
- Clear deployment documentation and examples
- Local testing with minikube or kind

## Tasks

### 1. Create Multi-Role Dockerfile
- [ ] Create `Dockerfile` in project root:
  - [ ] Multi-stage build (builder stage + runtime stage)
  - [ ] Supports all node roles: HTTP, ACTOR, BOTH
  - [ ] Uses `NODE_ROLE` environment variable to determine startup behavior
  - [ ] Minimal image size (consider Alpine base or distroless)
  - [ ] Include health check command in Dockerfile
  - [ ] Document build args and env vars in comments
- [ ] Create `.dockerignore` to exclude unnecessary files:
  - [ ] Exclude build outputs, test files, .git, etc.
- [ ] Test Dockerfile locally:
  - [ ] Build image: `docker build -t stock-api:latest .`
  - [ ] Run with each role: `docker run -e NODE_ROLE=http|actor|both`
  - [ ] Verify startup logs and port bindings
- [ ] Document image naming convention and registry (if applicable)

### 2. Define Kubernetes Manifests
- [ ] Create `k8s/` directory with manifests:
  - [ ] `k8s/namespace.yaml`: Create namespace (e.g., `stock-api`)
  - [ ] `k8s/http-deployment.yaml`: HTTP server pods
    - [ ] Replicas: configurable (e.g., 2-5)
    - [ ] NODE_ROLE=http environment variable
    - [ ] Port mapping: 8080 for HTTP
    - [ ] Resource requests/limits (CPU, memory)
    - [ ] Liveness and readiness probes
  - [ ] `k8s/actor-deployment.yaml`: Actor node pods
    - [ ] Replicas: configurable (e.g., 2-5)
    - [ ] NODE_ROLE=actor environment variable
    - [ ] Port mapping: 5000 for Actor4k cluster, 8080 for metrics/health
    - [ ] Resource requests/limits
    - [ ] Liveness and readiness probes
  - [ ] `k8s/service.yaml` or separate service files:
    - [ ] ClusterIP service for HTTP nodes (internal routing)
    - [ ] Headless service for actor nodes (for cluster discovery)
    - [ ] Document how pods discover each other (DNS names, StatefulSet)
  - [ ] `k8s/configmap.yaml`: Shared configuration (if needed)
  - [ ] `k8s/ingress.yaml`: (Optional) External routing to HTTP service
- [ ] Document cluster discovery mechanism:
  - [ ] Use headless service for actor nodes (StatefulSet recommended)
  - [ ] HTTP nodes configured with actor service DNS name (e.g., `actor-nodes:5000`)
  - [ ] Seed nodes: automatically populated or manually configured?

### 3. Add Health Checks
- [ ] Implement `/health` endpoint in Ktor:
  - [ ] Create `src/main/kotlin/org/darren/stock/ktor/Health.kt`
  - [ ] GET /health returns HTTP 200 with simple JSON: `{"status": "ok"}`
  - [ ] Add to routing in `Application.kt`
- [ ] Implement readiness check for actor nodes:
  - [ ] GET /ready returns 200 only if cluster is connected
  - [ ] Returns 503 if cluster is unavailable
  - [ ] Readiness probe: HTTP nodes ready immediately, actor nodes wait for cluster join
- [ ] Implement liveness check:
  - [ ] GET /live returns 200 if actor system is responsive
  - [ ] Basic check: no hanging threads or deadlocks
- [ ] Update Dockerfile:
  - [ ] Add HEALTHCHECK directive (or rely on Kubernetes probes)
- [ ] Update Kubernetes manifests with probe definitions:
  - [ ] HTTP nodes:
    ```yaml
    livenessProbe:
      httpGet:
        path: /live
        port: 8080
      initialDelaySeconds: 30
      periodSeconds: 10
    readinessProbe:
      httpGet:
        path: /ready
        port: 8080
      initialDelaySeconds: 10
      periodSeconds: 5
    ```
  - [ ] Actor nodes:
    ```yaml
    readinessProbe:
      httpGet:
        path: /ready
        port: 8080
      initialDelaySeconds: 30
      periodSeconds: 10
    ```

### 4. Document Deployment Scenarios
- [ ] Create `docs/DEPLOYMENT.md`:
  - [ ] **Scenario 1: Single-node dev** (role = BOTH)
    - [ ] Instructions: run locally or Docker
    - [ ] Example command: `docker run -e NODE_ROLE=both -p 8080:8080 stock-api:latest`
  - [ ] **Scenario 2: Multi-node staging** (separate HTTP and actor)
    - [ ] Instructions: Docker Compose example
    - [ ] Include `docker-compose.yaml` in repo
  - [ ] **Scenario 3: Multi-node production with Kubernetes**
    - [ ] Prerequisites: kubectl, minikube/kind, or cloud cluster
    - [ ] Step-by-step deployment:
      1. Build and push image to registry
      2. Update manifests with image tag
      3. Apply manifests: `kubectl apply -f k8s/`
      4. Verify: `kubectl get pods -n stock-api`
      5. Test: forward ports and make HTTP requests
    - [ ] Scaling instructions: `kubectl scale deployment http-server -n stock-api --replicas=5`
    - [ ] Troubleshooting: logs, events, describe commands
  - [ ] Configuration examples for each scenario:
    - [ ] Environment variables
    - [ ] Resource limits
    - [ ] Cluster seed nodes

- [ ] Create `docker-compose.yaml` for multi-node local testing:
  - [ ] Services: http-node, actor-node-1, actor-node-2 (optional)
  - [ ] Networks: internal cluster network
  - [ ] Environment: NODE_ROLE, CLUSTER_SEED_NODES
  - [ ] Ports: 8080 (HTTP), 5000 (cluster)
  - [ ] Example usage in README or DEPLOYMENT.md

### 5. Add Deployment Tests (Optional but Recommended)
- [ ] Docker image build tests:
  - [ ] Build for each role: HTTP, ACTOR, BOTH
  - [ ] Verify image size is acceptable
  - [ ] Test container starts without errors
- [ ] Container runtime tests (test-containers library):
  - [ ] Spin up container in test, verify HTTP endpoints work
  - [ ] Verify environment variables are respected
  - [ ] Test health check endpoints return expected status
- [ ] Docker Compose integration test:
  - [ ] Start HTTP + ACTOR nodes via docker-compose
  - [ ] Make HTTP request; verify it reaches actor node
  - [ ] Verify data consistency across restart
  - [ ] Can be a script in `scripts/test-docker-compose.sh`
- [ ] Kubernetes integration test (minikube):
  - [ ] Deploy manifests to minikube
  - [ ] Verify pods reach ready state
  - [ ] Make HTTP request; verify success
  - [ ] Can be a script in `scripts/test-k8s-minikube.sh`

## Acceptance Criteria

- [ ] Docker image builds successfully for all node roles (HTTP, ACTOR, BOTH)
- [ ] Docker image size is reasonable (< 500MB recommended)
- [ ] Container starts and respects NODE_ROLE environment variable
- [ ] Health check endpoints return correct status
- [ ] Kubernetes manifests are valid (pass `kubectl validate`)
- [ ] Pods reach ready state and communicate successfully (tested locally)
- [ ] HTTP service exposes endpoints correctly
- [ ] Actor service headless DNS works for pod discovery
- [ ] Deployment documentation is clear and tested
- [ ] Multi-node setup (docker-compose) works locally
- [ ] Minikube deployment succeeds (pods ready, endpoints reachable)
- [ ] Code formatting passes `./gradlew spotlessCheck`
- [ ] No new detekt issues: `./gradlew detekt`
- [ ] Build passes: `./gradlew build`

## Testing Checklist

- [ ] Local Docker image tests:
  - [ ] `docker build -t stock-api:latest .` succeeds
  - [ ] `docker run -e NODE_ROLE=both -p 8080:8080 stock-api:latest` starts and is healthy
  - [ ] `docker run -e NODE_ROLE=http ...` and `docker run -e NODE_ROLE=actor ...` both work
- [ ] Docker Compose tests:
  - [ ] `docker-compose up` starts all services
  - [ ] HTTP node reachable at localhost:8080
  - [ ] HTTP requests successfully route to actor nodes
  - [ ] `docker-compose down` cleans up without errors
- [ ] Kubernetes tests (minikube):
  - [ ] `minikube start` (if needed)
  - [ ] `kubectl apply -f k8s/` applies without errors
  - [ ] `kubectl get pods -n stock-api` shows all pods running
  - [ ] Pods reach READY state within 1-2 minutes
  - [ ] Port-forward and test HTTP endpoints
  - [ ] Health checks report correct status
  - [ ] `kubectl delete -f k8s/` cleans up cleanly
- [ ] Existing test suite still passes:
  - [ ] `./gradlew test` passes (no regressions)
  - [ ] Cucumber features pass in containerized environment

## Code Quality Checklist

- [ ] Dockerfile is well-formatted and commented
- [ ] Health check implementation follows existing code style
- [ ] Kubernetes manifests use consistent naming and indentation
- [ ] Documentation is clear and includes code examples
- [ ] docker-compose.yaml is valid YAML and documented
- [ ] All scripts in `scripts/` are executable and tested

## Deliverable

Merge PR with Dockerfile, Kubernetes manifests, and deployment documentation. Application is ready for container deployment and Kubernetes orchestration.

## Related Issues/PRs

- Implements Phase 4 of: [Cluster Architecture Migration](../docs/CLUSTER_MIGRATION_APPROACH.md)
- Depends on: Phase 3 (Actor Replication & Load Distribution)
- Prerequisite for: Phase 5 (Observability & Operational Readiness)

## Definition of Done

- [ ] All acceptance criteria met
- [ ] All tests passing locally and in CI
- [ ] Code review approved
- [ ] Documentation reviewed and tested by team member
- [ ] PR merged to main
- [ ] Images built and optionally pushed to registry

## Related Files

- `Dockerfile`: Multi-stage, multi-role
- `docker-compose.yaml`: Local multi-node testing
- `k8s/`: All Kubernetes manifests
- `docs/DEPLOYMENT.md`: Deployment guide
- `scripts/test-docker-compose.sh`: Docker Compose test script
- `scripts/test-k8s-minikube.sh`: Minikube test script

## Notes

- Consider using a container registry (Docker Hub, ECR, GCR, ACR) for image storage
- Document image tagging strategy (e.g., v1.0.0, latest)
- Consider adding pre-deployment validation (e.g., manifest linting)
- For production, consider using Kustomize or Helm for managing multiple environments
