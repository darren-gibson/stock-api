Feature: Selling a Product form a location decrements the stock from that location
  Scenario: Simple sale from a location decrements the Stock on hand
    Given "Cambridge" is a store
    And the stock level of "Beans" in "Cambridge" is 20
    When there is a sale of 5 "Beans" in the "Cambridge" store
    Then the current stock level of "Beans" in "Cambridge" will equal 15

  Scenario Outline: Sales can only happen at a tracked location
    Given <location> is a <type> location
    And the stock level of "Beans" in "<location>" is 20
    Then the sale of "Beans" in "<location>" will result in "<result>"

    Examples:
      | location | type      | result  |
      | store 1  | Untracked | failure |
      | shelf    | Untracked | failure |
      | store 2  | Tracked   | success |