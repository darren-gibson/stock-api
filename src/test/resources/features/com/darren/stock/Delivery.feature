Feature: Deliveries of Stock to a location

  Scenario: Simple deliveries to a location increment the Stock on hand
    Given the stock level of Beans in Cambridge store is 0
    When there is a delivery of 20 Beans to Cambridge store
    Then the current stock level of Beans in Cambridge store will equal 20

  Scenario: Multiple deliveries to a location increment the Stock on hand
    Given the stock level of Beans in Cambridge store is 0
    When there is a delivery of 20 Beans to Cambridge store
    When there is a delivery of 50 Beans to Cambridge store
    Then the current stock level of Beans in Cambridge store will equal 70