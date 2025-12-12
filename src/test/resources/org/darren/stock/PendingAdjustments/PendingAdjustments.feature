@section-PendingAdjustments @asciidoc @order-1
Feature: Pending adjustments can be viewed and reconciled

  Background:
    Given the following tracked locations exist:
      | Location Id | Parent Location Id | Name | Roles                          |
      | Cambridge   |                    | Shop | Shop, TrackedInventoryLocation |

  Scenario: Sales with Zero Stock Trigger Pending Adjustment
    A sale made when stock is zero creates a pending adjustment, even if a delivery replenishes the stock afterward.
    Given the following are the current stock levels:
      | Location Id | Product | Stock Level |
      | Cambridge   | Beans   | 0           |
    When there is a sale of 1 can of "Beans" in the "Cambridge" store
    And there is a delivery of 10 cans of "Beans" to "Cambridge"
    Then the stock levels should be updated as follows:
      | Location Id | Product | Stock Level | Pending Adjustment |
      | Cambridge   | Beans   | 10           | -1                 |

  Scenario: Deliveries Before Sales Fix Pending Adjustments
      Deliveries made before sales can resolve pending adjustments. The system processes events out of order, ensuring stock levels are updated accordingly.
    Given it's 12:00 on 2024-12-31
    And the following are the current stock levels:
      | Location Id | Product | Stock Level |
      | Cambridge   | Beans   | 0           |
    When there is a sale of 1 can of "Beans" in the "Cambridge" store
    And a delivery of 10 cans of "Beans" to the "Cambridge" store that occurred at 11:00 on 2024-12-31
    Then the stock levels should be updated as follows:
      | Location Id | Product | Stock Level | Pending Adjustment |
      | Cambridge   | Beans   | 9           | 0                  |

  Scenario: Out-of-Order Deliveries Resolve Applicable Adjustments
    Deliveries made before sales only resolve the applicable pending adjustments. The system can process events out of order, and stock levels are updated accordingly. If the system receives a delivery that occurred before a sale but processes it after the sale, only the relevant pending adjustments will be resolved. Any unresolved adjustments will remain.
    Given it's 11:00 on 2024-12-31
    And the following are the current stock levels:
      | Location Id | Product | Stock Level |
      | Cambridge   | Beans   | 0           |
    When there is a sale of 5 cans of "Beans" in the "Cambridge" store
    Given it's 12:00 on 2024-12-31
    When there is a sale of 1 can of "Beans" in the "Cambridge" store
    And a delivery of 10 cans of "Beans" to the "Cambridge" store that occurred at 11:30 on 2024-12-31
    Then the stock levels should be updated as follows:
      | Location Id | Product | Stock Level | Pending Adjustment |
      | Cambridge   | Beans   | 9           | -5                 |