# Cluster Architecture Migration: Implementation Approach

## Overview

Migrate from a monolithic single-node actor system to a distributed cluster model using Actor4k cluster support. This change enables horizontal scaling and resilience by separating Ktor HTTP server nodes from actor-hosting nodes. The implementation maintains a single codebase with deployment configuration determining role assignment (HTTP server, actor node, or both).

**Key Benefits:**
- Horizontal scalability for actor workload
- Independent scaling of HTTP tier and computation tier
- Foundation for Kubernetes deployment
- Improved fault isolation and resilience

---

## Architecture Changes

### Current State
- Single application node running both Ktor HTTP server and `StockSystem` actors
- All state managed in-process via actor mailboxes
- No inter-node communication

### Future State
- **HTTP Nodes**: Ktor server instances that handle incoming requests, delegate work to actor nodes via Actor4k cluster messaging
- **Actor Nodes**: Host `StockSystem` actors (StockStateManager, etc.) and process messages from HTTP nodes or other actor nodes
- **Hybrid Nodes** (optional): Can run both HTTP server and host actors for small deployments
- **Cluster Coordination**: Actor4k cluster framework manages node discovery and message routing
- **State Sharing**: Actor state remains distributed across cluster nodes; each actor instance lives on one node

### Configuration-Driven Roles
Nodes run identical software but are configured at startup via environment variables or config files:
```yaml
node:
  role: "http"        # Options: "http", "actor", or "both" (default: "both")
  cluster:
    enabled: true
    seedNodes: ["actor-node-1:5000", "actor-node-2:5000"]
    port: 5000        # Actor4k cluster port
```

---

## Implementation Phases

### Phase 1: Foundation & Configuration Framework
**Goal**: Establish deployment configuration and introduce cluster support concepts without breaking current operation.

**Tasks:**
1. Add deployment configuration layer
   - Create `src/main/kotlin/org/darren/stock/deployment/NodeConfig.kt`
   - Define `NodeRole` enum (HTTP, ACTOR, BOTH)
   - Read from `koin.properties` and environment variables
   - Add unit tests for configuration loading

2. Add Actor4k cluster dependency
   - Update `build.gradle.kts` with Actor4k cluster support
   - Document Actor4k version and compatibility with current actor setup
   - Verify existing actor code is Actor4k-compatible

3. Introduce cluster bootstrap in Koin
   - Create `src/main/kotlin/org/darren/stock/config/ClusterModule.kt`
   - Conditionally enable cluster based on `NodeRole`
   - Keep single-node mode as fallback (role = BOTH for backward compatibility)

4. Update `Application.kt` to respect node role
   - HTTP server starts only if role includes HTTP
   - Actor system starts only if role includes ACTOR
   - Add startup logs indicating node role and cluster status

**Acceptance Criteria:**
- Application starts with `nodeRole=both` and behaves identically to current version
- Configuration can be overridden via environment (e.g., `NODE_ROLE=http`)
- Tests confirm configuration loads correctly for all three roles
- No changes to existing StockSystem actor implementation required yet
- Existing feature tests pass without modification

**Deliverable:**
- Merge PR with configuration framework; application is backward compatible

---

### Phase 2: HTTP-to-Actor Message Routing
**Goal**: Enable HTTP nodes to send requests to actor nodes without requiring actors to live on the HTTP node.

**Tasks:**
1. Refactor `StockService` to use remote actors
   - Create abstraction layer: `RemoteStockService` interface
   - Implement local variant for single-node (role = BOTH)
   - Implement remote variant using Actor4k cluster messaging for HTTP nodes
   - HTTP node creates "actor client" references to actor node services

2. Update routing to use abstraction
   - Modify existing endpoints in `GetStock.kt`, `CreateStock.kt`, etc.
   - Inject `StockService` (polymorphic); routing code remains unchanged
   - Remote calls return futures/deferred responses (compatible with Ktor async)

3. Test remote messaging
   - Add integration tests: two separate Ktor processes (HTTP + ACTOR nodes) communicate
   - Verify request/response round-trip with sample data
   - Confirm error handling (e.g., actor node unavailable → HTTP 503)

4. Update Cucumber features (if needed)
   - Confirm existing scenarios pass with HTTP and actor on separate nodes
   - Add new scenario: "HTTP node routes request to actor node and returns result"

**Acceptance Criteria:**
- HTTP node role can receive requests and successfully delegate to actor node
- All existing HTTP status codes and error responses preserved
- Response time acceptable (no significant latency regression from cluster messaging)
- Existing feature tests pass with mixed-node setup
- Integration test demonstrates two-node communication

**Deliverable:**
- Merge PR with remote actor communication; application supports separate HTTP and actor nodes

---

### Phase 3: Actor Replication & Load Distribution
**Goal**: Enable multiple actor nodes to share actor state and handle increased load.

**Tasks:**
1. Design actor replication strategy
   - Decide: Master-slave per actor instance or full replication?
   - Document failover behavior if actor node goes down
   - Plan: Does HTTP node cache actor references or query cluster on each request?

2. Implement actor distribution logic
   - Extend Koin to create actor pools on actor nodes
   - Add actor placement strategy (hash-based, round-robin, etc.)
   - HTTP nodes query cluster to find actor by location ID

3. Handle actor migration/re-balance
   - Document behavior when actor node joins/leaves cluster
   - Add cluster event listener (Actor4k provides this)
   - Consider: should we re-balance existing actors or wait for new requests?

4. Add resilience tests
   - Test: Stop one actor node; verify requests still succeed (failover to replica)
   - Test: Add new actor node; verify new requests can be routed to it
   - Test: HTTP node detects actor node unavailability and returns appropriate error

5. Update feature coverage
   - Add Cucumber scenarios for multi-node actor scenarios
   - Test location tracking across actor node boundaries

**Acceptance Criteria:**
- Multiple actor nodes can host the same actor system concurrently
- HTTP nodes successfully route requests to any actor node
- If one actor node fails, requests route to replica on another node
- Cluster automatically detects node additions/removals
- No data loss during actor migration (if applicable)
- Existing tests pass; new tests cover failover scenarios

**Deliverable:**
- Merge PR with actor replication and distribution

---

### Phase 4: Containerization & Kubernetes Readiness
**Goal**: Prepare application for container deployment and Kubernetes orchestration.

**Tasks:**
1. Create multi-role Dockerfile
   - Single Dockerfile supports all node roles
   - Entrypoint uses `NODE_ROLE` environment variable to determine behavior
   - Minimal image size; consider multi-stage build

2. Define Kubernetes manifests (example templates)
   - `k8s/http-deployment.yaml`: HTTP server pods
   - `k8s/actor-deployment.yaml`: Actor node pods
   - `k8s/service.yaml`: Service definitions for inter-pod communication
   - Document how pods discover each other (cluster seed nodes via headless service)

3. Add health checks
   - Implement `/health` endpoint (Ktor built-in or custom)
   - Add readiness check: actor nodes confirm cluster connectivity
   - Add liveness check: ensure actor system is responsive

4. Document deployment scenarios
   - Single-node dev (role = BOTH)
   - Multi-node staging (separate HTTP and actor)
   - Multi-node production with Kubernetes
   - Configuration examples for each scenario

5. Add deployment tests (optional, phase 4+)
   - Docker image builds successfully for each role
   - Container starts and responds to health checks
   - Multi-container compose test (http + actor) communicates correctly

**Acceptance Criteria:**
- Docker image builds and runs for all node roles
- Kubernetes manifests deploy successfully (local minikube test)
- Pods communicate via cluster discovery
- Health checks report correct status
- Deployment documentation is clear and tested

**Deliverable:**
- Merge PR with Dockerfile and Kubernetes templates

---

### Phase 5: Observability & Operational Readiness
**Goal**: Add monitoring, logging, and tracing for distributed system.

**Tasks:**
1. Enhance logging for cluster visibility
   - Log node role, cluster membership, actor location routing
   - Include trace IDs in cluster messages for end-to-end request tracing
   - Use `kotlin-logging` with structured fields

2. Add cluster metrics
   - Track: actor messages sent/received, actor node count, routing latency
   - Expose via existing metrics system (if applicable)
   - Document key metrics for alerting

3. Implement distributed tracing (optional, phase 5+)
   - Propagate trace context across cluster messages
   - Integrate with existing logging framework

4. Create operational runbook
   - How to scale HTTP tier (add/remove HTTP nodes)
   - How to scale actor tier (add/remove actor nodes)
   - How to handle actor node failure
   - Troubleshooting guide for cluster communication issues

**Acceptance Criteria:**
- Logs clearly indicate which node and which actor processes a request
- Metrics available for monitoring cluster health
- Runbook tested by team member unfamiliar with system

**Deliverable:**
- Merge PR with observability enhancements

---

## Technical Decisions & Notes

### Actor4k Cluster Configuration
- **Seed Nodes**: HTTP nodes need to know actor node addresses. Document seed node configuration (environment variable or config file).
- **Port Assignment**: Cluster messaging port separate from HTTP port (e.g., 5000 for Actor4k, 8080 for HTTP).
- **Serialization**: Ensure actor messages are serializable across the network (already using `kotlinx.serialization`).

### Backward Compatibility
- Single-node deployments (role = BOTH) continue to work without Actor4k cluster configuration.
- Existing code changes are minimal for the first three phases.
- API contracts unchanged; only internal routing changes.

### Testing Strategy
- Phase 1-2: Existing feature tests pass unchanged (black-box from test perspective).
- Phase 3-5: Add new scenarios specifically for multi-node behavior.
- Local integration tests with Docker Compose or test containers before Kubernetes.

### Risks & Mitigations
| Risk | Mitigation |
|------|-----------|
| Actor4k cluster complexity | Start with seed-node discovery; use managed cluster only if needed later |
| Network latency between nodes | Implement timeouts, circuit breakers; monitor metrics |
| State inconsistency during rebalancing | Document rebalancing strategy early; decide: migrate or re-create |
| Kubernetes networking challenges | Test locally with Docker Compose first; use headless services for discovery |

---

## Success Criteria (Overall)

✅ Application supports single-node (backward compatible) and multi-node deployments  
✅ HTTP and actor workloads can be scaled independently  
✅ Cluster is resilient to node failures (tested with failover scenarios)  
✅ Deployment configuration is clear and environment-driven  
✅ Documentation and runbook allow operators to deploy to Kubernetes  
✅ Existing tests pass; new tests cover distributed scenarios  

---

## Questions for Design Review

1. **Actor Replication**: Do we need active-active replication or master-slave per actor?
2. **Failover Behavior**: If an actor node dies, do we immediately create a new actor instance on another node, or let HTTP nodes retry (eventual consistency)?
3. **State Persistence**: Should actor state be persisted (e.g., to a database) to survive cluster rebalancing, or is in-memory with replication sufficient?
4. **Seed Node Management**: In Kubernetes, how should nodes discover each other? (StatefulSet with headless service vs. service discovery API)
5. **HTTP Node Caching**: Should HTTP nodes cache actor references, or query the cluster on each request?
6. **Backward Compatibility Timeline**: How long should we support role = BOTH mode? (indefinitely or deprecate after N releases?)

---

## References

- [Actor4k Documentation](https://actor4k.io) (placeholder; replace with actual docs)
- [Ktor Distributed Systems Guide](https://ktor.io)
- [Kubernetes Pod-to-Pod Communication](https://kubernetes.io/docs/concepts/cluster-administration/networking/)
- Current project docs: `docs/CONCURRENCY.md`
