@idempotency @content-validation
Feature: Idempotency - Request Content Validation

  Background:
    Given "Store-001" is a tracked location
    And a product "SKU12345" exists in "Store-001" with a stock level of 100

  Scenario: Duplicate requestId with identical content should return cached response
    When I send a POST request to "/locations/Store-001/products/SKU12345/sales" with the following payload:
      """
      {
          "requestId": "content-test-001",
          "quantity": 10.0,
          "soldAt": "2024-12-14T10:00:00Z"
      }
      """
    Then the API should respond with status code 201
    And the response body should contain:
      """
      {
          "requestId": "content-test-001",
          "locationId": "Store-001",
          "productId": "SKU12345",
          "quantitySold": 10.0,
          "soldAt": "2024-12-14T10:00:00"
      }
      """
    And the response should be cached in the idempotency store
    And the stock level of "SKU12345" in "Store-001" should be updated to 90
    
    # Send exact same request again
    When I send a POST request to "/locations/Store-001/products/SKU12345/sales" with the following payload:
      """
      {
          "requestId": "content-test-001",
          "quantity": 10.0,
          "soldAt": "2024-12-14T10:00:00Z"
      }
      """
    Then the API should respond with status code 201
    And the response body should contain:
      """
      {
          "requestId": "content-test-001",
          "locationId": "Store-001",
          "productId": "SKU12345",
          "quantitySold": 10.0,
          "soldAt": "2024-12-14T10:00:00"
      }
      """
    And the response should be cached in the idempotency store
    And the stock level of "SKU12345" in "Store-001" should remain 90

  # This scenario validates that duplicate requestId with different content returns 409 Conflict
  # The body hash detects content mismatches and prevents accidental reuse of requestIds
  Scenario: Duplicate requestId with different quantity (content mismatch)
    When I send a POST request to "/locations/Store-001/products/SKU12345/sales" with the following payload:
      """
      {
          "requestId": "content-test-002",
          "quantity": 10.0,
          "soldAt": "2024-12-14T10:00:00Z"
      }
      """
    Then the API should respond with status code 201
    And the stock level of "SKU12345" in "Store-001" should be updated to 90
    
    # Send request with SAME requestId but DIFFERENT quantity
    # Expected: Returns 409 Conflict due to content mismatch
    When I send a POST request to "/locations/Store-001/products/SKU12345/sales" with the following payload:
      """
      {
          "requestId": "content-test-002",
          "quantity": 20.0,
          "soldAt": "2024-12-14T10:00:00Z"
      }
      """
    Then the API should respond with status code 409
    And the stock level of "SKU12345" in "Store-001" should remain 90

  # This scenario validates that duplicate requestId with different timestamp returns 409 Conflict
  # Even small changes like timestamp trigger content mismatch detection
  Scenario: Duplicate requestId with different timestamp (content mismatch)
    When I send a POST request to "/locations/Store-001/products/SKU12345/sales" with the following payload:
      """
      {
          "requestId": "content-test-003",
          "quantity": 10.0,
          "soldAt": "2024-12-14T10:00:00Z"
      }
      """
    Then the API should respond with status code 201
    And the stock level of "SKU12345" in "Store-001" should be updated to 90
    
    # Send request with SAME requestId but DIFFERENT timestamp
    # Expected: Returns 409 Conflict due to content mismatch
    When I send a POST request to "/locations/Store-001/products/SKU12345/sales" with the following payload:
      """
      {
          "requestId": "content-test-003",
          "quantity": 10.0,
          "soldAt": "2024-12-14T15:30:00Z"
      }
      """
    Then the API should respond with status code 409
    And the stock level of "SKU12345" in "Store-001" should remain 90

  Scenario: Different requestIds with identical content should both be processed
    When I send a POST request to "/locations/Store-001/products/SKU12345/sales" with the following payload:
      """
      {
          "requestId": "unique-001",
          "quantity": 10.0,
          "soldAt": "2024-12-14T10:00:00Z"
      }
      """
    Then the API should respond with status code 201
    And the stock level of "SKU12345" in "Store-001" should be updated to 90
    
    # Send identical content but DIFFERENT requestId - should process normally
    When I send a POST request to "/locations/Store-001/products/SKU12345/sales" with the following payload:
      """
      {
          "requestId": "unique-002",
          "quantity": 10.0,
          "soldAt": "2024-12-14T10:00:00Z"
      }
      """
    Then the API should respond with status code 201
    And the response body should contain:
      """
      {
          "requestId": "unique-002",
          "locationId": "Store-001",
          "productId": "SKU12345",
          "quantitySold": 10.0,
          "soldAt": "2024-12-14T10:00:00"
      }
      """
    And the stock level of "SKU12345" in "Store-001" should be updated to 80
