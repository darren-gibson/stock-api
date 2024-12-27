@section-Delivery @asciidoc @order-2
Feature: Delivery: Happy path scenarios
  Deliveries are recorded when products are delivered to a receiving location within a warehouse. Recording a delivery updates the stock on hand for the products delivered and ensures inventory accuracy. Deliveries can include multiple products and fractional quantities when necessary.

  Scenario: Delivery of multiple products
  Deliveries can include multiple products, and each product's stock on hand is updated individually.
    Given "warehouse1-receiving" is a receiving location
    And the stock level of "Cereal" in "warehouse1-receiving" is 50
    And the stock level of "Milk" in "warehouse1-receiving" is 20
    When there is a delivery of 30 "Cereal" and 10 "Milk" to "warehouse1-receiving"
    Then the current stock level of "Cereal" in "warehouse1-receiving" will equal 80
    And the current stock level of "Milk" in "warehouse1-receiving" will equal 30

  Scenario: Delivery of fractional quantities
  Deliveries can include fractional quantities for products that are tracked by weight or volume.
    Given "warehouse1-receiving" is a receiving location
    And the stock level of "Olive Oil" in "warehouse1-receiving" is 15.5
    When there is a delivery of 2.25 "Olive Oil" to "warehouse1-receiving"
    Then the current stock level of "Olive Oil" in "warehouse1-receiving" will equal 17.75

  Scenario: Delivery updates empty stock
    If a product is out of stock, a delivery updates the stock level from zero.
    Given "warehouse1-receiving" is a receiving location
    And the stock level of "Sugar" in "warehouse1-receiving" is 0
    When there is a delivery of 20 "Sugar" to "warehouse1-receiving"
    Then the current stock level of "Sugar" in "warehouse1-receiving" will equal 20
