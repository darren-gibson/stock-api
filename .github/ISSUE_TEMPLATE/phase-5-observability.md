---
name: "Phase 5: Observability & Operational Readiness"
about: "Add monitoring, logging, and tracing for distributed system operations"
title: "[Phase 5] Observability & Operational Readiness"
labels: ["architecture", "cluster-migration", "phase-5", "observability", "operations"]
---

# Phase 5: Observability & Operational Readiness

## Overview
Add monitoring, logging, and tracing for the distributed cluster system. This phase ensures operators have visibility into system behavior, can troubleshoot issues, and can effectively manage the cluster at scale.

**Recommended Feature Branch:** `feat/phase-5-observability-operations`

**Depends On:** Phase 4 (Containerization & Kubernetes Readiness)

## Goals
- Comprehensive cluster logging with trace IDs
- Key metrics for cluster health and performance
- Distributed tracing across cluster nodes
- Operational runbook for common tasks
- Effective troubleshooting guides

## Tasks

### 1. Enhance Cluster Logging
- [ ] Add structured logging with cluster context:
  - [ ] Create `src/main/kotlin/org/darren/stock/logging/ClusterLogging.kt`
  - [ ] Extend existing `kotlin-logging` usage with structured fields:
    - [ ] `nodeId`: unique identifier for this node
    - [ ] `nodeRole`: HTTP, ACTOR, or BOTH
    - [ ] `traceId`: unique request ID (propagated across cluster)
    - [ ] `actorPath`: which actor processed message (actor nodes)
    - [ ] `clusterId`: cluster name/ID (if applicable)

- [ ] Update application startup logging:
  - [ ] Log node role, cluster enabled, seed nodes, ports
  - [ ] Format: `[Node: {nodeId}, Role: {role}, Cluster: {enabled}] Starting application`
  - [ ] Log cluster join/leave events with timestamp
  - [ ] Log actor creation/destruction with location ID

- [ ] Add request tracing:
  - [ ] HTTP endpoints: generate traceId from request (Ktor feature or custom)
  - [ ] Pass traceId to actor messages via Actor4k cluster
  - [ ] Log all routing decisions with traceId for debugging
  - [ ] Ensure existing logging already captures key operations (refer to `kotlin-logging` setup)

- [ ] Update logback configuration:
  - [ ] Ensure `src/main/resources/logback.xml` includes cluster context fields
  - [ ] Add separate appender for cluster events (optional, for easier filtering)
  - [ ] Document log format for operators

### 2. Add Cluster Metrics
- [ ] Identify key metrics:
  - [ ] **Cluster Health**: number of active nodes, cluster state (forming/stable/unhealthy)
  - [ ] **Actor Metrics**: messages sent/received, actors per location, actor mailbox size (if observable)
  - [ ] **HTTP Metrics**: request count by endpoint, routing latency (HTTP to actor), error rate
  - [ ] **Routing Metrics**: actor lookup time, cache hit rate (if caching implemented)

- [ ] Integrate with existing metrics system:
  - [ ] Determine if project uses micrometer, Prometheus, or custom metrics
  - [ ] Create `src/main/kotlin/org/darren/stock/metrics/ClusterMetrics.kt`
  - [ ] Define metric names consistently (e.g., `stock.cluster.nodes.active`, `stock.actor.messages.sent`)
  - [ ] Document metrics constants for team reference

- [ ] Expose metrics:
  - [ ] Add `/metrics` endpoint (or integrate with existing monitoring)
  - [ ] Format: Prometheus format (text) or JSON (depending on system)
  - [ ] Include cluster metrics alongside existing application metrics

- [ ] Create metrics dashboard:
  - [ ] Example Grafana dashboard definition (JSON) for common queries
  - [ ] Document in `docs/OBSERVABILITY.md`

### 3. Implement Distributed Tracing (Optional but Recommended)
- [ ] Add trace context propagation:
  - [ ] Generate or extract traceId from HTTP requests (e.g., `X-Trace-ID` header)
  - [ ] Propagate traceId through Actor4k cluster messages
  - [ ] Log traceId in all operations for end-to-end request tracking
  - [ ] Consider OpenTelemetry integration (if team uses it)

- [ ] Enable request tracking across nodes:
  - [ ] Example flow: HTTP node receives request → generates traceId → routes to actor node → logs all operations with traceId
  - [ ] Query logs by traceId to reconstruct full request path

- [ ] Integrate with logging framework:
  - [ ] Ensure traceId appears in all log messages related to a request
  - [ ] Use MDC (Mapped Diagnostic Context) for context propagation
  - [ ] Update logback pattern to include traceId

### 4. Create Operational Runbook
- [ ] Create `docs/OPERATIONS.md` with detailed procedures:

#### 4a. Scaling Operations
  - [ ] **Scale HTTP Tier (Add/Remove Nodes)**:
    - [ ] Kubernetes: `kubectl scale deployment http-server -n stock-api --replicas=N`
    - [ ] What to monitor: pod startup time, request distribution
    - [ ] Troubleshooting: if pod stuck in pending, check resource availability
  - [ ] **Scale Actor Tier (Add/Remove Nodes)**:
    - [ ] Kubernetes: `kubectl scale deployment actor-nodes -n stock-api --replicas=N`
    - [ ] Expected: cluster discovers new nodes, begins routing traffic within seconds
    - [ ] New actors may be re-balanced to new node (depends on Phase 3 implementation)
    - [ ] Verify: `kubectl logs -f deployment/actor-nodes -n stock-api` shows cluster join
  - [ ] **Expected Behavior During Scaling**:
    - [ ] Existing requests continue (no disruption)
    - [ ] New requests go to newly available nodes
    - [ ] Logs show cluster membership changes

#### 4b. Node Failure Handling
  - [ ] **Actor Node Failure (Unexpected)**:
    - [ ] Cluster detects failure (within heartbeat timeout, e.g., 10s)
    - [ ] HTTP nodes stop routing to failed node
    - [ ] Requests failover to replicas (or re-routed)
    - [ ] Monitoring: watch for error spike in metrics
    - [ ] Recovery: manual intervention (remove pod) or Kubernetes controller (restarts pod)
  - [ ] **HTTP Node Failure**:
    - [ ] No direct impact to actor nodes
    - [ ] Load balancer routes traffic to healthy HTTP nodes
    - [ ] Existing connections drop (HTTP stateless, so clients retry)
  - [ ] **Network Partition (Split-Brain)**:
    - [ ] Document behavior: which partition continues operating?
    - [ ] Recovery: manual intervention to heal network or isolate partition

#### 4c. Backup and Recovery
  - [ ] **State Backup** (if applicable):
    - [ ] If actor state is persisted, document backup procedure
    - [ ] Frequency: daily, on-demand, or continuous replication
  - [ ] **Recovery Procedure**:
    - [ ] How to restore actor state from backup
    - [ ] Expected downtime and data loss RTO/RPO

#### 4d. Monitoring and Alerting
  - [ ] **Key Metrics to Monitor**:
    - [ ] Cluster node count (alert if < expected minimum)
    - [ ] HTTP error rate (alert if > threshold)
    - [ ] Actor message latency (alert if > baseline + threshold)
    - [ ] Pod resource usage (CPU, memory)
  - [ ] **Alerting Rules**:
    - [ ] Document alerting thresholds and conditions
    - [ ] Example: alert if cluster nodes < 2 for > 5 minutes
  - [ ] **Log Analysis**:
    - [ ] Key log patterns to watch for: errors, cluster join/leave, actor failures
    - [ ] Example queries (for log aggregation system, e.g., Loki, ELK):
      ```
      # Find all cluster join events
      {job="stock-api"} |= "cluster" |= "joined"
      
      # Find errors by traceId
      {job="stock-api"} traceId="abc123" level="ERROR"
      
      # Count actor messages per node
      {job="stock-api"} nodeId=~"actor-.*" | stats count by nodeId
      ```

#### 4e. Configuration Management
  - [ ] **Configuration Updates** (without restart):
    - [ ] Document which settings require pod restart vs. dynamic update
    - [ ] Procedure for updating cluster seed nodes (if dynamic)
    - [ ] Procedure for changing replica count or resource limits
  - [ ] **Environment Variable Reference**:
    - [ ] Full list of supported env vars with defaults
    - [ ] Example: `NODE_ROLE=http|actor|both`, `CLUSTER_PORT=5000`, `HTTP_PORT=8080`

### 5. Create Troubleshooting Guide
- [ ] Create `docs/TROUBLESHOOTING.md`:

  - [ ] **Problem: Pods not reaching ready state**
    - [ ] Check: `kubectl describe pod {pod-name} -n stock-api` → Events section
    - [ ] Common causes: resource unavailable, image pull error, health check timing
    - [ ] Solution: increase CPU/memory, verify image registry access, adjust probe delays

  - [ ] **Problem: HTTP requests timeout or error**
    - [ ] Check: logs for routing errors, actor node availability
    - [ ] Command: `kubectl logs deployment/http-server -n stock-api | grep ERROR`
    - [ ] Check metrics: are actor nodes responding? are they overloaded?
    - [ ] Solution: scale actor tier, check network connectivity

  - [ ] **Problem: Actor node not joining cluster**
    - [ ] Check: logs for cluster join errors
    - [ ] Command: `kubectl logs deployment/actor-nodes -n stock-api | grep "cluster"`
    - [ ] Verify seed nodes are reachable: `nslookup actor-nodes.stock-api.svc.cluster.local`
    - [ ] Check firewall/network policies allow port 5000
    - [ ] Solution: verify seed node configuration, check network policies

  - [ ] **Problem: Uneven actor distribution across nodes**
    - [ ] Expected: depends on Phase 3 rebalancing strategy
    - [ ] Check: metrics or logs showing actor placement
    - [ ] If concerning: manually trigger rebalancing (if supported)

  - [ ] **Problem: Data inconsistency across replicas**
    - [ ] Check: did failover occur? was state properly replicated?
    - [ ] Verify: replication strategy is working (Phase 3)
    - [ ] Solution: check logs for replication errors, may require manual recovery

  - [ ] **Problem: High latency between HTTP and actor nodes**
    - [ ] Check: network latency using `ping` between pods
    - [ ] Check: actor message queue depth (if observable)
    - [ ] Check: actor node resource utilization (CPU, memory)
    - [ ] Solution: scale actor tier, optimize code, check network performance

  - [ ] **Generic Debugging Checklist**:
    - [ ] Check pod status: `kubectl get pods -n stock-api`
    - [ ] Check pod logs: `kubectl logs -f {pod-name} -n stock-api`
    - [ ] Check pod events: `kubectl describe pod {pod-name} -n stock-api`
    - [ ] Check metrics: connect to Grafana/Prometheus dashboard
    - [ ] Check cluster status: internal API (if available) or logs

## Acceptance Criteria

- [ ] Logs clearly indicate node ID, role, and which actor processes each request
- [ ] Trace IDs propagate across HTTP → actor routing and appear in logs
- [ ] Key cluster metrics are available and exposed (node count, messages, latency)
- [ ] Metrics dashboard (or template) provided for monitoring
- [ ] Operational runbook covers scaling, failure handling, monitoring, configuration
- [ ] Troubleshooting guide addresses common scenarios with solutions
- [ ] Runbook tested: team member unfamiliar with system follows it successfully
- [ ] All existing tests pass (no regressions)
- [ ] Code formatting passes `./gradlew spotlessCheck`
- [ ] No new detekt issues: `./gradlew detekt`
- [ ] Build passes: `./gradlew build`

## Testing Checklist

- [ ] Local smoke test:
  - [ ] Start multi-node setup (docker-compose)
  - [ ] Make HTTP request
  - [ ] Verify logs include traceId, nodeId, nodeRole
  - [ ] Verify metrics endpoint returns cluster metrics
- [ ] Kubernetes test:
  - [ ] Deploy to minikube
  - [ ] Verify logs accessible via `kubectl logs`
  - [ ] Verify metrics accessible via port-forward
  - [ ] Verify health checks functional
- [ ] Operational procedures tested:
  - [ ] Scaling up/down actor nodes on minikube
  - [ ] Simulating node failure (kill pod) and verifying recovery
  - [ ] Checking logs and metrics during events

## Code Quality Checklist

- [ ] Logging implementation follows `kotlin-logging` patterns (existing style)
- [ ] Metrics follow naming conventions (e.g., `stock.cluster.*`)
- [ ] Documentation is clear, includes code examples, and is tested
- [ ] No unused imports or variables
- [ ] Distributed tracing context properly propagated through code

## Deliverable

Merge PR with observability, metrics, and operational documentation. Application is ready for production operations with clear visibility and runbooks.

## Related Issues/PRs

- Implements Phase 5 of: [Cluster Architecture Migration](../docs/CLUSTER_MIGRATION_APPROACH.md)
- Depends on: Phase 4 (Containerization & Kubernetes Readiness)
- Final phase: enables production cluster deployments

## Definition of Done

- [ ] All acceptance criteria met
- [ ] All tests passing locally and in CI
- [ ] Code review approved
- [ ] Documentation reviewed and tested (by ops/on-call person if available)
- [ ] PR merged to main
- [ ] Team trained on runbook (optional for final phase)

## Related Files

- `src/main/kotlin/org/darren/stock/logging/ClusterLogging.kt`: Cluster logging utilities
- `src/main/kotlin/org/darren/stock/metrics/ClusterMetrics.kt`: Cluster metrics
- `src/main/resources/logback.xml`: Updated logging configuration
- `docs/OBSERVABILITY.md`: Observability strategy and dashboards
- `docs/OPERATIONS.md`: Operational runbook
- `docs/TROUBLESHOOTING.md`: Troubleshooting guide
- `grafana-dashboard.json`: Example Grafana dashboard

## Notes

- Phase 5 is the final phase; after this, the cluster migration is complete
- Team training on new operational procedures recommended before go-live
- Consider dry-run exercises (disaster recovery simulation) before production
- Post-production monitoring for the first few weeks to identify edge cases
