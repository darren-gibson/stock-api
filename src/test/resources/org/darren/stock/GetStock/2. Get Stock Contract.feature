@section-StockLevel @asciidoc @order-1
Feature: Contract: Get Stock Level for Product at Location Endpoint

    === Get Stock Level for Product at Location Endpoint Specification

    The Get Stock Level for Product at Location endpoint provides real-time visibility into the inventory of specific products at designated locations. This functionality is essential for maintaining accurate inventory data and supporting operational decisions.

    In addition to stock levels, the endpoint now includes a `pendingAdjustment` field, representing any known discrepancies between the recorded stock and actual stock that have not yet been reconciled. If no discrepancies exist, the `pendingAdjustment` field is omitted from the response. This ensures transparency in stock anomalies and enhances inventory accuracy.

    [cols="1,3", options="header"]
    |===
    | Attribute        | Description

    | `endpoint`       | `/locations/{locationId}/products/{productId}`
    | `method`         | `GET`
    | `description`    | Retrieves the current stock level for a specific product at a given location, including child locations by default.
    | `authentication` | Requires a valid API token with appropriate permissions.
    | `response format`| JSON
    |===

    ==== Input Parameters

    The Get Stock Level for Product at Location endpoint accepts the following input parameters:

    [cols="1,4,1,2", options="header"]
    |===
    | Parameter              | Description                                                       | Required | Example

    | `locationId`           | The identifier for the location.                                  | Yes      | `"DC01"`
    | `productId`            | The unique identifier for the product.                            | Yes      | `"product123"`
    | `includeChildren`      | A flag to specify whether to include stock levels from child locations. Defaults to true. | No | `false`
    | `Authentication Token` | The API token used for authentication.                            | Yes      | `Bearer abc123xyz`
    |===

    ==== REST Endpoint Definition

    **Endpoint:**
    `GET /locations/{locationId}/products/{productId}`

    [source, http]
    .Request to retrieve stock level
    -----
    GET /locations/DC01/products/product123?includeChildren=true HTTP/1.1
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
      "locationId": "DC01",
      "productId": "product123",
      "quantity": 150.5,
      "totalQuantity": 230.0,
      "lastUpdated": "2024-12-26T15:35:00Z",
      "childLocations": [
        {
          "locationId": "WH01",
          "quantity": 50.0,
          "pendingAdjustment": 3.0,
          "totalQuantity": 80.0,
          "childLocations": [
            {
              "locationId": "SU01",
              "quantity": 30.0,
              "totalQuantity": 30.0
            }
          ]
        },
        {
          "locationId": "WH02",
          "quantity": 30.5,
          "totalQuantity": 30.5
        }
      ]
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

    - **`pendingAdjustment` Field:** Represents any unrecorded or known discrepancies in the stock level. If no adjustments are pending, this field is omitted from the response.
    - **Authentication Requirement:** A valid API token with sufficient permissions is mandatory to use this endpoint.
    - **Precision in Quantities:** The `quantity` and `pendingAdjustment` fields are doubles to support fractional values.
    - **Hierarchy Support:** By default, the endpoint includes stock levels for child locations. Set `includeChildren=false` to limit results to the specified location only.

    The Get Stock Level for Product at Location endpoint provides a dependable method to monitor inventory and ensure operational consistency.

  Background:
    Given the following locations exist:
      | Location Id      | Parent Location Id | Role               |
      | DC01             |                    | Distribution Centre|
      | WH01             | DC01               | Warehouse          |
      | SU01             | WH01               | Storage Unit       |
      | WH02             | DC01               | Warehouse          |

    And the following are the current stock levels:
      | Location Id | Product    | Stock Level | Pending Adjustment |
      | DC01        | product123 | 150.5       | -5.0               |
      | WH01        | product123 | 50.0        | -3.0               |
      | SU01        | product123 | 30.0        | 0.0                |
      | WH02        | product123 | 30.5        | -2.0               |

  Scenario: Successfully retrieve the stock level of a product at a location
  This is a "happy path" test to ensure the endpoint returns the correct stock level and pending adjustments for valid input.
    When I send a GET request to "/locations/DC01/products/product123"
    Then the API should respond with status code 200
    And the response body should contain:
      """asciidoc
      [source, json]
      -----
      {
          "locationId": "DC01",
          "productId": "product123",
          "quantity": 150.5,
          "pendingAdjustment": -5.0,
          "totalQuantity": 261.0,
          "lastUpdated": "<timestamp>",
          "childLocations": [
            {
              "locationId": "WH01",
              "quantity": 50.0,
              "pendingAdjustment": -3.0,
              "totalQuantity": 80.0,
              "childLocations": [
                {
                  "locationId": "SU01",
                  "quantity": 30.0,
                  "totalQuantity": 30.0
                }
              ]
            },
            {
              "locationId": "WH02",
              "quantity": 30.5,
              "pendingAdjustment": -2.0,
              "totalQuantity": 30.5
            }
          ]
      }
      -----
      <1> timestamp in the ISO8601 format, e.g., 2024-12-26T15:35:00.123Z
      """

  Scenario: Retrieve the stock level only for the specific location without child locations
  This test ensures the endpoint respects the `includeChildren=false` parameter and still returns `pendingAdjustment` if applicable.
    When I send a GET request to "/locations/DC01/products/product123?includeChildren=false"
    Then the API should respond with status code 200
    And the response body should contain:
      """asciidoc
      [source, json]
      -----
      {
          "locationId": "DC01",
          "productId": "product123",
          "quantity": 150.5,
          "pendingAdjustment": -5.0,
          "lastUpdated": "<timestamp>"
      }
      -----
      <1> timestamp in the ISO8601 format, e.g., 2024-12-26T15:35:00.123Z
      """

#  Scenario: Fail to retrieve stock level due to invalid product
#  This test ensures that the endpoint returns an appropriate error when the product does not exist.
#
#    Given "product456" does not exist in "DC01"
#    When I send a GET request to "/locations/DC01/products/product456"
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
#    When I send a GET request to "/locations/DC01/products/product123"
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
