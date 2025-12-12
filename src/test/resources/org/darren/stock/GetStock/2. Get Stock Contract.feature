@section-StockLevel @asciidoc @order-1
Feature: Contract: Get Stock Level for Product at Location Endpoint
  === Get Stock Level for Product at Location Endpoint Specification

  The Get Stock Level for Product at Location endpoint provides real-time visibility into the inventory of products in locations. This functionality is essential for maintaining accurate inventory data and supporting operational decisions.

  The endpoint supports both tracked and untracked locations:

  - **Tracked Locations:** Stock levels are directly associated with the location, the response can optionally include the the aggregated view of all tracked child locations.
  - **Untracked Locations:** Untracked locations do not maintain their own `quantity` therefore the result is the aggregated view of all tracked child locations.

  The response includes a `pendingAdjustment` field, representing any known discrepancies between the recorded stock and actual stock for tracked locations. This field is omitted for untracked locations.

  ==== Key Points

  - **Aggregated Stock:** the `totalQuantity` represents the sum of all tracked child locations' `quantity`.
  - **Quantity and Untracked Locations:** For untracked locations, the `quantity` and `pendingAdjustment` fields are omitted as stock is not maintained at these locations.
  - **Child Location Details:** For both tracked and untracked locations, the response optionally includes stock details for child locations.
  - **Pending Adjustment:** The `pendingAdjustment` field is included only for tracked locations and does not affect `totalQuantity`.

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
  | `includeChildren`      | A flag to specify whether to include stock levels from child locations. Defaults to `true`. | No | `false`
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
  .Successful response for a tracked location
  -----
  HTTP/1.1 200 OK
  Content-Type: application/json

  {
    "locationId": "DC01",
    "productId": "product123",
    "quantity": 150.5,
    "pendingAdjustment": -5.0,
    "totalQuantity": 261.0,
    "lastUpdated": "2024-12-26T15:35:00Z",
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
        "totalQuantity": 30.5
      }
    ]
  }
  -----

  [source, http]
  .Successful response for an untracked location
  -----
  HTTP/1.1 200 OK
  Content-Type: application/json

  {
    "locationId": "DC02",
    "productId": "product123",
    "totalQuantity": 180.0,
    "lastUpdated": "2024-12-26T15:35:00Z",
    "childLocations": [
      {
        "locationId": "WH03",
        "quantity": 80.0,
        "pendingAdjustment": -2.0,
        "totalQuantity": 80.0
      },
      {
        "locationId": "WH04",
        "quantity": 100.0,
        "totalQuantity": 100.0
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

  - **Untracked Location Behavior:** Untracked locations do not maintain a `quantity` field. Instead, `totalQuantity` represents the aggregate stock across all tracked child locations.
  - **Tracked Location Behavior:** Tracked locations include both `quantity` and `totalQuantity`, with `pendingAdjustment` for stock discrepancies.
  - **Empty Child Locations for Untracked Locations:** If no tracked child locations exist, `totalQuantity` is `0`, and `childLocations` is an empty array.

  Background:
    Given the following locations exist:
      | Location Id | Parent Location Id | Name                | Roles                    |
      | DC01        |                    | Distribution Centre | TrackedInventoryLocation |
      | WH01        | DC01               | Warehouse           | TrackedInventoryLocation |
      | SU01        | WH01               | Storage Unit        | TrackedInventoryLocation |
      | WH02        | DC01               | Warehouse           | TrackedInventoryLocation |
      | DC02        |                    | Distribution Centre |                          |
      | WH03        | DC02               | Warehouse           | TrackedInventoryLocation |
      | WH04        | DC02               | Warehouse           | TrackedInventoryLocation |
      | DC03        |                    | Distribution Centre |                          |

    And the following are the current stock levels:
      | Location Id | Product    | Stock Level | Pending Adjustment |
      | DC01        | product123 | 150.5       | -5.0               |
      | WH01        | product123 | 50.0        | -3.0               |
      | SU01        | product123 | 30.0        | 0.0                |
      | WH02        | product123 | 30.5        | -2.0               |
      | WH03        | product123 | 80.0        | -2.0               |
      | WH04        | product123 | 100.0       | 0.0                |

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
  Scenario: Successfully retrieve the stock level of a product at an untracked location
    This test ensures the endpoint aggregates stock levels for an untracked location with tracked child locations.
    When I send a GET request to "/locations/DC02/products/product123"
    Then the API should respond with status code 200
    And the response body should contain:
      """asciidoc
      [source, json]
      -----
      {
          "locationId": "DC02",
          "productId": "product123",
          "totalQuantity": 180.0,
          "lastUpdated": "<timestamp>",
          "childLocations": [
            {
              "locationId": "WH03",
              "quantity": 80.0,
              "pendingAdjustment": -2.0,
              "totalQuantity": 80.0
            },
            {
              "locationId": "WH04",
              "quantity": 100.0,
              "totalQuantity": 100.0
            }
          ]
      }
      -----
      """

  Scenario: Retrieve stock level for an untracked location with no child tracked locations
    This test ensures the endpoint handles untracked locations with no tracked child locations correctly.
    When I send a GET request to "/locations/DC03/products/product123"
    Then the API should respond with status code 200
    And the response body should contain:
      """asciidoc
      [source, json]
      -----
      {
          "locationId": "DC03",
          "productId": "product123",
          "totalQuantity": 0.0,
          "lastUpdated": "<timestamp>"
      }
      -----
      """

  Scenario: Retrieve stock level for an untracked location without children
  This test ensures the endpoint handles untracked locations handing the `includeChildren=false` flag correctly.  This will result in no `quantity` and not `totalQuantity` fields in the response.
    When I send a GET request to "/locations/DC03/products/product123?includeChildren=false"
    Then the API should respond with status code 200
    And the response body should contain:
      """asciidoc
      [source, json]
      -----
      {
          "locationId": "DC03",
          "productId": "product123",
          "lastUpdated": "<timestamp>"
      }
      -----
      """
