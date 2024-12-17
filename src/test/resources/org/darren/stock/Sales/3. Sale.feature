@section-Sales @asciidoc @order-2
Feature: Sale: Happy path scenarios
  Products are sold from locations, when they are sold then the stock on hand of that product is decremented by the quantity sold.  Products can only be sold from a `tracked` location.

  Scenario: Sale by each
    Products sold by unit (known as sell by each) from a Location decrements the Stock on hand by the quantity sold.
    Given "Cambridge" is a store
    And the stock level of "Beans" in "Cambridge" is 20
    When there is a sale of 5 "Beans" in the "Cambridge" store
    Then the current stock level of "Beans" in "Cambridge" will equal 15

  Scenario: Sale by weight
    Some products are sold by weight (e.g. KG) and not "by each", the system allows decimal values of quantity to be sold and decrements the stock file accordingly
    Given "Cambridge" is a store
    And the stock level of "Potatoes" in "Cambridge" is 10
    When there is a sale of 1.25 "Potatoes" in the "Cambridge" store
    Then the current stock level of "Potatoes" in "Cambridge" will equal 8.75
