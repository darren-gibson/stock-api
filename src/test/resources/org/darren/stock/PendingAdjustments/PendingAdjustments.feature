@asciidoc @section-PendingAdjustments
Feature: Pending adjustments can be viewed and reconciled

  Background:
    Given it's 12:00 on 2024-12-31
    And the following locations exist:
      | Location Id | Parent Location Id | Role |
      | Cambridge   |                    | Shop |

  Scenario: The sale of a product in a location where the current stock is zero results in a pending adjustment
    Given the following are the current stock levels:
      | Location Id | Product | Stock Level |
      | Cambridge   | Beans   | 0           |
    When there is a sale of 1 can of "Beans" in the "Cambridge" store
    Then the stock levels should be updated as follows:
      | Location Id | Product | Stock Level | Pending Adjustment |
      | Cambridge   | Beans   | 0           | -1                 |

  Scenario: deliveries that occur before sales can resolve pending adjustments
  events can be received and processed out-of-order by the system and the stock levels will be updated accordingly
    Given the following are the current stock levels:
      | Location Id | Product | Stock Level |
      | Cambridge   | Beans   | 0           |
    When there is a sale of 1 can of "Beans" in the "Cambridge" store
    And a delivery of 10 cans of "Beans" to the "Cambridge" store that occurred at 11:00 on 2024-12-31
    Then the stock levels should be updated as follows:
      | Location Id | Product | Stock Level | Pending Adjustment |
      | Cambridge   | Beans   | 9           | 0                  |