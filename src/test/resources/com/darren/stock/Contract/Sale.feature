@section-Sales
@asciidoc @order-1
Feature: Sale Endpoint: Contract Test

  *Purpose*
  The Sale Endpoint must behave according to the contract described in the specification.  These tests exercise the API and are transparent about the inputs/outputs of the API to ensure that it behalves according to the specification.  If any of these tests fail, then the contract must have changed either the request/response structure or the underlying behaviour i.e. a breaking change.

  *Request*

  [cols="1,1,1,1,4,2", options="header"]
  |===
  | Field       | Type     | Location   | Required   | Description                                                   | Example

  | locationId  | string   | Path       | Yes        | The unique identifier of the location where the sale occurred.| Store-001
  | productId   | string   | Path       | Yes        | The unique identifier of the product being sold.              | Product-123
  | requestId   | string   | Body       | Yes        | A unique identifier for the sale request to ensure idempotency.| sale-001-12345
  | quantity    | number   | Body       | Yes        | The number of units of the product being sold.                | 5.0
  |===

  *Response*

  [cols="1,1,1,4,2", options="header"]
  |===
  | Variable       | Type     | Location   | Description                                                    | Example

  | requestId      | string   | Body       | The unique identifier for the sale request.                   | sale-001-12345
  | locationId     | string   | Body       | The unique identifier of the location where the sale occurred.| Store-001
  | productId      | string   | Body       | The unique identifier of the product sold.                    | Product-123
  | quantitySold   | integer  | Body       | The number of units of the product sold.                      | 5.0
  | saleTimestamp  | string   | Body       | The timestamp when the sale was recorded, in ISO 8601 format. | 2024-12-07T12:00:00Z
  |===

  *Specification*

  [source, yml]
  -----
  openapi: 3.0.3
  info:
    title: Stock API
    description: API to manage stock levels, including the recording of product sales.
    version: 1.0.0
  paths:
    /locations/{locationId}/products/{productId}/sales:
      post:
        summary: Record a sale of a product at a specific location
        description: Records the sale of a product at a given location and updates the stock level accordingly.
        operationId: recordSale
        parameters:
          - name: locationId
            in: path
            required: true
            description: The unique identifier of the location where the sale occurred.
            schema:
              type: string
              example: Store-001
          - name: productId
            in: path
            required: true
            description: The unique identifier of the product being sold.
            schema:
              type: string
              example: Product-123
        requestBody:
          required: true
          description: Details of the sale to be recorded.
          content:
            application/json:
              schema:
                type: object
                required:
                  - requestId
                  - quantity
                properties:
                  requestId:
                    type: string
                    description: A unique identifier for the sale request to ensure idempotency.
                    example: sale-001-12345
                  quantity:
                    type: number
                    description: The quantity of the product being sold.
                    minimum: 0.0001
                    example: 5.00
        responses:
          '201':
            description: Sale recorded successfully.
            content:
              application/json:
                schema:
                  type: object
                  properties:
                    requestId:
                      type: string
                      example: sale-001-12345
                    locationId:
                      type: string
                      example: Store-001
                    productId:
                      type: string
                      example: Product-123
                    quantitySold:
                      type: number
                      example: 5.0
                    saleTimestamp:
                      type: string
                      format: date-time
                      example: "2024-12-07T12:00:00Z"
          '400':
            description: Invalid request or insufficient stock.
            content:
              application/json:
                schema:
                  type: object
                  properties:
                    status:
                      type: string
                      enum: [InvalidRequest, InsufficientStock]
                      example: InsufficientStock
          '404':
            description: Product or location not found.
            content:
              application/json:
                schema:
                  type: object
                  properties:
                    status:
                      type: string
                      enum: [LocationNotFound, ProductNotFound]
                      example: LocationNotFound
        security:
          - bearerAuth: []
  components:
    securitySchemes:
      bearerAuth:
        type: http
        scheme: bearer
        bearerFormat: JWT
  -----


#  Background:
#    Given the API is authenticated with a valid bearer token

  Scenario: Successfully record a product sale and reduce stock level
    This is a "happy path" test to ensure that the Sale endpoint accepts a valid JSON request and reduces the stock as a result.
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

  Scenario: Fail to record a product sale due to an invalid location
  This test ensures that the Sale endpoint returns a proper error response when the specified location does not exist.
    Given "Invalid-Store" does not exist as a store
    When I send a POST request to "/stores/Invalid-Store/products/SKU12345/sales" with the following payload:
      """asciidoc
      [source, json]
      -----
      {
          "requestId": "abc123-e89b-12d3-a456-426614174001",
          "quantity": 5
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

#  Scenario: Handle duplicate sale request using the same requestId
#    Given "Store-001" is a store
#    And a product "SKU12345" exists in "Store-001" with a stock level of 45
#    And a sale request with "requestId": "abc123-e89b-12d3-a456-426614174000" has already been processed
#    When I resend the POST request to "/stores/Store-001/products/SKU12345/sales" with the same payload:
#      """
#      {
#          "requestId": "abc123-e89b-12d3-a456-426614174000",
#          "quantity": 5
#      }
#      """
#    Then the API should respond with status code 200
#    And the response body should contain:
#      """
#      {
#          "status": "duplicate_request",
#          "message": "The request has already been processed.",
#          "processedAt": "2024-12-07T12:00:00Z"
#      }
#      """
#    And the stock level of product "SKU12345" in "Store-001" should remain at 45
#
#  Scenario: Fail to record a sale due to insufficient stock
#    Given "Store-001" is a store
#    And a product "SKU12345" exists in "Store-001" with a stock level of 3
#    When I send a POST request to "/stores/Store-001/products/SKU12345/sales" with the following payload:
#      """
#      {
#          "requestId": "def456-e89b-12d3-a456-426614174000",
#          "quantity": 5
#      }
#      """
#    Then the API should respond with status code 400
#    And the response body should contain:
#      """
#      {
#          "status": "error",
#          "message": "Insufficient stock to complete the sale."
#      }
#      """
#    And the stock level of product "SKU12345" in "Store-001" should remain at 3
#
#  Scenario: Fail to record a sale due to invalid request payload
#    Given "Store-001" is a store
#    And a product "SKU12345" exists in "Store-001" with a stock level of 45
#    When I send a POST request to "/stores/Store-001/products/SKU12345/sales" with the following payload:
#      """
#      {
#          "quantity": 5
#      }
#      """
#    Then the API should respond with status code 400
#    And the response body should contain:
#      """
#      {
#          "status": "error",
#          "message": "Missing or invalid requestId"
#      }
#      """
