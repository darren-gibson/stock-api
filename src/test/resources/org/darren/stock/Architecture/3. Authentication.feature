@section-Architecture @asciidoc @order-2
Feature: Authentication & Authorization

  *Purpose*
  The Stock API requires authentication and authorization for all endpoints. This feature defines the authentication mechanism and authorization rules that apply across all API operations.

  *Authentication Mechanism*
  The API uses Bearer Token authentication with JWT (JSON Web Tokens). Clients must include a valid bearer token in the `Authorization` header for all requests.

  *Authorization Model*
  The API implements role-based access control with the following permissions:
  - **READ**: View stock levels and location information
  - **WRITE**: Record sales, deliveries, movements, and stock counts
  - **ADMIN**: Perform admin overrides and system configuration

  ==== Authentication Header Format

  [source, http]
  -----
  Authorization: Bearer <jwt-token>
  -----

  ==== Response Codes

  [cols="1,3", options="header"]
  |===
  | Code              | Description

  | `401 Unauthorized`| Authentication token is missing, invalid, expired, or malformed.
  | `403 Forbidden`   | Authentication is valid but the token lacks required permissions for the operation.
  |===

  ==== Authentication Error Response

  [source, json]
  -----
  {
      "status": "Unauthorized"
  }
  -----

  ==== Authorization Error Response

  [source, json]
  -----
  {
      "status": "PermissionDenied"
  }
  -----

  ==== Notes

  - All endpoints require authentication unless explicitly documented as public
  - READ operations require READ permission or higher
  - WRITE operations require WRITE permission or higher
  - ADMIN operations require ADMIN permission
  - Tokens should be validated on every request
  - Expired tokens should be rejected with `401 Unauthorized`

  Background:
    Given the following locations exist:
      | Location Id | Parent Location Id | Roles                    |
      | DC01        |                    | TrackedInventoryLocation |

  Scenario: Fail to access API without authentication token
    This test ensures that requests without an Authorization header are rejected.
    When I send a GET request to "/locations/DC01/products/product123" without authentication
    Then the API should respond with status code 401
    And the response body should contain:
      """asciidoc
      [source, json]
      -----
      {
          "status": "Unauthorized"
      }
      -----
      """

  Scenario: Fail to access API with invalid authentication token
    This test ensures that requests with malformed or invalid tokens are rejected.
    Given I have an invalid authentication token
    When I send a GET request to "/locations/DC01/products/product123"
    Then the API should respond with status code 401
    And the response body should contain:
      """asciidoc
      [source, json]
      -----
      {
          "status": "Unauthorized"
      }
      -----
      """

  Scenario: Fail to access API with expired authentication token
    This test ensures that expired tokens are rejected.
    Given I have an expired authentication token
    When I send a GET request to "/locations/DC01/products/product123"
    Then the API should respond with status code 401
    And the response body should contain:
      """asciidoc
      [source, json]
      -----
      {
          "status": "Unauthorized"
      }
      -----
      """

  Scenario: Successfully access API with valid READ permission
    This test ensures that valid tokens with READ permission can access read-only endpoints.
    Given I have a valid authentication token with READ permission
    And a product "product123" exists in "DC01" with a stock level of 100
    When I send a GET request to "/locations/DC01/products/product123"
    Then the API should respond with status code 200

  Scenario: Fail to perform write operation with only READ permission
    This test ensures that tokens with only READ permission cannot perform write operations.
    Given I have a valid authentication token with READ permission
    And "DC01" is a tracked location
    And a product "product123" exists in "DC01" with a stock level of 100
    When I send a POST request to "/locations/DC01/products/product123/sales" with the following payload:
      """asciidoc
      [source, json]
      -----
      {
          "requestId": "sale-001",
          "quantity": 5,
          "soldAt": "2024-12-13T12:00:00Z"
      }
      -----
      """
    Then the API should respond with status code 403
    And the response body should contain:
      """asciidoc
      [source, json]
      -----
      {
          "status": "PermissionDenied"
      }
      -----
      """

  Scenario: Successfully perform write operation with WRITE permission
    This test ensures that valid tokens with WRITE permission can perform write operations.
    Given I have a valid authentication token with WRITE permission
    And "DC01" is a tracked location
    And a product "product123" exists in "DC01" with a stock level of 100
    When I send a POST request to "/locations/DC01/products/product123/sales" with the following payload:
      """asciidoc
      [source, json]
      -----
      {
          "requestId": "sale-002",
          "quantity": 5,
          "soldAt": "2024-12-13T12:00:00Z"
      }
      -----
      """
    Then the API should respond with status code 201

  Scenario: Fail to perform admin operation without ADMIN permission
    This test ensures that admin operations require ADMIN permission.
    Given I have a valid authentication token with WRITE permission
    And "DC01" is a tracked location
    And a product "product123" exists in "DC01" with a stock level of 100
    When I send a POST request to "/locations/DC01/products/product123/counts" with the following payload:
      """asciidoc
      [source, json]
      -----
      {
          "requestId": "count-001",
          "quantity": 50,
          "reason": "Override",
          "countedAt": "2024-12-13T12:00:00Z"
      }
      -----
      """
    Then the API should respond with status code 403
    And the response body should contain:
      """asciidoc
      [source, json]
      -----
      {
          "status": "PermissionDenied"
      }
      -----
      """

  Scenario: Successfully perform admin operation with ADMIN permission
    This test ensures that valid tokens with ADMIN permission can perform admin operations.
    Given I have a valid authentication token with ADMIN permission
    And "DC01" is a tracked location
    And a product "product123" exists in "DC01" with a stock level of 100
    When I send a POST request to "/locations/DC01/products/product123/counts" with the following payload:
      """asciidoc
      [source, json]
      -----
      {
          "requestId": "count-002",
          "quantity": 50,
          "reason": "Override",
          "countedAt": "2024-12-13T12:00:00Z"
      }
      -----
      """
    Then the API should respond with status code 201
