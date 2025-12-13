@section-Architecture @asciidoc @order-2
Feature: Authentication & Authorization

  *Purpose*
  The Stock API requires authentication and authorization for all endpoints. This feature defines the authentication mechanism and authorization rules that apply across all API operations.

  *Authentication Mechanism*
  The API uses Bearer Token authentication with JWT (JSON Web Tokens). Clients must include a valid bearer token in the `Authorization` header for all requests.

  *Authorization Model*
  The API implements job-based access control where:
  - The bearer token contains colleague identity and their job function (e.g., "Store Stock Controller")
  - Job functions are mapped to specific permissions/claims maintained within the Stock API
  - Permissions can be scoped to specific locations (e.g., "Cambridge" store)
  - The Stock API maintains the job-to-permission mappings internally
  - Admin functions exist to manage job roles and their associated permissions

  *Example Job Functions and Permissions*
  - **Store Stock Controller** (location-scoped): Can record stock counts for their assigned location(s)
  - **Warehouse Manager** (location-scoped): Can record deliveries, movements, and counts for their warehouse
  - **Regional Stock Auditor**: Can view stock levels across multiple locations
  - **System Administrator**: Can manage job roles, permissions, and system configuration

  *Token Claims*
  The JWT token must contain:
  - `sub`: Colleague unique identifier
  - `name`: Colleague name
  - `job`: Job function (e.g., "Store Stock Controller")
  - `location`: Assigned location(s) if job is location-scoped (optional array)
  - `iss`: Token issuer
  - `aud`: Token audience
  - `exp`: Token expiration timestamp
  - `iat`: Token issued at timestamp

  *Token Validation*
  - JWT signature must be valid (RS256 algorithm)
  - Token must not be expired (`exp` > current time)
  - Issuer (`iss`) must match expected identity provider
  - Audience (`aud`) must include this API

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
  - The Stock API validates the JWT signature using public keys from the identity provider
  - The identity provider is treated as an external system (mocked in tests)
  - JWT tokens follow OAuth 2.0 standard with RS256 signing algorithm
  - The Stock API looks up the job function from the token and resolves it to specific permissions
  - Location-scoped permissions are validated against the location in the request
  - Expired or invalid tokens are rejected with `401 Unauthorized`
  - Valid tokens without required permissions return `403 Forbidden`
  - Job-to-permission mappings are managed through admin endpoints

  Background:
    Given the identity provider is available
    And the following locations exist:
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
    This test ensures that valid tokens with appropriate job function can access read endpoints.
    Given I have a valid authentication token with job "Regional Stock Auditor"
    And a product "product123" exists in "DC01" with a stock level of 100
    When I send a GET request to "/locations/DC01/products/product123"
    Then the API should respond with status code 200

  Scenario: Fail to perform write operation without required permission
    This test ensures that tokens without write permissions for the operation cannot perform it.
    Given I have a valid authentication token with job "Regional Stock Auditor"
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

  Scenario: Successfully perform write operation with appropriate job permission
    This test ensures that valid tokens with appropriate job function can perform write operations.
    Given I have a valid authentication token with job "Store Stock Controller" for location "DC01"
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

  Scenario: Fail to perform operation at location outside colleague scope
    This test ensures that location-scoped permissions are enforced.
    Given I have a valid authentication token with job "Store Stock Controller" for location "Cambridge"
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

  Scenario: Successfully perform operation at location within colleague scope
    This test ensures that location-scoped permissions work correctly.
    Given I have a valid authentication token with job "Store Stock Controller" for location "Cambridge"
    And "Cambridge" is a tracked location
    And a product "product123" exists in "Cambridge" with a stock level of 100
    When I send a POST request to "/locations/Cambridge/products/product123/counts" with the following payload:
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

  Scenario: Fail to manage job permissions without admin rights
    This test ensures that job-permission management requires appropriate admin access.
    Given I have a valid authentication token with job "Store Stock Controller" for location "Cambridge"
    When I send a POST request to "/admin/jobs" with the following payload:
      """asciidoc
      [source, json]
      -----
      {
          "jobFunction": "New Role",
          "permissions": ["stock:count:write"]
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

  Scenario: Successfully manage job permissions with admin rights
    This test ensures that system administrators can manage job-to-permission mappings.
    Given I have a valid authentication token with job "System Administrator"
    When I send a POST request to "/admin/jobs" with the following payload:
      """asciidoc
      [source, json]
      -----
      {
          "jobFunction": "Warehouse Manager",
          "permissions": ["stock:read", "stock:movement:write", "stock:delivery:write", "stock:count:write"],
          "locationScoped": true
      }
      -----
      """
    Then the API should respond with status code 201
