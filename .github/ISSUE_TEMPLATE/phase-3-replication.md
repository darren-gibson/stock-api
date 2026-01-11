---
name: "Phase 3: Actor Replication & Load Distribution"
about: "Enable multiple actor nodes to share state and handle load"
title: "[Phase 3] Actor Replication & Load Distribution"
labels: ["architecture", "cluster-migration", "phase-3"]
---

# Phase 3: Actor Replication & Load Distribution

## Overview
Enable multiple actor nodes to host concurrent instances of the same actor system, with proper state management and failover. This phase adds resilience and horizontal scalability for the actor tier.

**Recommended Feature Branch:** `feat/phase-3-actor-replication-distribution`

**Depends On:** Phase 2 (HTTP-to-Actor Message Routing)

## Goals
- Multiple actor nodes hosting same actor system concurrently
- HTTP nodes can route requests to any available actor node
- Resilience to single actor node failure (requests route to replica)
- Automatic cluster membership detection and rebalancing
- No data loss during actor migration
- All existing tests pass; new tests cover failover scenarios

## Tasks

### 1. Design Actor Replication Strategy
- [ ] Document decision on replication model:
  - [ ] Active-active (all replicas serve requests, state synchronized)
  - [ ] Master-slave (primary handles writes, replicas handle reads)
  - [ ] Other: _________________
  - [ ] Record rationale and tradeoffs in `docs/CLUSTER_ARCHITECTURE.md`
- [ ] Document failover behavior if actor node goes down:
  - [ ] When does failover trigger? (immediate vs. timeout)
  - [ ] Is new actor instance created on another node or redirected to replica?
  - [ ] How is state recovered?
- [ ] Decide on actor location lookup strategy:
  - [ ] HTTP node caches actor references (performance vs. stale data risk)
  - [ ] HTTP node queries cluster on each request (overhead vs. always current)
  - [ ] Hybrid: cache with TTL
- [ ] Document decisions in architectural decision record (ADR)

### 2. Implement Actor Distribution Logic
- [ ] Extend Koin configuration for actor pools on actor nodes:
  - [ ] Create `ActorPoolManager` to manage multiple actor instances per location
  - [ ] Actor nodes create/maintain pools on startup based on cluster assignments
  - [ ] Implement placement strategy (hash-based, round-robin, etc.)
  - [ ] Document strategy and how new locations are assigned
- [ ] Implement actor lookup on HTTP nodes:
  - [ ] HTTP nodes query cluster to find actor node for given location ID
  - [ ] Implement routing strategy (consistent hashing recommended)
  - [ ] Cache actor references if decided in task 1
- [ ] Update `RemoteStockActorService` (from Phase 2):
  - [ ] Add actor discovery logic before sending request
  - [ ] Handle "actor not found" scenario gracefully

### 3. Handle Actor Migration and Rebalancing
- [ ] Add cluster event listener (Actor4k provides this):
  - [ ] Listen for node join events: trigger rebalancing if desired
  - [ ] Listen for node leave events: migrate affected actors
  - [ ] Document migration strategy: re-create vs. copy state
- [ ] Implement rebalancing logic (if desired):
  - [ ] Strategy: do we rebalance existing actors when new node joins?
  - [ ] Or do new actors go to new node, existing stay put?
  - [ ] Coordinate migrations to avoid double-processing
- [ ] Add cluster state tracking:
  - [ ] Track which actors live on which nodes
  - [ ] Expose via internal API or metrics for debugging

### 4. Add Resilience Tests
- [ ] Create new test file: `src/test/kotlin/org/darren/stock/steps/MultiNodeActorTest.kt`
  - [ ] Test 1: Start 3 actor nodes with stock data; HTTP makes requests; stop 1 actor node
    - [ ] Verify requests continue to succeed (failover to replicas)
    - [ ] Verify response consistency
  - [ ] Test 2: HTTP nodes request actor lookup; new actor node joins
    - [ ] Verify new requests can be routed to new node
    - [ ] Verify old requests still work (no disruption)
  - [ ] Test 3: Actor node crashes; HTTP detects unavailability
    - [ ] Verify appropriate error returned (or retry succeeds)
    - [ ] Verify automatic failover (if implemented)
  - [ ] Test 4: Split-brain scenario (optional): simulate network partition
    - [ ] Document behavior and recovery

- [ ] Add Cucumber scenarios: `src/test/resources/org/darren/stock/3. Actor Replication.feature`
  - [ ] Scenario: "Multiple actor nodes host same location data"
  - [ ] Scenario: "HTTP node routes requests to any available actor node"
  - [ ] Scenario: "Actor node failure triggers automatic failover"
  - [ ] Scenario: "New actor node joins cluster and receives traffic"

### 5. Update Feature Coverage
- [ ] Run all Cucumber scenarios with multi-node setup:
  - [ ] Verify location tracking works across actor node boundaries
  - [ ] Test stock operations on replicated actors (move, update, etc.)
  - [ ] Verify idempotency across replicas
- [ ] Add load test scenario:
  - [ ] Multiple concurrent HTTP requests distributed across actor nodes
  - [ ] Verify no bottlenecks or dropped requests

## Acceptance Criteria

- [ ] Multiple actor nodes can host the same actor system concurrently
- [ ] HTTP nodes successfully route requests to any available actor node
- [ ] If one actor node fails, requests route to replica on another node (no data loss)
- [ ] Cluster automatically detects node additions and removals
- [ ] No data loss during actor migration
- [ ] Rebalancing strategy documented and implemented (or explicitly deferred with reasoning)
- [ ] All existing tests pass (backward compatible)
- [ ] New tests cover failover and multi-node scenarios
- [ ] Response time acceptable under load (no significant regression)
- [ ] Code formatting passes `./gradlew spotlessCheck`
- [ ] No new detekt issues: `./gradlew detekt`
- [ ] Build passes: `./gradlew build`

## Testing Checklist

- [ ] Local unit tests pass: `./gradlew test`
- [ ] Cucumber feature suite passes (existing + new scenarios): `./gradlew test`
- [ ] Multi-node integration tests (Docker Compose):
  - [ ] 3 actor nodes + 2 HTTP nodes
  - [ ] Test all scenarios in task 4
  - [ ] Verify metrics/logs show correct actor distribution
- [ ] Chaos engineering tests (optional but recommended):
  - [ ] Randomly kill actor nodes during load test
  - [ ] Verify no dropped requests or data loss
- [ ] Manual verification:
  - [ ] Start 2 actor nodes; verify cluster sees both
  - [ ] Make HTTP request; verify it routes to one of the nodes
  - [ ] Kill that node; verify next request routes to other node
  - [ ] Restart node; verify it re-joins cluster

## Code Quality Checklist

- [ ] Spotless formatting applied: `./gradlew spotlessApply`
- [ ] No detekt issues: `./gradlew detekt` passes
- [ ] Replication strategy and failover behavior documented in code
- [ ] Cluster event handling well-commented
- [ ] No unused imports or variables
- [ ] Configuration options (caching, rebalancing) documented in `koin.properties`

## Deliverable

Merge PR with actor replication and distribution. Application is now ready for production multi-node deployments with automatic failover and load distribution.

## Related Issues/PRs

- Implements Phase 3 of: [Cluster Architecture Migration](../docs/CLUSTER_MIGRATION_APPROACH.md)
- Depends on: Phase 2 (HTTP-to-Actor Message Routing)
- Prerequisite for: Phase 4 (Containerization & Kubernetes Readiness)

## Definition of Done

- [ ] All acceptance criteria met
- [ ] All tests passing locally and in CI
- [ ] Code review approved
- [ ] Architecture decisions documented (ADR)
- [ ] PR merged to main
- [ ] Deployment tested (at least locally with Docker Compose)

## Open Design Questions

These should be resolved during implementation and documented:

1. **Replication Model**: Active-active vs. master-slave? Trade-offs?
2. **Failover Timing**: How long does HTTP node wait before routing to replica?
3. **State Recovery**: How is actor state recovered if a node crashes?
4. **Caching Strategy**: Should HTTP nodes cache actor locations? TTL?
5. **Rebalancing**: When new actor node joins, do we rebalance existing actors or wait?
6. **Consistency**: What is the consistency model (strong, eventual, causal)?

## Notes

- Consider implementing circuit breakers in HTTP nodes for failing actor nodes
- Monitor latency impact of cluster discovery and actor routing
- Consider add observability/metrics in Phase 5 for tracking actor distribution
