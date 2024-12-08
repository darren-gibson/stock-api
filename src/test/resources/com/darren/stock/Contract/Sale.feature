Feature: Sale API Contract Test - Record a Product Sale in a Store
  As a system user
  I want to record a sale of a product in a store
  So that the system updates the stock levels accurately

#  Background:
#    Given the API is authenticated with a valid bearer token

  Scenario: Successfully record a product sale and reduce stock level
    Given "Store-001" is a store
    And a product "SKU12345" exists in "Store-001" with a stock level of 50
    When I send a POST request to "/stores/Store-001/products/SKU12345/sales" with the following payload:
      """
      {
          "requestId": "abc123-e89b-12d3-a456-426614174000",
          "quantity": 5
      }
      """
    Then the API should respond with status code 201
    And the response body should contain:
      """
      {
        "requestId": "abc123-e89b-12d3-a456-426614174000",
        "location": "Store-001",
        "productId": "SKU12345",
        "quantitySold": 5.0,
        "saleTimestamp": "${json-unit.ignore}"
      }
      """
    And the stock level of product "SKU12345" in "Store-001" should be updated to 45

#  Scenario: Handle duplicate sale request using the same requestId
#    Given "Store-001" is a store
#    And a product "SKU12345" exists in "Store-001" with a stock level of 45
#    And a sale request with "requestId": "abc123-e89b-12d3-a456-426614174000" has already been processed
#    When I resend the POST request to "/stores/Store-001/products/SKU12345/sales" with the same payload:
#      """
#      {
#          "requestId": "abc123-e89b-12d3-a456-426614174000",
#          "quantity": 5
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
#    And the stock level of product "SKU12345" in "Store-001" should remain at 45
#
#  Scenario: Fail to record a sale due to insufficient stock
#    Given "Store-001" is a store
#    And a product "SKU12345" exists in "Store-001" with a stock level of 3
#    When I send a POST request to "/stores/Store-001/products/SKU12345/sales" with the following payload:
#      """
#      {
#          "requestId": "def456-e89b-12d3-a456-426614174000",
#          "quantity": 5
#      }
#      """
#    Then the API should respond with status code 400
#    And the response body should contain:
#      """
#      {
#          "status": "error",
#          "message": "Insufficient stock to complete the sale."
#      }
#      """
#    And the stock level of product "SKU12345" in "Store-001" should remain at 3
#
#  Scenario: Fail to record a sale due to invalid request payload
#    Given "Store-001" is a store
#    And a product "SKU12345" exists in "Store-001" with a stock level of 45
#    When I send a POST request to "/stores/Store-001/products/SKU12345/sales" with the following payload:
#      """
#      {
#          "quantity": 5
#      }
#      """
#    Then the API should respond with status code 400
#    And the response body should contain:
#      """
#      {
#          "status": "error",
#          "message": "Missing or invalid requestId"
#      }
#      """
