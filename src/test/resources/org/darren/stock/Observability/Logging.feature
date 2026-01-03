@Observability
@Logging
@order-1
Feature: Logging

  *Purpose*
  This feature documents the expected observability behaviour of the Stock API's
  logging infrastructure. It verifies that requests are issued with a trace
  id, that the id is returned to the caller, and that the trace id is
  propagated through downstream calls and actor/handler logs.

  *Overview*
  - The API must emit structured logs that include a `traceId` value.
  - Incoming HTTP requests should be assigned a `traceparent` header when
    one is not provided by the client.
  - The same trace id must be present in logs for the HTTP handler,
    any actor processing the request, and outbound HTTP calls initiated while
    processing the request.

  Background:
    Given the application is running
    And "DC01" is a Distribution Centre
    And "DC02" is a Distribution Centre

  Scenario: Trace context is propagated internally
    When I send a GET request to "/_status" with header "traceparent" -> "00-12345678901234567890123456789012-1234567890123456-01"
    Then the API should respond with status code 200
    And the response header "traceparent" should not be present

  Scenario: Correlation id is present in handler logs
    This scenario asserts that log output from the HTTP handler includes the trace id.
    When I send a POST request to "/locations/DC01/products/product123/sales" with the payload:
      """asciidoc
      [source, json]
      -----
      {
        "requestId": "sale-log-001",
        "quantity": 1,
        "soldAt": "2024-12-13T12:00:00Z"
      }
      -----
      """
    Then the API should respond with status code 201
    And logs produced while processing the request should include the same trace id value

  Scenario: Correlation id is propagated to outbound HTTP calls
    When I send a GET request to "/locations/DC01/products/product123" which triggers an outbound location lookup
    Then the API should respond with status code 200
    And the outbound HTTP call made by the service should include a traceparent header matching the request's trace id
    
  Scenario: Correlation id is visible in actor logs
    When I send a POST request to "/locations/DC01/deliveries" with the payload:
      """asciidoc
      [source, json]
      -----
      {
        "requestId": "delivery-log-001",
        "supplierId": "supplier-123",
        "supplierRef": "supplier-order-456",
        "deliveredAt": "2024-12-13T12:00:00Z",
        "products": [
          { "productId": "product123", "quantity": 10 }
        ]
      }
      -----
      """
    Then the API should respond with status code 201
    And logs emitted by the actor(s) handling the delivery should contain the request's trace id

  Scenario: Actor logs include correlation id for sale
    When I send a POST request to "/locations/DC01/products/product123/sales" with the payload:
      """asciidoc
      [source, json]
      -----
      {
        "requestId": "sale-actor-log-001",
        "quantity": 1,
        "soldAt": "2024-12-13T12:00:00Z"
      }
      -----
      """
    Then the API should respond with status code 201
    And logs emitted by the actor(s) handling the sale should contain the request's trace id

  Scenario: Actor logs include correlation id for count
    When I send a POST request to "/locations/DC01/products/product123/counts" with the payload:
      """asciidoc
      [source, json]
      -----
      {
        "requestId": "count-actor-log-001",
        "reason": "AdminOverride",
        "quantity": 5.0,
        "countedAt": "2024-12-13T12:00:00Z"
      }
      -----
      """
    Then the API should respond with status code 201
    And logs emitted by the actor(s) handling the count should contain the request's trace id

  Scenario: The log contains the message "=== All done <traceId> ==="
    When I send a POST request to "/locations/DC01/products/product123/counts" with the payload:
      """asciidoc
      [source, json]
      -----
      {
        "requestId": "count-actor-log-001",
        "reason": "AdminOverride",
        "quantity": 5.0,
        "countedAt": "2024-12-13T12:00:00Z"
      }
      -----
      """
    Then the API should respond with status code 201
    And logs emitted should contain the request's trace id within the "=== All done <traceId> ===" log

  Scenario: Actor logs include correlation id for move
    Given the stock level of "product123" in "DC01" is 5.0
    When I send a POST request to "/locations/DC01/product123/movements" with the payload:
      """asciidoc
      [source, json]
      -----
      {
        "requestId": "move-actor-log-001",
        "destinationLocationId": "DC02",
        "quantity": 3.0,
        "reason": "replenishment",
        "movedAt": "2024-12-13T12:00:00Z"
      }
      -----
      """
    Then the API should respond with status code 201
    And logs emitted by the actor(s) handling the move should contain the request's trace id

  Scenario: Structured log contains trace id field
    When I send a GET request to "/_status"
    Then the API should respond with status code 200
    And at least one structured log event produced during the request should contain a traceId field

  Scenario: Logs include span id alongside trace id
    When I send a GET request to "/_status"
    Then the API should respond with status code 200
    And logs produced while processing the request should include a span id value
