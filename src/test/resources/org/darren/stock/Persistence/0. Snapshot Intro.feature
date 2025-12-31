@section-Snapshot-Intro @asciidoc @order-0
Feature: Snapshot Intro - documented overview and external behaviour

  The Snapshot Intro feature documents the external behaviour and expectations of the
  actor state snapshotting subsystem. It is written for documentation generation via
  Cukedoctor (asciidoc sections embedded into the feature file) and also contains a
  compact set of scenarios that map to existing step definitions for automated checks.

  Snapshotting provides faster system startup, reduced event replay cost, and a
  persistent baseline for actor state. The examples below describe how snapshots
  are expected to behave from an operator and consumer perspective.

  === Rationale for Snapshotting

  Snapshotting exists to shorten actor initialization time by capturing a consistent
  representation of each actor's state. This reduces the need to replay long event
  streams at startup and improves system responsiveness after restarts.

  === Key Elements Captured in Snapshots

  * *Quantity*: the persisted quantity for the actor's product/location.
  * *Pending Adjustment*: any in-flight adjustments not yet applied to quantity.
  * *lastRequestId*: the id of the last processed request (used to replay only newer events).
  * *lastUpdated*: timestamp for when the snapshot was taken.

  === Operational Guarantees

  * Loads snapshots on startup when available and valid.
  * Replays only events that occurred after the snapshot's `lastRequestId`.
  * Supports configurable snapshot strategies (event-count, time-based, hybrid).

  Scenario: Snapshot presence speeds up startup
    Given the stock system is configured with snapshotting enabled
    And "Warehouse A" is a receiving location
    And "Store B" is a receiving location
    And a snapshot exists with the following stock levels from a previous session:
      | location    | product    | quantity | pendingAdjustment | lastRequestId |
      | Warehouse A | Blue Jeans | 150.0    | 0.0               | delivery-200  |
      | Store B     | Red Socks  | 75.0     | 0.0               | sale-150      |
    When the system starts up
    Then the stock state is loaded from the snapshot instead of being rebuilt from initial data
    And all actors are initialized with via the snapshot
    And the startup process bypasses the expensive initial data rebuilding phase
