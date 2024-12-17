Feature: Record Product Delivery from a Distribution to a Store
  As a system user
  I want to record the delivery of a product from a distribution location to a store
  So that the system updates the stock levels at both locations accurately

#  Background:
#    Given the API is authenticated with a valid bearer token

  Scenario: Successfully record a delivery and update stock levels
    Given "Dist-001" is a Distribution Centre
    And "Store-001" is a store
    And a product "Product-123" exists in "Dist-001" with a stock level of 100
    And a product "Product-123" exists in "Store-001" with a stock level of 20
    When I send a POST request to "/locations/Dist-001/deliveries" with the following payload:
      """
      {
          "requestId": "delivery-001",
          "productId": "Product-123",
          "destinationLocationId": "Store-001",
          "quantity": 10
      }
      """
    Then the API should respond with status code 201
    And the response body should contain:
      """
      {
        "requestId": "delivery-001",
        "sourceLocationId": "Dist-001",
        "destinationLocationId": "Store-001",
        "productId": "Product-123",
        "quantityDelivered": 10.0,
        "deliveryTimestamp": "${json-unit.ignore}"
      }
      """
    And the stock level of product "Product-123" in "Store-001" should be updated to 30
    And the stock level of product "Product-123" in "Dist-001" should be updated to 90
#
#  Scenario: Handle duplicate delivery request using the same requestId
#    Given a distribution location "Dist-001" exists
#    And a store location "Store-001" exists
#    And a product "Product-123" exists with a stock level of 90 at "Dist-001"
#    And a product "Product-123" exists with a stock level of 30 at "Store-001"
#    And a delivery request with "requestId": "delivery-001" has already been processed
#    When I resend the POST request to "/locations/Dist-001/deliveries" with the same payload:
#      """
#      {
#          "requestId": "delivery-001",
#          "productId": "Product-123",
#          "destinationLocationId": "Store-001",
#          "quantity": 10
#      }
#      """
#    Then the API should respond with status code 200
#    And the response body should contain:
#      """
#      {
#          "status": "duplicate_request",
#          "message": "The request has already been processed.",
#          "processedAt": "2024-12-07T12:00:00Z"
#      }
#      """
#    And the stock levels of product "Product-123" at "Dist-001" should remain at 90
#    And the stock levels of product "Product-123" at "Store-001" should remain at 30
#
#  Scenario: Fail to record a delivery due to insufficient stock at source location
#    Given a distribution location "Dist-001" exists
#    And a store location "Store-001" exists
#    And a product "Product-123" exists with a stock level of 5 at "Dist-001"
#    When I send a POST request to "/locations/Dist-001/deliveries" with the following payload:
#      """
#      {
#          "requestId": "delivery-002",
#          "productId": "Product-123",
#          "destinationLocationId": "Store-001",
#          "quantity": 10
#      }
#      """
#    Then the API should respond with status code 400
#    And the response body should contain:
#      """
#      {
#          "status": "error",
#          "message": "Insufficient stock at source location to complete the delivery."
#      }
#      """
#    And the stock level of product "Product-123" at "Dist-001" should remain at 5
#    And the stock level of product "Product-123" at "Store-001" should remain unchanged
#
#  Scenario: Fail to record a delivery due to missing or invalid request payload
#    Given a distribution location "Dist-001" exists
#    And a store location "Store-001" exists
#    When I send a POST request to "/locations/Dist-001/deliveries" with the following payload:
#      """
#      {
#          "productId": "Product-123",
#          "destinationLocationId": "Store-001"
#      }
#      """
#    Then the API should respond with status code 400
#    And the response body should contain:
#      """
#      {
#          "status": "error",
#          "message": "Missing or invalid fields: requestId or quantity"
#      }
#      """