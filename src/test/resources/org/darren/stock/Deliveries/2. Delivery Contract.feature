@section-Delivery @asciidoc @order-1
Feature: Contract: Record Delivery Endpoint

    === Record Delivery Endpoint Specification

    The Record Delivery Endpoint enables the accurate logging of product deliveries from external suppliers into a location within the network. This functionality ensures seamless integration of incoming inventory into the system, supporting real-time stock updates and maintaining operational efficiency.

    [cols="1,3", options="header"]
    |===
    | Attribute        | Description

    | `endpoint`       | `/locations/{locationId}/deliveries`
    | `method`         | `POST`
    | `description`    | Records a delivery of products from an external supplier into a specified receiving location.
    | `authentication` | Requires a valid API token with appropriate permissions.
    | `request format` | JSON
    | `response format`| JSON
    |===

    ==== Input Parameters

    The Record Delivery Endpoint accepts the following input parameters:

    [cols="1,4,1,2", options="header"]
    |===
    | Parameter              | Description                                                       | Required | Example

    | `locationId`           | The identifier for the receiving location within a warehouse (URI path). | Yes      | `"warehouse1-receiving"`
    | `requestId`            | A unique identifier for the request to ensure idempotency.        | Yes      | `"req-123456789"`
    | `supplierId`           | The unique identifier for the external supplier.                 | Yes      | `"supplier123"`
    | `supplierRef`          | A reference provided by the supplier for the delivery.           | No       | `"supplier-order-9876"`
    | `deliveredAt`          | The date and time the delivery actually occurred in ISO8601 format. | Yes      | `"2024-12-26T15:35:00Z"`
    | `products`             | A list of products included in the delivery, including quantity. | Yes      | `[{"productId": "product456", "quantity": 100}]`
    | `Authentication Token` | The API token used for authentication.                            | Yes      | `Bearer abc123xyz`
    |===

    ==== REST Endpoint Definition

    **Endpoint:**
    `POST /locations/{locationId}/deliveries`

    [source, http]
    .Request to record a delivery
    -----
    POST /locations/warehouse1-receiving/deliveries HTTP/1.1
    Host: api.example.com
    Authorization: Bearer abc123xyz
    Content-Type: application/json
    Accept: application/json

    {
        "requestId": "req-123456789",
        "supplierId": "supplier123",
        "supplierRef": "supplier-order-9876",
        "deliveredAt": "2024-12-26T15:35:00Z",
        "products": [
            {
                "productId": "product456",
                "quantity": 100
            }
        ]
    }
    -----

    ==== Example Response

    [source, http]
    .Successful response
    -----
    HTTP/1.1 201 Created
    Content-Type: application/json
    -----

    ==== Response Codes

    [cols="1,3", options="header"]
    |===
    | Code              | Description

    | `201 Created`     | Delivery successfully recorded.
    | `400 Bad Request` | Invalid request data or missing required fields.
    | `401 Unauthorized`| Authentication token is missing or invalid.
    | `403 Forbidden`   | User does not have permission to access this functionality.
    | `409 Conflict`    | A delivery with the same `requestId` has already been processed.
    | `500 Internal Server Error` | An unexpected error occurred on the server.
    |===

    ==== Notes

    - **Idempotency Requirement:** The `requestId` ensures that duplicate submissions of the same request are handled gracefully.
    - **Authentication Requirement:** A valid API token with sufficient permissions is mandatory to use this endpoint.
    - **Supplier Verification:** The `supplierId` must correspond to a registered external supplier.
    - **Delivery ID Generation:** A unique identifier is assigned to each recorded delivery for tracking purposes.

    The Record Delivery Endpoint provides a reliable way to document and track incoming deliveries, ensuring accurate inventory management and smooth operations.

  Background:
    Given "supplier123" is a registered supplier
    And "warehouse1-receiving" is a receiving location in the network

  Scenario: Successfully record a delivery with multiple products
    Given "warehouse1-receiving" is expecting the following deliveries from "supplier123":
      | Product ID  | Quantity |
      | product456  | 100      |
      | product789  | 50       |

    When I send a POST request to "/locations/warehouse1-receiving/deliveries" with the following payload:
      """asciidoc
      [source, json]
      -----
      {
          "requestId": "req-987654321",
          "supplierId": "supplier123",
          "supplierRef": "supplier-order-1234",
          "deliveredAt": "2024-12-26T15:35:00Z",
          "products": [
              {
                  "productId": "product456",
                  "quantity": 100
              },
              {
                  "productId": "product789",
                  "quantity": 50
              }
          ]
      }
      -----
      """
    Then the API should respond with status code 201
    And the stock level of "product456" in "warehouse1-receiving" should be updated to 100
    And the stock level of "product789" in "warehouse1-receiving" should be updated to 50

  Scenario: Fail to record a delivery due to invalid location
    Given "invalidLocation" does not exist as a location
    When I send a POST request to "/locations/invalidLocation/deliveries" with the following payload:
      """asciidoc
      [source, json]
      -----
      {
          "requestId": "req-555555555",
          "supplierId": "supplier123",
          "supplierRef": "supplier-invoice-9999",
          "deliveredAt": "2024-12-26T15:35:00Z",
          "products": [
              {
                  "productId": "product456",
                  "quantity": 100
              }
          ]
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

  Scenario: Fail to record a delivery due to missing required fields
    When I send a POST request to "/locations/warehouse1-receiving/deliveries" with the following payload:
      """asciidoc
      [source, json]
      -----
      {
          "supplierId": "supplier123",
          "supplierRef": "xxx",
          "products": []
      }
      -----
      """
    Then the API should respond with status code 400
    And the response body should contain:
      """asciidoc
      [source, json]
      -----
      {
          "missingFields": ["requestId", "deliveredAt"]
      }
      -----
      """

  Scenario: Fail to record a delivery due to invalid `deliveredAt` format
    When I send a POST request to "/locations/warehouse1-receiving/deliveries" with the following payload:
      """asciidoc
      [source, json]
      -----
      {
          "requestId": "req-999999999",
          "supplierId": "supplier123",
          "supplierRef": "supplier-invalid-ref",
          "deliveredAt": "invalid-date-format",
          "products": [
              {
                  "productId": "product456",
                  "quantity": 100
              }
          ]
      }
      -----
      """
    Then the API should respond with status code 400
    And the response body should contain:
      """asciidoc
      [source, json]
      -----
      {
          "invalidValues": ["deliveredAt"]
      }
      -----
      """

#  Scenario: Fail to record a delivery due to invalid values
#    When I send a POST request to "/locations/warehouse1-receiving/deliveries" with the following payload:
#      """asciidoc
#      [source, json]
#      -----
#      {
#          "requestId": "req-999999999",
#          "supplierId": "supplier123",
#          "supplierRef": "supplier-invalid-ref",
#          "deliveryDate": "invalid-date-format",
#          "products": [
#          ]
#      }
#      -----
#      """
#    Then the API should respond with status code 400
#    And the response body should contain:
#      """asciidoc
#      [source, json]
#      -----
#      {
#          "invalidValues": ["deliveryDate", "products"]
#      }
#      -----
#      """
