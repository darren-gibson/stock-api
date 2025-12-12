@section-Counts @asciidoc @order-1
Feature: Override stock count record for Product in a Location

  *Why Override a Stock Figure for a Location?*
  In inventory management systems, overriding a stock figure for a location is an administrative action that adjusts the recorded inventory level of a product at a specific location to reflect the correct quantity. This action is necessary in a few situations where discrepancies arise between the system-recorded stock level and the actual stock level. This action is acts as an override and should be taken as a last resort.

  Below are the key reasons and scenarios for performing a stock override:

  1. [.underline]#Correcting Discrepancies#
  To set the system-recorded stock with the physical inventory. For example where a counting error during stocktaking resulted in incorrect stock levels being entered into the system.
  2. [.underline]#Recovering from a System issue#
  In the event of a system defect that's led to a stock file error and this needs to be corrected.

  As a Admin User
  I want to override a stock count for a product at a specific location
  So that the system reflects accurate inventory levels

#  Background:
#    Given the API is authenticated with a valid bearer token

  @asciidoc
  Scenario: "Override" as a stock count reason will set the stock level at that location
  Count recorded as an override will be used in all future readings of stock levels for the product at the location as the base.

    Given "Warehouse-01" is a Distribution Centre
    And a valid product code "SKU12345" exists
    When I send a POST request to "/locations/Warehouse-01/products/SKU12345/counts" with the following payload:
      """asciidoc
      [source, json]
      -----
      {
          "requestId": "123e4567-e89b-12d3-a456-426614174000",
          "reason": "AdminOverride",
          "quantity": 100,
          "countedAt": "2024-12-11T09:16:29.577617"
      }
      -----
      """
    Then the API should respond with status code 201
    And the response body should contain:
      """asciidoc
      [source, json]
      -----
      {
        "requestId": "123e4567-e89b-12d3-a456-426614174000",
        "location": "Warehouse-01",
        "productId": "SKU12345",
        "quantity": 100.0,
        "reason": "AdminOverride",
        "countedAt": "2024-12-11T09:16:29.577617" (1)
      }
      -----
      <1> timestamp in the ISO8601 format, e.g: 2024-12-11T09:16:29.577617
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
    locations must exist and be known to the system before counts can be recorded against them. Attempting to set the stock level for a product in a location that does not exist will result in an error.
    Given an invalid location "Invalid-Warehouse" is provided
    When I send a POST request to "/locations/Invalid-Warehouse/products/SKU12345/counts" with the following payload:
      """asciidoc
      [source, json]
      -----
      {
          "requestId": "123e4567-e89b-12d3-a456-426614174003",
          "reason": "AdminOverride",
          "quantity": 100,
          "countedAt": "2024-12-11T09:16:29.577617"
      }
      -----
      """
    Then the API should respond with status code 404
    And the response body should contain:
      """asciidoc
      [source, json]
      -----
      {
          "status": "LocationNotFound"
      }
      -----
      """

  Scenario: Fail to create a stock count due to missing requestId
    Given "Warehouse-01" is a Distribution Centre
    And a valid product code "SKU12345" exists
    When I send a POST request to "/locations/Warehouse-01/products/SKU12345/counts" with the following payload:
      """asciidoc
      [source, json]
      -----
      {
          "reason": "AdminOverride",
          "quantity": 100,
          "countedAt": "2024-12-11T09:16:29.577617"
      }
      -----
      """
    Then the API should respond with status code 400

  Scenario: Fail to create a stock count due to missing reason
    Given "Warehouse-01" is a Distribution Centre
    And a valid product code "SKU12345" exists
    When I send a POST request to "/locations/Warehouse-01/products/SKU12345/counts" with the following payload:
      """asciidoc
      [source, json]
      -----
      {
          "requestId": "123e4567-e89b-12d3-a456-426614174002",
          "quantity": 100,
          "countedAt": "2024-12-11T09:16:29.577617"
      }
      -----
      """
    Then the API should respond with status code 400

  Scenario: Fail to create a stock count due to missing countedAt
    Given "Warehouse-01" is a Distribution Centre
    And a valid product code "SKU12345" exists
    When I send a POST request to "/locations/Warehouse-01/products/SKU12345/counts" with the following payload:
      """asciidoc
      [source, json]
      -----
      {
          "requestId": "123e4567-e89b-12d3-a456-426614174002",
          "reason": "AdminOverride",
          "quantity": 100
      }
      -----
      """
    Then the API should respond with status code 400

  Scenario: Fail to create a stock count due to invalid countedAt
    Given "Warehouse-01" is a Distribution Centre
    And a valid product code "SKU12345" exists
    When I send a POST request to "/locations/Warehouse-01/products/SKU12345/counts" with the following payload:
      """asciidoc
      [source, json]
      -----
      {
          "requestId": "123e4567-e89b-12d3-a456-426614174002",
          "reason": "AdminOverride",
          "quantity": 100,
          "countedAt": "invalid-timestamp"
      }
      -----
      """
    Then the API should respond with status code 400
