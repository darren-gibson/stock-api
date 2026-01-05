@section-StockPot-Memory @asciidoc @order-4
Feature: StockPot memory management

  Overview: Inactive StockPots are unloaded from memory and rehydrated from persisted state when needed.
  
  StockPot unloading can be configured via `application.yaml` using:
  - `stockPot.unloadAfterInactivity`: Duration string (e.g., "5m", "100ms") for how long inactive StockPots remain in hot memory before being unloaded
  - `stockPot.unloadCheckInterval`: Duration string (e.g., "30s", "25ms") for how often to check for and unload inactive StockPots

  @manual-service-start
  Scenario: StockPot memory unloading can be configured via application settings
    Given the system is configured with the following StockPot unloading settings:
      | Setting                        | Value |
      | stockPot.unloadAfterInactivity | 100ms |
      | stockPot.unloadCheckInterval   | 25ms  |
    And the service is running
    And "Warehouse A" is a receiving location
    And "Red Socks" is a product
    And "Store A" is a receiving location
    When there is a delivery of 20.0 "Red Socks" to "Store A"
    Then the StockPot for "Red Socks" at "Store A" should remain in memory
    When I wait for 50 milliseconds
    Then the StockPot for "Red Socks" at "Store A" should still be in memory
    When I wait for 100 milliseconds
    Then the StockPot for "Red Socks" at "Store A" should eventually be removed from memory
    When there is a delivery of 10.0 "Red Socks" to "Store A"
    Then the current stock level of "Red Socks" in "Store A" will equal 30.0
