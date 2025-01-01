@asciidoc @section-PendingAdjustments
Feature: Pending adjustments can be viewed and reconciled

  Background:
    Given the following locations exist:
      | Location Id | Parent Location Id | Role |
      | Cambridge   |                    | Shop |

  Scenario: The sale of a product in a location where the current stock is zero results in a pending adjustment, even if there is a delivery of more stock following the sale.
    Given the following are the current stock levels:
      | Location Id | Product | Stock Level |
      | Cambridge   | Beans   | 0           |
    When there is a sale of 1 can of "Beans" in the "Cambridge" store
    And there is a delivery of 10 cans of "Beans" to "Cambridge"
    Then the stock levels should be updated as follows:
      | Location Id | Product | Stock Level | Pending Adjustment |
      | Cambridge   | Beans   | 10           | -1                 |

  Scenario: deliveries that occur before sales can resolve pending adjustments
  events can be received and processed out-of-order by the system and the stock levels will be updated accordingly
    Given it's 12:00 on 2024-12-31
    And the following are the current stock levels:
      | Location Id | Product | Stock Level |
      | Cambridge   | Beans   | 0           |
    When there is a sale of 1 can of "Beans" in the "Cambridge" store
    And a delivery of 10 cans of "Beans" to the "Cambridge" store that occurred at 11:00 on 2024-12-31
    Then the stock levels should be updated as follows:
      | Location Id | Product | Stock Level | Pending Adjustment |
      | Cambridge   | Beans   | 9           | 0                  |

  Scenario: deliveries that occur before sales only resolve applicable pending adjustments
  events can be received and processed out-of-order by the system and the stock levels will be updated accordingly.  If a delivery that occurred before a sale is received by the system after the sale, then only those pending adjustments will be resolved.  Any pending adjustments that are not resolved by the delivery will remain.
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