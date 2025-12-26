@idempotency @out-of-order-events
Feature: Idempotency - Out-of-Order Events

  This feature demonstrates that the system maintains idempotency guarantees even when out-of-order events occur.
  It covers both sales and stock movement scenarios, ensuring that duplicate requests return identical responses
  and do not result in further state changes, even if the underlying state has changed due to late-arriving events.

  Background:
    Given "Central Store" is a tracked location
    And a product "Chocolate Bar" exists in "Central Store" with a stock level of 100

  Scenario: Out-of-order events cause idempotency violations for sales
    """
    This scenario demonstrates that a sale request is idempotent even if an out-of-order stock adjustment event
    occurs between the original request and its retry. The system should return the same response for the duplicate
    request and not further reduce the stock level.
    """
    When I send a POST request to "/locations/Central Store/products/Chocolate Bar/sales" with the following payload:
      """asciidoc
      [source, json]
      -----
      {
          "requestId": "out-of-order-test-001",
          "quantity": 10.0,
          "soldAt": "2024-12-14T10:00:00Z"
      }
      -----
      """
    Then the API should respond with status code 201
    And store the response for comparison
    And the stock level of "Chocolate Bar" in "Central Store" should be updated to 90

    # Simulate an out-of-order event that changes the state (e.g., a stock adjustment that arrived late)
    When an out-of-order stock adjustment event occurs for "Chocolate Bar" in "Central Store" with quantity 70.0 at "2024-12-14T09:55:00Z"

    # Retry the same idempotent request - this should return the same response as before,
    # but currently it will return different results due to the state change
    When I send a POST request to "/locations/Central Store/products/Chocolate Bar/sales" with the following payload:
      """asciidoc
      [source, json]
      -----
      {
          "requestId": "out-of-order-test-001",
          "quantity": 10.0,
          "soldAt": "2024-12-14T10:00:00Z"
      }
      -----
      """
    Then the API should respond with status code 201
    # This assertion should pass, but currently fails due to the bug
    And the response should be identical to the previous response
    # The stock level should remain 60 (no further reduction), not be further reduced
    And the stock level of "Chocolate Bar" in "Central Store" should remain 60

  Scenario: Out-of-order events do not break move idempotency
    """
    This scenario demonstrates that a stock movement request remains idempotent even if an out-of-order sale event
    occurs in the destination location between the original move and its retry. The system should return the same
    response for the duplicate move request and not further change stock levels.
    """
    # Set up initial stock
    Given "Central Store" is a tracked location
    And "Corner Shop" is a tracked location
    And a product "Gummy Bears" exists in "Central Store" with a stock level of 50
    And a product "Gummy Bears" exists in "Corner Shop" with a stock level of 0

    # Make an initial idempotent move request that succeeds
    When I send a POST request to "/locations/Central Store/Gummy Bears/movements" with the following payload:
      """asciidoc
      [source, json]
      -----
      {
          "requestId": "move-out-of-order-test-001",
          "destinationLocationId": "Corner Shop",
          "quantity": 50.0,
          "reason": "Replenishment",
          "movedAt": "2024-12-14T10:00:00Z"
      }
      -----
      """
    Then the API should respond with status code 201
    And store the response for comparison
    And the stock level of "Gummy Bears" in "Central Store" should be updated to 0
    And the stock level of "Gummy Bears" in "Corner Shop" should be updated to 50

    # Simulate an out-of-order sale event that changes the state
    When an out-of-order stock sale event occurs for "Gummy Bears" in "Corner Shop" with quantity 1.0 at "2024-12-14T09:55:00Z"

    # Retry the same idempotent move request - this should return the same response
    When I send a POST request to "/locations/Central Store/Gummy Bears/movements" with the following payload:
      """asciidoc
      [source, json]
      -----
      {
          "requestId": "move-out-of-order-test-001",
          "destinationLocationId": "Corner Shop",
          "quantity": 50.0,
          "reason": "Replenishment",
          "movedAt": "2024-12-14T10:00:00Z"
      }
      -----
      """
    Then the API should respond with status code 201
    And the response should be identical to the previous response
    # The stock levels should remain the same (move was idempotent)
    And the stock level of "Gummy Bears" in "Central Store" should remain 0
    And the stock level of "Gummy Bears" in "Corner Shop" should remain 50