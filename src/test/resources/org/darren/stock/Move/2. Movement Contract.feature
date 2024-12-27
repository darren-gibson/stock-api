@section-Movement @asciidoc @order-1
Feature: Contract: Stock Movement Endpoint

  === Create Stock Movement Transaction Endpoint Specification

  The Create Stock Movement Transaction endpoint enables users to log the transfer of stock between locations. This endpoint is essential for maintaining real-time accuracy in inventory tracking and ensuring seamless operational workflows. The endpoint supports detailed documentation of each transaction, capturing critical data points for transparency and traceability.

  [cols="1,3", options="header"]
  |===
  | Attribute        | Description

  | `endpoint`       | `/locations/{sourceLocationId}/{productId}/movements`
  | `method`         | `POST`
  | `description`    | Creates a new stock movement transaction in the system.
  | `authentication` | Requires a valid API token with appropriate permissions.
  | `response format`| JSON
  |===

  ==== Input Parameters

  The Create Stock Movement Transaction endpoint accepts the following mandatory and optional input parameters:

  [cols="1,4,1,2", options="header"]
  |===
  | Parameter              | Description                                                        | Required | Example

  | `sourceLocationId`     | The identifier for the source location.                            | Yes      | `"warehouse1"`
  | `destinationLocationId`| The identifier for the destination location.                      | Yes      | `"store3"`
  | `quantity`             | The number of items being moved (as a double).                    | Yes      | `50.5`
  | `productId`            | The unique identifier for the product being moved.                | Yes      | `"product123"`
  | `requestId`            | A unique identifier for the request.                              | Yes      | `"req-12345"`
  | `Authentication Token` | The API token used for authentication.                            | Yes      | `Bearer abc123xyz`
  | `reason`               | The reason for the stock movement.                                | Optional | `"replenishment"`
  |===

  ==== REST Endpoint Definition

  **Endpoint:**
  `POST /locations/{sourceLocationId}/{productId}/movements`

  **Request Body:**
  The request body must include the parameters in JSON format.

  [source, http]
  .Request to create a stock movement transaction
  -----
  POST /locations/warehouse1/product123/movements HTTP/1.1
  Host: api.example.com
  Authorization: Bearer abc123xyz
  Content-Type: application/json

  {
      "requestId": "req-12345",
      "destinationLocationId": "store3",
      "quantity": 50.5,
      "reason": "replenishment"
  }
  -----

  ==== Example Response

  [source, http]
  .Successful response
  -----
  HTTP/1.1 201 Created
  Content-Type: application/json

  {
      "requestId": "req-12345",
      "sourceLocationId": "warehouse1",
      "destinationLocationId": "store3",
      "quantity": 50.5,
      "productId": "product123",
      "reason": "replenishment",
      "createdAt": "2024-12-26T15:35:00Z"
  }
  -----

  ==== Response Codes

  [cols="1,3", options="header"]
  |===
  | Code              | Description

  | `201 Created`     | The stock movement transaction was successfully created.
  | `400 Bad Request` | Invalid request data or missing required fields.
  | `401 Unauthorized`| Authentication token is missing or invalid.
  | `403 Forbidden`   | User does not have permission to create stock movements.
  | `409 Conflict`    | A transaction with the same parameters already exists.
  | `500 Internal Server Error` | An unexpected error occurred on the server.
  |===

  ==== Notes

  - **Authentication Requirement:** A valid API token with sufficient permissions is mandatory to use this endpoint.
  - **Reason Field:** The `reason` parameter is optional but provides context for the stock movement and supports reporting.
  - **Precision in Quantity:** The `quantity` parameter is a double to support fractional stock movements where applicable.

  The Create Stock Movement Transaction endpoint offers a reliable and detailed way to log inventory transfers, enabling enhanced traceability and operational efficiency.

  Background:
    Given "warehouse1" is a Distribution Centre
    And "store3" is a store

  Scenario: Successfully record a stock movement transaction
  This is a "happy path" test to ensure that the stock movement endpoint accepts a valid JSON request and records the transaction correctly.
    Given a product "product123" exists in "warehouse1" with a stock level of 100.5
    When I send a POST request to "/locations/warehouse1/product123/movements" with the following payload:
      """asciidoc
      [source, json]
      -----
      {
          "destinationLocationId": "store3",
          "quantity": 50.5,
          "requestId": "req-12345",
          "reason": "replenishment"
      }
      -----
      """
    Then the API should respond with status code 201
    And the response body should contain:
      """asciidoc
      [source, json]
      -----
      {
          "requestId": "req-12345",
          "sourceLocationId": "warehouse1",
          "destinationLocationId": "store3",
          "quantity": 50.5,
          "productId": "product123",
          "reason": "replenishment",
          "createdAt": "<timestamp>" (1)
      }
      -----
      <1> timestamp in the ISO8601 format, e.g., 2024-12-26T15:35:00.123Z
      """
    And the stock level of "product123" in "warehouse1" should be updated to 50.0

  Scenario: Fail to record a stock movement due to invalid source location
    This test ensures that the stock movement endpoint returns an appropriate error when the source location is invalid.

    Given "invalidLocation" does not exist as a Distribution Centre
    When I send a POST request to "/locations/invalidLocation/product123/movements" with the following payload:
      """asciidoc
      [source, json]
      -----
      {
          "requestId": "req-12346",
          "destinationLocationId": "store3",
          "quantity": 10.0,
          "reason": "replenishment"
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

  Scenario: Fail to record a stock movement due to missing input parameters
    This test ensures that the stock movement endpoint returns an error response when required parameters are missing.

    Given a product "product123" exists in "warehouse1" with a stock level of 100.5
    When I send a POST request to "/locations/warehouse1/product123/movements" with the following payload:
      """asciidoc
      [source, json]
      -----
      {
          "destinationLocationId": "store3",
          "quantity": 20.0
      }
      -----
      """
    Then the API should respond with status code 400
    And the response body should contain:
      """asciidoc
      [source, json]
      -----
      {
          "missingFields": ["requestId", "reason"]
      }
      -----
      """

  Scenario: Fail to record a stock movement due to multiple missing input parameters
    This test ensures that the stock movement endpoint returns an error response when multiple required parameters are missing.

    Given a product "product123" exists in "warehouse1" with a stock level of 100.5
    When I send a POST request to "/locations/warehouse1/product123/movements" with the following payload:
      """asciidoc
      [source, json]
      -----
      {
          "requestId": "req-12348",
          "reason": "replenishment"
      }
      -----
      """
    Then the API should respond with status code 400
    And the response body should contain:
      """asciidoc
      [source, json]
      -----
      {
          "missingFields": ["destinationLocationId", "quantity"]
      }
      -----
      """


#  Scenario: Fail to record a stock movement due to insufficient permissions
#    This test ensures that the stock movement endpoint returns a proper error response when the authentication token lacks necessary permissions.
#
#    Given I have an invalid or insufficient API token
#    When I send a POST request to "/locations/warehouse1/product123/movements" with the following payload:
#      """asciidoc
#      [source, json]
#      -----
#      {
#          "destinationLocationId": "store3",
#          "quantity": 20.0,
#          "requestId": "req-12347",
#          "reason": "redistribution"
#      }
#      -----
#      """
#    Then the API should respond with status code 403
#    And the response body should contain:
#      """asciidoc
#      [source, json]
#      -----
#      {
#          "status": "PermissionDenied"
#      }
#      -----
#      """
