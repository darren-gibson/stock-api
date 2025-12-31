Feature: State Snapshotting for Faster Startup

  Overview: The stock system maintains a persistent snapshot of its current state to enable faster startup and actor initialization with reduced processing overhead.

  Background:
    Given the stock system is configured with snapshotting enabled
    And "Warehouse A" is a receiving location
    And "Store B" is a receiving location

  Scenario: Startup with existing snapshot

    Given a snapshot exists with the following stock levels from a previous session:
      | location    | product    | quantity | pendingAdjustment | lastRequestId |
      | Warehouse A | Blue Jeans | 150.0    | 0.0               | delivery-200  |
      | Store B     | Red Socks  | 75.0     | 0.0               | sale-150      |
    When the system starts up
    Then the stock state is loaded from the snapshot instead of being rebuilt from initial data
    And all actors are initialized with via the snapshot
    And the current stock level should equal:
      | Location Id | Product    | Stock Level | Pending Adjustment |
      | Warehouse A | Blue Jeans | 150.0       | 0.0                |
      | Store B     | Red Socks  | 75.0        | 0.0                |
    And the startup process bypasses the expensive initial data rebuilding phase

  Scenario: Startup without snapshot
    Given no state snapshot exists
    And the initial configuration contains the following stock data:
      | location    | product    | quantity | pendingAdjustment |
      | Warehouse A | Blue Jeans | 0.0      | 0.0               |
      | Store B     | Red Socks  | 0.0      | 0.0               |
    When the system starts up
    And all actors are initialized with the initial stock data:
      | Location Id | Product    | Stock Level | Pending Adjustment |
      | Warehouse A | Blue Jeans | 0.0         | 0.0                |
      | Store B     | Red Socks  | 0.0         | 0.0                |

  Scenario: Periodic snapshot saving during operations
    Given the system is running with snapshotting enabled and configured to snapshot every 5 events
    And an initial snapshot exists with:
      | location    | product    | quantity | pendingAdjustment | lastRequestId |
      | Warehouse A | Blue Jeans | 100.0    | 0.0               | initial-100   |
    When the following stock operations occur:
      | location    | product    | eventType | quantity | requestId   |
      | Warehouse A | Blue Jeans | delivery  | 50.0     | request-101 |
      | Warehouse A | Blue Jeans | sale      | 20.0     | request-102 |
      | Warehouse A | Blue Jeans | delivery  | 30.0     | request-103 |
      | Warehouse A | Blue Jeans | sale      | 10.0     | request-104 |
      | Warehouse A | Blue Jeans | delivery  | 25.0     | request-105 |
      | Warehouse A | Blue Jeans | sale      | 5.0      | request-106 |
      | Warehouse A | Blue Jeans | delivery  | 15.0     | request-107 |
      | Warehouse A | Blue Jeans | sale      | 8.0      | request-108 |
      | Warehouse A | Blue Jeans | delivery  | 12.0     | request-109 |
      | Warehouse A | Blue Jeans | sale      | 3.0      | request-110 |
      | Warehouse A | Blue Jeans | sale      | 1.0      | request-111 |
    Then the following new snapshots are created during operation:
      | Location Id | Product    | Stock Level | Pending Adjustment | lastRequestId |
      | Warehouse A | Blue Jeans | 175.0       | 0.0                | request-105-0 |
      | Warehouse A | Blue Jeans | 186.0       | 0.0                | request-110   |
    And the current stock level should equal:
      | Location Id | Product    | Stock Level | Pending Adjustment |
      | Warehouse A | Blue Jeans | 185.0       | 0.0                |

  Scenario: Complete state persistence including pending adjustments
    Given a valid state snapshot exists from a previous run with the following stock data including pending adjustments:
      | location    | product    | quantity | pendingAdjustment | lastRequestId |
      | Warehouse A | Blue Jeans | 200.0    | 25.0              | delivery-300  |
      | Store B     | Red Socks  | 50.0     | -10.0             | adjustment-75 |
    When the system starts up
    Then the stock state including pending adjustments is loaded from the snapshot
    And all actors are initialized with the complete snapshot stock data including pending adjustments
    And the current stock level should equal:
      | Location Id | Product    | Stock Level | Pending Adjustment |
      | Warehouse A | Blue Jeans | 200.0       | 25.0               |
      | Store B     | Red Socks  | 50.0        | -10.0              |

  Scenario: Events recorded after snapshot are replayed on startup
    Given a snapshot exists with the following stock levels from a previous session:
      | location    | product   | quantity | pendingAdjustment | lastRequestId |
      | Warehouse A | Red Socks | 100.0    | 0.0               | delivery-100  |
    And the following historic events led to the snapshot state:
      | location    | product   | eventType | quantity | requestId    | contentHash     |
      | Warehouse A | Red Socks | delivery  | 50.0     | delivery-98  | historic-hash-1 |
      | Warehouse A | Red Socks | sale      | 20.0     | sale-99      | historic-hash-2 |
      | Warehouse A | Red Socks | delivery  | 70.0     | delivery-100 | historic-hash-3 |
    And the following stock events occurred after the snapshot was taken:
      | location    | product   | eventType | quantity | requestId    | contentHash               |
      | Warehouse A | Red Socks | sale      | 10.0     | sale-101     | test-hash-after-snapshot  |
      | Warehouse A | Red Socks | delivery  | 5.0      | delivery-102 | test-hash-after-snapshot2 |
    When the system starts up
    Then the current stock level should equal:
      | Location Id | Product   | Stock Level | Pending Adjustment |
      | Warehouse A | Red Socks | 95.0        | 0.0                |

