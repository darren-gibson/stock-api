@section-Status @asciidoc @order-1
Feature: Service Health Probes
  ==== Overview

  The service exposes three standard health probe endpoints for container lifecycle management.
  These probes follow the
  https://github.com/microprofile/microprofile-health[MicroProfile Health specification],
  which is the de facto standard used by cloud platforms and container orchestration systems
  for reporting liveness, readiness, and startup status.

  Each probe is a simple HTTP endpoint that returns:
  * A response status code (200 for UP, 503 for DOWN, 500 for processing errors)
  * A JSON body with overall `status` and an array of individual health check `checks`

  ==== Health Probe Endpoints

  * `GET /health/live` : Liveness Probe – Is the process alive and running?
  * `GET /health/ready` : Readiness Probe – Can the service accept traffic? Checks critical dependencies.
  * `GET /health/started` : Startup Probe – Has the application finished starting?

  Background:
    Given the service is running

  Scenario: Liveness probe reports UP when process is running
    When I send a GET request to the "/health/live" endpoint
    Then the response status code should be 200
    And the response body should match JSON:
      """asciidoc
      [source, json]
      -----
      {
        "status": "UP",
        "checks": []
      }
      -----
      """

  Scenario: Readiness probe reports UP when dependencies are healthy
    When I send a GET request to the "/health/ready" endpoint
    Then the response status code should be 200
    And the response body should match JSON:
      """asciidoc
      [source, json]
      -----
      {
        "status": "UP",
        "checks": []
      }
      -----
      """

  Scenario: Readiness probe reports DOWN when Location API is unavailable
    Given the Location API health check will fail
    When I send a GET request to the "/health/ready" endpoint
    Then the response status code should be 503
    And the response body should match JSON:
      """asciidoc
      [source, json]
      -----
      {
        "status": "DOWN",
        "checks": [
          {
            "name": "locationApi",
            "status": "DOWN"
          }
        ]
      }
      -----
      """

  Scenario: Startup probe reports UP when application is ready
    When I send a GET request to the "/health/started" endpoint
    Then the response status code should be 200
    And the response body should match JSON:
      """asciidoc
      [source, json]
      -----
      {
        "status": "UP",
        "checks": []
      }
      -----
      """
