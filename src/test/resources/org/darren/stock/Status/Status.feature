@section-Status @asciidoc @order-1
Feature: Service Health Check
  As a user of the service
  I want to verify the health status of the service
  So that I know the service is running and operational

  Scenario: Verify the _status API endpoint returns 200 OK
    Given the service is running
    When I send a GET request to the "_status" endpoint
    Then the response status code should be 200
    And the response body should indicate the service is healthy

  Scenario: Status endpoint exposes version metadata
    Given the service is running
    When I send a GET request to the "/_status" endpoint
    Then the response status code should be 200
    And the response body should indicate the service is healthy
    And the response body should include version and build time metadata
    And the response body should match JSON:
      """
      {
        "status": "Healthy",
        "version": "${json-unit.any-string}",
        "buildTime": "<timestamp>"
      }
      """

  Scenario: Status endpoint reports unhealthy when Location API is down
    Given the service is running
    And the Location API health check will fail
    When I send a GET request to the "/_status" endpoint
    Then the response status code should be 503
    And the response body should indicate the service is unhealthy
    And the response body should include version and build time metadata
