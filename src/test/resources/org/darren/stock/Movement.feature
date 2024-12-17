Feature: Feature: Stock Movement Transaction
  Stock Movement refers to the transfer of inventory from one location to another,
  typically recorded in an inventory management system. When movement occurs between tracked
  locations, it is fully documented, ensuring that stock levels and locations are updated accurately in real time.

  Definition:
  The transfer of stock from one physical or logical location to another, where both locations are tracked
  within the system. Every movement is logged with details such as:
  - Quantity moved
  - Source and destination
  - Date and time
  - Reason for the transfer (e.g., replenishment, redistribution, return)

  Purpose:
  - To maintain visibility and control over inventory.
  - To ensure accurate stock levels in both source and destination locations.
  = To support planning and operational efficiency.

  As a system user
  I want the System to correctly perform stock movement transactions between tracked locations
  So that inventory levels are accurately updated in both source and destination

  Scenario: Successful stock movement from a Distribution Centre (DC) to a Store
    Given a Distribution Centre "DC1" with 1000 units of "Product A"
    And a Store "Store1" with 200 units of "Product A"
    When I initiate a stock movement transaction with the following details:
      | source | destination | product   | quantity | reason        |
      | DC1    | Store1      | Product A | 100      | Replenishment |
    Then the stock levels should be updated as follows:
      | location | product   | quantity |
      | DC1      | Product A | 900      |
      | Store1   | Product A | 300      |

  Scenario: Stock movement fails due to insufficient stock at the source
    Given a Distribution Centre "DC1" with 50 units of "Product B"
    And a Store "Store2" with 10 units of "Product B"
    When I initiate a stock movement transaction with the following details:
      | source | destination | product   | quantity | reason        |
      | DC1    | Store2      | Product B | 100      | Replenishment |
    Then the move request should fail with an InsufficientStock exception
    And the stock levels should remain unchanged:
      | location | product   | quantity |
      | DC1      | Product B | 50       |
      | Store2   | Product B | 10       |