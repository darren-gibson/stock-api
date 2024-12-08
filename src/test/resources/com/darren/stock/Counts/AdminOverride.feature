Feature: Create a Stock Count for a Product
  As a Admin Users
  I want to create a stock count for a product at a specific location
  So that the system reflects accurate inventory levels

#  Background:
#    Given the API is authenticated with a valid bearer token

  Scenario: Successfully create a stock count
    Given "Warehouse-01" is a Distribution Centre
    And a valid product code "SKU12345" exists
    When I send a POST request to "/locations/Warehouse-01/products/SKU12345/counts" with the following payload:
      """
      {
          "requestId": "123e4567-e89b-12d3-a456-426614174000",
          "reason": "AdminOverride",
          "quantity": 100
      }
      """
    Then the API should respond with status code 201
    And the response body should contain:
      """
        {
          "requestId": "123e4567-e89b-12d3-a456-426614174000",
          "location": "Warehouse-01",
          "productId": "SKU12345",
          "quantity": 100.0,
          "reason": "AdminOverride",
          "createdAt": "${json-unit.ignore}"
        }
      """
    And the current stock level of "SKU12345" in "Warehouse-01" will equal 100

#  Scenario: Handle duplicate stock count creation with the same requestId
#    Given a valid location "Warehouse-01" exists
#    And a valid product code "SKU12345" exists
#    And a stock count has already been created with "requestId": "123e4567-e89b-12d3-a456-426614174000"
#    When I resend the POST request to "/locations/Warehouse-01/products/SKU12345/counts" with the same payload:
#      """
#      {
#          "requestId": "123e4567-e89b-12d3-a456-426614174000",
#          "reason": "AdminOverride",
#          "quantity": 100
#      }
#      """
#    Then the API should respond with status code 200
#    And the response body should contain:
#      """
#      {
#          "status": "duplicate_request",
#          "message": "The request has already been processed.",
#          "processedAt": "2024-12-07T10:00:00Z"
#      }
#      """
#
  Scenario: Fail to create a stock count due to invalid location
    Given an invalid location "Invalid-Warehouse" is provided
    When I send a POST request to "/locations/Invalid-Warehouse/products/SKU12345/counts" with the following payload:
      """
      {
          "requestId": "123e4567-e89b-12d3-a456-426614174003",
          "reason": "AdminOverride",
          "quantity": 100
      }
      """
    Then the API should respond with status code 404
    And the response body should contain:
      """
      {
          "status": "LocationNotFound"
      }
      """

  Scenario: Fail to create a stock count due to missing requestId
    Given "Warehouse-01" is a Distribution Centre
    And a valid product code "SKU12345" exists
    When I send a POST request to "/locations/Warehouse-01/products/SKU12345/counts" with the following payload:
      """
      {
          "reason": "AdminOverride",
          "quantity": 100
      }
      """
    Then the API should respond with status code 400

  Scenario: Fail to create a stock count due to missing reason
    Given "Warehouse-01" is a Distribution Centre
    And a valid product code "SKU12345" exists
    When I send a POST request to "/locations/Warehouse-01/products/SKU12345/counts" with the following payload:
      """
      {
          "requestId": "123e4567-e89b-12d3-a456-426614174002",
          "quantity": 100
      }
      """
    Then the API should respond with status code 400
