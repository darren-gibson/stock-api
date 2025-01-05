@section-Sales @asciidoc @order-3
Feature: Sale: Exception flows
  Sales must be for know products in know locations, attempting to call the Sale Endpoint with a location or product that is not know to the system will result in an error.

  Sales are only permitted from `Tracked` locations.

  Scenario Outline: Sales can only happen at a location that's a shop
    Given "<location>" is a "<type>" location
    And the stock level of "Beans" in "<location>" is 20
    Then the sale of "Beans" in "<location>" will result in a HTTP Status of <status> and error "<code>"

    Examples:
      | location | type                     | status | code                 |
      | store 1  | Untracked                | 400    | LocationNotTracked |
      | store 2  | TrackedInventoryLocation | 201    |                      |