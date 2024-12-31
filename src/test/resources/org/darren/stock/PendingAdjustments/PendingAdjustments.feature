@asciidoc @section-PendingAdjustments
  Feature: Pending adjustments can be viewed and reconciled

    Background:
      Given the following locations exist:
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
