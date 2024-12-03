Feature: Service Health Check
  As a user of the service
  I want to verify the health status of the service
  So that I know the service is running and operational

  Scenario: Verify the _status API endpoint returns 200 OK
    Given the service is running
    When I send a GET request to the "_status" endpoint
    Then the response status code should be 200
    And the response body should indicate the service is healthy
