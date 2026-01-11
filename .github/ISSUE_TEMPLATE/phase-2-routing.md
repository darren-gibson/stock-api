---
name: "Phase 2: HTTP-to-Actor Message Routing"
about: "Enable HTTP nodes to delegate requests to remote actor nodes"
title: "[Phase 2] HTTP-to-Actor Message Routing"
labels: ["architecture", "cluster-migration", "phase-2"]
---

# Phase 2: HTTP-to-Actor Message Routing

## Overview
Enable HTTP nodes to send requests to remote actor nodes without requiring actors to live on the HTTP node. This phase creates the abstraction layer for polymorphic stock service implementation supporting both local and remote actor communication.

**Recommended Feature Branch:** `feat/phase-2-remote-actor-routing`

**Depends On:** Phase 1 (Foundation & Configuration Framework)

## Goals
- Create abstraction layer for stock service
- Implement remote actor communication via Actor4k cluster
- Enable HTTP nodes to route requests to actor nodes
- Maintain all existing HTTP status codes and error responses
- Acceptable latency from cluster messaging

## Tasks

### 1. Refactor StockService to Support Remote Actors
- [ ] Create `src/main/kotlin/org/darren/stock/service/RemoteStockService.kt` interface
  - Extract interface from current `StockService` (if needed) or create new facade
  - Define contract for operations: read stock, create stock, update stock, delete stock, move stock, etc.
- [ ] Create local implementation: `src/main/kotlin/org/darren/stock/service/LocalStockService.kt`
  - Wrapper around existing `StockSystem` actors (current behavior)
  - Used when role includes ACTOR
- [ ] Create remote implementation: `src/main/kotlin/org/darren/stock/service/RemoteStockActorService.kt`
  - Uses Actor4k cluster messaging to communicate with actor nodes
  - HTTP node creates actor client references to remote actors
  - Handles serialization/deserialization of requests and responses
  - Used when role = HTTP (no local actors)
- [ ] Create Koin factory to select implementation based on node role
  - If role includes ACTOR: inject `LocalStockService`
  - If role = HTTP only: inject `RemoteStockActorService`
  - Add to `src/main/kotlin/org/darren/stock/config/ClusterModule.kt` or new module

### 2. Update HTTP Routing to Use Abstraction
- [ ] Review all endpoint files in `src/main/kotlin/org/darren/stock/ktor/`
  - Document current dependencies on `StockService`
  - Identify all endpoints that use stock service
- [ ] Modify routing code to inject `RemoteStockService` (by interface)
  - Example files: `GetStock.kt`, `CreateStock.kt`, `UpdateStock.kt`, `DeleteStock.kt`, `MoveStock.kt`
  - Routing code remains unchanged (no conditional logic)
  - Service abstraction handles local vs. remote dispatch
- [ ] Ensure remote calls return futures/deferred responses
  - Compatible with Ktor's async request handling
  - No blocking calls on HTTP thread

### 3. Test Remote Messaging
- [ ] Add unit tests: `src/test/kotlin/org/darren/stock/service/RemoteStockActorServiceTest.kt`
  - Mock Actor4k cluster communication
  - Test request/response serialization
  - Test error handling (e.g., remote actor unavailable)
- [ ] Add integration tests: `src/test/kotlin/org/darren/stock/steps/RemoteActorIntegrationTest.kt`
  - Start two separate application instances (HTTP node + ACTOR node)
  - HTTP node sends request; ACTOR node processes
  - Verify response round-trip with sample data
  - Test error scenarios: actor node unavailable → HTTP 503
- [ ] Test timeouts and circuit breakers
  - Unresponsive actor node behavior
  - Network delays

### 4. Update Cucumber Features
- [ ] Run existing feature tests: `./gradlew test` (via `RunSuiteTests`)
  - Confirm all existing scenarios pass with mixed HTTP/ACTOR node setup
  - If failures, update step definitions to work with remote service
- [ ] Add new feature file: `src/test/resources/org/darren/stock/2. Remote Actor Routing.feature`
  - Scenario: "HTTP node routes request to actor node and returns result"
  - Use existing step definitions where possible
  - Document background setup (two-node test environment)

## Acceptance Criteria

- [ ] HTTP node role can receive requests and successfully delegate to actor node
- [ ] All existing HTTP status codes and error responses preserved
- [ ] Response time acceptable (document baseline; no >50ms regression from cluster messaging)
- [ ] Remote service properly handles timeouts and unavailable nodes
- [ ] All existing feature tests pass with mixed-node setup
- [ ] Integration test demonstrates two-node communication (new scenario)
- [ ] Local vs. remote implementation transparent to routing code
- [ ] Code formatting passes `./gradlew spotlessCheck`
- [ ] No new detekt issues: `./gradlew detekt`
- [ ] Build passes: `./gradlew build`

## Testing Checklist

- [ ] Local unit tests pass: `./gradlew test`
- [ ] Integration tests with Docker Compose or test containers:
  - [ ] HTTP node (NODE_ROLE=http) + ACTOR node (NODE_ROLE=actor)
  - [ ] Test all CRUD operations across nodes
  - [ ] Test error scenarios (actor node down)
- [ ] Cucumber feature suite passes: existing + new scenarios
- [ ] Load test (basic): 10+ concurrent requests routed through cluster
- [ ] Manual verification:
  - [ ] Single-node (role=BOTH) behaves identically to Phase 1
  - [ ] HTTP-only node rejects actor operations (if applicable) or routes cleanly
  - [ ] ACTOR-only node rejects HTTP operations (if applicable)

## Code Quality Checklist

- [ ] Spotless formatting applied: `./gradlew spotlessApply`
- [ ] No detekt issues: `./gradlew detekt` passes
- [ ] New interfaces and implementations well-documented
- [ ] No unused imports or variables
- [ ] Error messages are clear and actionable

## Deliverable

Merge PR with remote actor communication. Application supports separate HTTP and actor nodes. All existing deployments continue to work (backward compatible).

## Related Issues/PRs

- Implements Phase 2 of: [Cluster Architecture Migration](../docs/CLUSTER_MIGRATION_APPROACH.md)
- Depends on: Phase 1 (Foundation & Configuration Framework)
- Prerequisite for: Phase 3 (Actor Replication & Load Distribution)

## Definition of Done

- [ ] All acceptance criteria met
- [ ] All tests passing locally and in CI
- [ ] Code review approved
- [ ] PR merged to main
- [ ] New feature documented (if needed)

## Notes

- Remote service implementation may differ based on Actor4k API; adjust design if needed during implementation
- Document cluster message format and serialization strategy
- Consider adding distributed tracing context for debugging (can be deferred to Phase 5)
