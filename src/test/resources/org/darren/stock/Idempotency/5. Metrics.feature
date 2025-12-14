@idempotency @asciidoc
Feature: Idempotency Metrics

  ## Overview

  The idempotency layer emits simple metrics intended for operational monitoring.
  This feature documents the metrics and provides an automated assertion that the
  counters are emitted when cache lookups occur.

  ## Exported Counters

  The idempotency instrumentation exposes two Long counters:

  * `idempotency.cache.hits` : Incremented when a previously-cached idempotent response is returned (cache hit).
  * `idempotency.cache.misses` : Incremented when a cache lookup does not find a stored response (cache miss).

  These counters should be incremented precisely for the semantics above; the scenario below
  documents a minimal end-to-end interaction that produces one miss and one hit.

  Background:
    Given an in-memory OpenTelemetry meter is configured
    And an in-memory idempotency store backed ResponseCacher is available

  Scenario: OpenTelemetry counters are emitted and recorded correctly for cache hits and misses
    When I request a missing cache key
    And I store a key and then request that key
    Then the exported metrics should include "idempotency.cache.hits" and "idempotency.cache.misses"
    And each counter should have value 1
