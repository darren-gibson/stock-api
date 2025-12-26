@idempotency @failure-handling
Feature: Idempotency - Failure Handling

  Background:
    Given "Store-001" is a tracked location
    And a product "SKU12345" exists in "Store-001" with a stock level of 50

  # This scenario validates that 5xx errors are not cached and can be retried
  # Uses TestStockEventRepository to simulate repository failures
  Scenario: Server errors (5xx) should not be cached - retries should be allowed
    Given the stock system will fail with a 500 error on the first request
    When I send a POST request to "/locations/Store-001/products/SKU12345/sales" with the following payload:
      """asciidoc
      [source, json]
      -----
      {
          "requestId": "error-test-001",
          "quantity": 5.0,
          "soldAt": "2024-12-14T10:00:00Z"
      }
      -----
      """
    Then the API should respond with status code 500
    And the stock level of "SKU12345" in "Store-001" should remain 50
    
    # Now the system recovers and the retry should succeed
    When the stock system is working normally
    And I send a POST request to "/locations/Store-001/products/SKU12345/sales" with the following payload:
      """asciidoc
      [source, json]
      -----
      {
          "requestId": "error-test-001",
          "quantity": 5.0,
          "soldAt": "2024-12-14T10:00:00Z"
      }
      -----
      """
    Then the API should respond with status code 201
    And the stock level of "SKU12345" in "Store-001" should be updated to 45

  Scenario: Successful responses (2xx) should be cached
    When I send a POST request to "/locations/Store-001/products/SKU12345/sales" with the following payload:
      """asciidoc
      [source, json]
      -----
      {
          "requestId": "success-test-001",
          "quantity": 5.0,
          "soldAt": "2024-12-14T10:00:00Z"
      }
      -----
      """
    Then the API should respond with status code 201
    And the stock level of "SKU12345" in "Store-001" should be updated to 45
    And store the response for comparison
    
    # Duplicate request should return identical response
    When I send a POST request to "/locations/Store-001/products/SKU12345/sales" with the following payload:
      """
      {
          "requestId": "success-test-001",
          "quantity": 5.0,
          "soldAt": "2024-12-14T10:00:00Z"
      }
      """
    Then the API should respond with status code 201
    And the response should be identical to the previous response
    And the stock level of "SKU12345" in "Store-001" should remain 45

  Scenario: Client errors (4xx) behavior - validation errors
    Given "InvalidLocation" is an untracked location
    When I send a POST request to "/locations/InvalidLocation/products/SKU12345/sales" with the following payload:
      """
      {
          "requestId": "validation-error-001",
          "quantity": 5.0,
          "soldAt": "2024-12-14T10:00:00Z"
      }
      """
    Then the API should respond with status code 400
    And store the response for comparison
    
    # Duplicate request should return identical error response
    When I send a POST request to "/locations/InvalidLocation/products/SKU12345/sales" with the following payload:
      """
      {
          "requestId": "validation-error-001",
          "quantity": 5.0,
          "soldAt": "2024-12-14T10:00:00Z"
      }
      """
    Then the API should respond with status code 400
    And the response should be identical to the previous response
