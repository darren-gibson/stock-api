---
name: "Phase 1: Foundation & Configuration Framework"
about: "Establish deployment configuration and cluster bootstrap concepts"
title: "[Phase 1] Foundation & Configuration Framework"
labels: ["architecture", "cluster-migration", "phase-1"]
---

# Phase 1: Foundation & Configuration Framework

## Overview
Establish deployment configuration and introduce cluster support concepts without breaking current operation. This phase creates the foundation for subsequent phases by introducing a `NodeRole` configuration system and cluster bootstrap logic.

**Recommended Feature Branch:** `feat/phase-1-node-config-and-cluster-bootstrap`

## Goals
- Establish deployment configuration layer
- Introduce cluster support concepts
- Maintain backward compatibility
- All tests pass without modification

## Tasks

### 1. Add Deployment Configuration Layer
- [ ] Create `src/main/kotlin/org/darren/stock/deployment/NodeConfig.kt`
  - Define `NodeRole` enum: `HTTP`, `ACTOR`, `BOTH`
  - Create `ClusterNodeConfig` data class with node role and cluster settings
  - Implement configuration loading from `koin.properties` and environment variables
  - Support `NODE_ROLE` environment variable override
- [ ] Add unit tests in `src/test/kotlin/org/darren/stock/deployment/NodeConfigTest.kt`
  - Test all three role configurations
  - Test environment variable overrides
  - Test default fallback behavior

### 2. Add Actor4k Cluster Dependency
- [ ] Update `build.gradle.kts` with Actor4k cluster support
  - Document the Actor4k version and rationale
  - Add implementation and verification documentation
- [ ] Verify existing actor code is Actor4k-compatible
  - Review `StockSystem` actor implementation
  - Check message serialization (already using `kotlinx.serialization`)
  - Document any required changes (should be minimal)

### 3. Introduce Cluster Bootstrap in Koin
- [ ] Create `src/main/kotlin/org/darren/stock/config/ClusterModule.kt`
  - Create Koin module that conditionally sets up cluster based on `NodeRole`
  - Keep single-node mode as fallback (role = BOTH)
  - Provide seed node configuration from `koin.properties`
- [ ] Add unit tests in `src/test/kotlin/org/darren/stock/config/ClusterModuleTest.kt`
  - Test module creation for each role
  - Test cluster configuration loading

### 4. Update Application Startup
- [ ] Modify `src/main/kotlin/org/darren/stock/ktor/Application.kt`
  - Conditionally start HTTP server based on node role
  - Conditionally start actor system based on node role
  - Add startup logs indicating node role and cluster status
  - Format: `[Node: {role}, Cluster: {enabled/disabled}]`
- [ ] Update `src/main/resources/koin.properties`
  - Add default `node.role=both` property
  - Add cluster configuration properties
- [ ] Add integration test in `src/test/kotlin/org/darren/stock/ApplicationStartupTest.kt`
  - Test application starts with each role configuration
  - Test startup logs contain role information

## Acceptance Criteria

- [ ] Application starts with `nodeRole=both` and behaves identically to current version
- [ ] Configuration can be overridden via environment variable (e.g., `NODE_ROLE=http`)
- [ ] Tests confirm configuration loads correctly for all three roles (HTTP, ACTOR, BOTH)
- [ ] No changes to existing `StockSystem` actor implementation required
- [ ] All existing feature tests pass without modification
- [ ] Startup logs clearly indicate node role and cluster status
- [ ] Code formatting passes `./gradlew spotlessCheck`
- [ ] No new lint issues introduced (`./gradlew detekt`)
- [ ] Build passes: `./gradlew build`

## Testing Checklist

- [ ] Local unit tests pass: `./gradlew test`
- [ ] Run Cucumber feature suite: `./gradlew test` (via `RunSuiteTests`)
- [ ] Manual verification:
  - [ ] Start with `NODE_ROLE=both` → behaves as current version
  - [ ] Start with `NODE_ROLE=http` → HTTP server runs, no actors
  - [ ] Start with `NODE_ROLE=actor` → actors run, no HTTP server
  - [ ] Default (no env var) → role=BOTH is used

## Code Quality Checklist

- [ ] Spotless formatting applied: `./gradlew spotlessApply`
- [ ] No detekt issues: `./gradlew detekt` passes
- [ ] New code includes documentation/comments for non-obvious logic
- [ ] No unused imports or variables

## Deliverable

Merge PR with configuration framework. Application is fully backward compatible; existing deployments work without configuration changes.

## Related Issues/PRs

- Implements Phase 1 of: [Cluster Architecture Migration](../docs/CLUSTER_MIGRATION_APPROACH.md)
- Prerequisite for: Phase 2 (HTTP-to-Actor Message Routing)

## Definition of Done

- [ ] All acceptance criteria met
- [ ] All tests passing locally and in CI
- [ ] Code review approved
- [ ] PR merged to main
- [ ] Deployment prepared (if applicable)
