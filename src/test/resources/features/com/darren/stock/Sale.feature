Feature: Selling a Product form a location decrements the stock from that location

  Scenario: Simple sale from a location decrements the Stock on hand
    Given the stock level of Beans in Cambridge store is 20
    When there is a sale of 5 Beans in the Cambridge store
    Then the current stock level of Beans in Cambridge store will equal 15