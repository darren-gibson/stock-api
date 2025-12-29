@section-Delivery @asciidoc @order-3
Feature: Delivery: Consistency in multi-product deliveries
  When recording a delivery with multiple products, the operation should be atomic: either all products are delivered successfully, or none are, to maintain inventory consistency.

  Scenario: Delivery with invalid quantity leaves state consistent
    Given "warehouse1-receiving" is a receiving location
    And the stock level of "Cereal" in "warehouse1-receiving" is 50
    And the stock level of "Milk" in "warehouse1-receiving" is 20
    When I attempt a delivery of 30 "Cereal" and -10 "Milk" to "warehouse1-receiving"
    Then the delivery should fail
    And the stock level of "Cereal" in "warehouse1-receiving" should remain 50
    And the stock level of "Milk" in "warehouse1-receiving" should remain 20

  Scenario: Delivery with transient repository failure eventually succeeds
    Given the repository will fail inserts for "FailingProduct" up to 2 times
    And "warehouse1-receiving" is a receiving location
    And the stock level of "Cereal" in "warehouse1-receiving" is 50
    And the stock level of "FailingProduct" in "warehouse1-receiving" is 20
    When I attempt a delivery of 30 "Cereal" and 10 "FailingProduct" to "warehouse1-receiving"
    Then the delivery should succeed
    And the stock level of "Cereal" in "warehouse1-receiving" should be 80
    And the stock level of "FailingProduct" in "warehouse1-receiving" should be 30
