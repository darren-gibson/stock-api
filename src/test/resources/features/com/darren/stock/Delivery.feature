Feature: Deliveries of Stock to a location

  Background: A store has multiple locations that a product can be held
    Given Cambridge is a store
    Given Waterside is a store

  Scenario: Simple deliveries to a location increment the Stock on hand
    Given the stock level of Beans in Cambridge is 0
    When there is a delivery of 20 Beans to Cambridge store
    Then the current stock level of Beans in Cambridge will equal 20

  Scenario: Multiple deliveries to a location increment the Stock on hand
    Given the stock level of Beans in Cambridge is 0
    When there is a delivery of 20 Beans to Cambridge store
    When there is a delivery of 50 Beans to Cambridge store
    Then the current stock level of Beans in Cambridge will equal 70


  Scenario: maintain stock pots for the same product in different locations
    Given the stock level of Beans in Cambridge is 0
    And the stock level of Beans in Waterside is 0
    When there is a delivery of 20 Beans to Cambridge store
    And there is a delivery of 50 Beans to Waterside store
    And there is a delivery of 10 Beans to Cambridge store
    Then the current stock level of Beans in Cambridge will equal 30
    And the current stock level of Beans in Waterside will equal 50

  Scenario: maintain stock pots for different products in the same location
    Given the stock level of Beans in Cambridge is 0
    And the stock level of Eggs in Cambridge is 0
    When there is a delivery of 20 Beans to Cambridge store
    And there is a delivery of 10 Eggs to Cambridge store
    Then the current stock level of Beans in Cambridge will equal 20
    And the current stock level of Eggs in Cambridge will equal 10