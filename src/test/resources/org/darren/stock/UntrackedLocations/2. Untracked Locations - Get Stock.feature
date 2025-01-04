@section-TrackedAndUntrackedLocations @asciidoc @order-2
Feature: Contract: Get Stock Level for Product at Untracked Location Endpoint

    === Get Stock Level for Product at Untracked Location Endpoint Specification

    This feature describes the behavior of the `Get Stock Level for Product at Location` endpoint when querying untracked locations. For details on the general endpoint behavior, refer to the main Get Stock specification.

    When a location is untracked:

    * The endpoint aggregates stock levels from all tracked child locations.
    * The `quantity` field is not included in the response as untracked locations do not maintain a specific stock level.
    * The `totalQuantity` field represents the sum of all tracked child locations' `quantity` values. The `pendingAdjustment` field is for informational purposes only and does not affect `totalQuantity`.
    * If no tracked child locations exist, the `totalQuantity` will be 0.
    * The response includes details of all child locations and their respective stock levels.

    ==== Key Points

    - **Aggregated Stock Levels:** The endpoint provides a consolidated view of stock for untracked locations by summing the `quantity` values of tracked child locations.
    - **Child Location Details:** Includes all tracked child locations, their stock levels, and any `pendingAdjustment` values.
    - **Empty Child Locations:** If an untracked location has no tracked child locations, the `totalQuantity` is `0`, and the `childLocations` field is empty.
    - **Reference to Parent Specification:** For details on input parameters, authentication, and response codes, see the Get Stock specification.

    ==== Example Response

    [source, http]
    .Successful response for an untracked location with tracked child locations
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

    ==== Notes

    - **Untracked Location Behavior:** Untracked locations do not maintain individual stock levels (`quantity`) and rely on the aggregated stock data of their tracked child locations.
    - **Pending Adjustment:** The `pendingAdjustment` field does not affect the `totalQuantity` field. It is included only for informational purposes.
    - **Empty Child Locations:** If no child locations are tracked, the response reflects a `totalQuantity` of `0` and an empty `childLocations` array.

  Background:
    Given the following locations exist:
      | Location Id | Parent Location Id | Name                | Roles                    |
      | DC02        |                    | Distribution Centre |                          |
      | WH03        | DC02               | Warehouse           | TrackedInventoryLocation |
      | WH04        | DC02               | Warehouse           | TrackedInventoryLocation |
      | DC03        |                    | Distribution Centre |                          |

    And the following are the current stock levels:
      | Location Id | Product    | Stock Level | Pending Adjustment |
      | WH03        | product123 | 80.0        | -2.0               |
      | WH04        | product123 | 100.0       | 0.0                |

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
          "lastUpdated": "<timestamp>", (1)
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
      <1> timestamp in the ISO8601 format, e.g., 2024-12-26T15:35:00.123Z
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
          "lastUpdated": "<timestamp>" (1)
      }
      -----
      <1> timestamp in the ISO8601 format, e.g., 2024-12-26T15:35:00.123Z
      """
