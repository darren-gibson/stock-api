@section-Locations @asciidoc @order-1
Feature: Location Endpoint: Contract

#  Background:
#    Given the API is authenticated with a valid bearer token

  Scenario: define a location
  Successfully create a location providing the minimum mandatory information required to describe that location.  Locations require as a minimum
  [cols="1,1,1,4,2", options="header"]
  |===
  | Field             | Type       | Required   | Description                                                                                                      | Example

  | locationId        | string     | Yes        | A unique identifier for the location. This serves as the primary key for identifying the location in the system. | PK-001
  | name              | string     | Yes        | A human-readable name for the location, describing its purpose or position.                                      | Picking Zone 1
  | type              | string     | Yes        | The type of location being created, defining its role in the inventory system. Common types include: `Warehouse`, `Store`, `PickingZone`, `QuarantineArea`. | PickingZone
  | parentLocationId  | string     | No         | The identifier of an existing location under which the new location is nested. If omitted, the location is created as a primary location. | WH-001
  |===
    Given "Store-001" is a store
    And a product "SKU12345" exists in "Store-001" with a stock level of 50
    When I send a POST request to "/stores/Store-001/products/SKU12345/sales" with the following payload:
"""asciidoc
      [source, json]
      -----
      {
          "requestId": "abc123-e89b-12d3-a456-426614174000",
          "quantity": 5
      }
      -----
      """
    Then the API should respond with status code 201
    And the response body should contain:
"""asciidoc
      [source, json]
      -----
      {
        "requestId": "abc123-e89b-12d3-a456-426614174000",
        "location": "Store-001",
        "productId": "SKU12345",
        "quantitySold": 5.0,
        "saleTimestamp": "${json-unit.regex}^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d+$" (1)
      }
      -----
      <1> timestamp in the ISO8601 format, e.g: 2024-12-11T09:16:29.577617
      """
    And the stock level of product "SKU12345" in "Store-001" should be updated to 45
