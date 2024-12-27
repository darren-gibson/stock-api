@section-StockLevel @asciidoc @order-1
Feature: Contract: Get Stock Level for Product at Location Endpoint

    === Get Stock Level for Product at Location Endpoint Specification

    The Get Stock Level for Product at Location endpoint provides real-time visibility into the inventory of specific products at designated locations. This functionality is essential for maintaining accurate inventory data and supporting operational decisions.

    [cols="1,3", options="header"]
    |===
    | Attribute        | Description

    | `endpoint`       | `/locations/{locationId}/products/{productId}`
    | `method`         | `GET`
    | `description`    | Retrieves the current stock level for a specific product at a given location.
    | `authentication` | Requires a valid API token with appropriate permissions.
    | `response format`| JSON
    |===

    ==== Input Parameters

    The Get Stock Level for Product at Location endpoint accepts the following input parameters:

    [cols="1,4,1,2", options="header"]
    |===
    | Parameter              | Description                                                       | Required | Example

    | `locationId`           | The identifier for the location.                                  | Yes      | `"warehouse1"`
    | `productId`            | The unique identifier for the product.                            | Yes      | `"product123"`
    | `Authentication Token` | The API token used for authentication.                            | Yes      | `Bearer abc123xyz`
    |===

    ==== REST Endpoint Definition

    **Endpoint:**
    `GET /locations/{locationId}/products/{productId}`

    [source, http]
    .Request to retrieve stock level
    -----
    GET /locations/warehouse1/products/product123 HTTP/1.1
    Host: api.example.com
    Authorization: Bearer abc123xyz
    Accept: application/json
    -----

    ==== Example Response

    [source, http]
    .Successful response
    -----
    HTTP/1.1 200 OK
    Content-Type: application/json

    {
      "locationId": "warehouse1",
      "productId": "product123",
      "quantity": 150.5,
      "lastUpdated": "2024-12-26T15:35:00Z"
    }
    -----

    ==== Response Codes

    [cols="1,3", options="header"]
    |===
    | Code              | Description

    | `200 OK`          | Stock level successfully retrieved.
    | `400 Bad Request` | Invalid request data or missing required fields.
    | `401 Unauthorized`| Authentication token is missing or invalid.
    | `403 Forbidden`   | User does not have permission to access this data.
    | `404 Not Found`   | The specified location or product was not found.
    | `500 Internal Server Error` | An unexpected error occurred on the server.
    |===

    ==== Notes

    - **Authentication Requirement:** A valid API token with sufficient permissions is mandatory to use this endpoint.
    - **Precision in Quantities:** The `quantity` field is a double to support fractional quantities.

    The Get Stock Level for Product at Location endpoint provides a dependable method to monitor inventory and ensure operational consistency.

  Background:
    Given "warehouse1" is a Distribution Centre

  Scenario: Successfully retrieve the stock level of a product at a location
  This is a "happy path" test to ensure the endpoint returns the correct stock level for valid input.
    Given a product "product123" exists in "warehouse1" with a stock level of 150.5
    When I send a GET request to "/locations/warehouse1/products/product123"
    Then the API should respond with status code 200
    And the response body should contain:
      """asciidoc
      [source, json]
      -----
      {
          "locationId": "warehouse1",
          "productId": "product123",
          "quantity": 150.5,
          "lastUpdated": "<timestamp>" (1)
      }
      -----
      <1> timestamp in the ISO8601 format, e.g., 2024-12-26T15:35:00.123Z
      """

#  Scenario: Fail to retrieve stock level due to invalid product
#  This test ensures that the endpoint returns an appropriate error when the product does not exist.
#
#    Given "product456" does not exist in "warehouse1"
#    When I send a GET request to "/locations/warehouse1/products/product456"
#    Then the API should respond with status code 404
#    And the response body should contain:
#      """asciidoc
#      [source, json]
#      -----
#      {
#          "status": "ProductNotFound"
#      }
#      -----
#      """

  Scenario: Fail to retrieve stock level due to invalid location
  This test ensures that the endpoint returns an appropriate error when the location does not exist.

    Given "invalidLocation" does not exist as a Distribution Centre
    When I send a GET request to "/locations/invalidLocation/products/product123"
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

#  Scenario: Fail to retrieve stock level due to insufficient permissions
#  This test ensures that the endpoint returns a proper error response when the authentication token lacks necessary permissions.
#
#    Given I have an invalid or insufficient API token
#    When I send a GET request to "/locations/warehouse1/products/product123"
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
