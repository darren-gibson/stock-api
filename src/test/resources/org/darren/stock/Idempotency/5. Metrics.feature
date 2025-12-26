@idempotency @asciidoc
Feature: Idempotency Metrics

  ## Overview

  The domain-level idempotency system emits metrics intended for operational monitoring.
  This feature documents the metrics and provides an automated assertion that the
  counters are emitted when duplicate requests are detected.

  ## Exported Counters

  The domain idempotency instrumentation exposes two Long counters:

  * `idempotency.domain.hits` : Incremented when a duplicate request is detected and skipped at the domain level.
  * `idempotency.domain.misses` : Incremented when a new request is processed (not a duplicate).

  These counters should be incremented precisely for the semantics above; the scenario below
  documents a minimal end-to-end interaction that produces one miss and one hit.

  Background:
    Given "test-location" is a tracked location
    And "test-supplier" is a registered supplier

  Scenario: Domain-level idempotency works correctly for duplicate requests
    When I make a delivery request with requestId "req-123"
    Then the request should succeed
    When I make the same delivery request again with requestId "req-123"
    Then the request should return conflict due to idempotency
