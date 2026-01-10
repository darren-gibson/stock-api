@section-Architecture @asciidoc @order-6
Feature: Architecture Overview - External API Resilience (Retry + Fast-Fail)
  ==== Overview
  The Stock API depends on external services, particularly the Location API, to fulfill requests. To protect both the Stock API and downstream systems from transient failures and cascading failures, the system implements resilience patterns for external API calls.

  **Rationale**

  * *Protect downstream systems*: Prevent overwhelming external services with repeated requests when they are experiencing issues
  * *Improve reliability*: Automatically recover from transient network errors and temporary service unavailability
  * *Fail fast*: When an external service is consistently unavailable, fail quickly rather than waiting for timeouts
  * *Graceful degradation*: Provide predictable error responses to clients when external dependencies are unavailable

  **Configuration Parameters**

  The resilience behavior is controlled by parameters in `application.yaml` and can be configured per external API. Durations use `kotlin.time.Duration` parseable strings (e.g., `50ms`, `2s`). Example keys for the Location API:

  * `resilience.apis.location.backoff.maxAttempts`: Maximum retry attempts for failed requests (default: 3)
  * `resilience.apis.location.backoff.initialDelay`: Initial retry delay (default: `100ms`)
  * `resilience.apis.location.backoff.maxDelay`: Maximum retry delay (default: `2s`)
  * `resilience.apis.location.backoff.multiplier`: Exponential backoff multiplier (default: 2.0)
  * `resilience.apis.location.failFast.failureThreshold`: Consecutive failures before failing fast (default: 5)
  * `resilience.apis.location.failFast.quietPeriod`: Period during which requests fail fast before a test request (default: `60s`)
  * `resilience.apis.location.failFast.testAfterQuietPeriod`: Whether a single test request is allowed after the `quietPeriod` (default: true)

  ==== Observable Behavior

  Background:
    Given a clean stock system
    And the identity provider is available
    And I have a valid authentication token with job "Store Stock Controller"

  Scenario: Transient failures are automatically retried
  The retry mechanism automatically retries failed requests according to configured parameters, allowing the system to recover from temporary network issues or service hiccups without failing the operation.

    Given resilience for the Location API is configured as:
      | Setting                                       | Value |
      | resilience.apis.location.backoff.maxAttempts  | 3     |
      | resilience.apis.location.backoff.initialDelay | 50ms  |
      | resilience.apis.location.backoff.maxDelay     | 200ms |
      | resilience.apis.location.backoff.multiplier   | 2.0   |
    And "store1" is a tracked location
    And the Location API will fail 2 times then succeed for location "store1"
    When I get the stock level for "Beans" in "store1"
    Then the response should be successful
    And the Location API should have been called 3 times for "store1"

  Scenario: Requests fail after exhausting all retry attempts
  After retries are exhausted without success, the system fails the request rather than continuing indefinitely, preventing resource exhaustion and providing timely feedback to clients.

    Given resilience for the Location API is configured as:
      | Setting                                       | Value |
      | resilience.apis.location.backoff.maxAttempts  | 3     |
      | resilience.apis.location.backoff.initialDelay | 50ms  |
      | resilience.apis.location.backoff.maxDelay     | 200ms |
      | resilience.apis.location.backoff.multiplier   | 2.0   |
    And "store2" is a tracked location
    And the Location API will always fail for location "store2"
    When I attempt to get the stock level for "Beans" in "store2"
    Then the response should fail with status 502
    And the Location API should have been called 3 times for "store2"

  Scenario: Fail-fast prevents overwhelming a failing external service
  After an external service experiences sustained failures, the fail-fast mechanism stops sending requests for a period to give the service time to recover. This protects the external service from being overwhelmed and allows the Stock API to fail fast.

    Given resilience for the Location API is configured as:
      | Setting                                            | Value |
      | resilience.apis.location.backoff.maxAttempts       | 1     |
      | resilience.apis.location.failFast.failureThreshold | 3     |
      | resilience.apis.location.failFast.quietPeriod      | 1s    |
    And "store3" is a tracked location
    And the Location API will always fail for location "store3"
    When I attempt to get the stock level for "Beans" in "store3"
    And I attempt to get the stock level for "Beans" in "store3"
    And I attempt to get the stock level for "Beans" in "store3"
    Then subsequent requests should fail fast without contacting the Location API
    And the logs should contain evidence of circuit breaker state change
    When I attempt to get the stock level for "Beans" in "store3"
    Then the response should fail immediately without calling the Location API
    And the total Location API calls for "store3" should be 3

  Scenario: Fail-fast period ends and testing allows recovery
  After the fail-fast period (`quietPeriod`) expires, the system allows a single test request. If it succeeds, normal operation resumes; if it fails, fail-fast starts again.

    Given resilience for the Location API is configured as:
      | Setting                                                | Value |
      | resilience.apis.location.backoff.maxAttempts           | 1     |
      | resilience.apis.location.failFast.failureThreshold     | 2     |
      | resilience.apis.location.failFast.quietPeriod          | 150ms |
      | resilience.apis.location.failFast.testAfterQuietPeriod | true  |
    And "store4" is a tracked location
    And the Location API will fail 2 times then succeed for location "store4"
    When I attempt to get the stock level for "Beans" in "store4"
    And I attempt to get the stock level for "Beans" in "store4"
    Then subsequent requests should fail fast without contacting the Location API
    And the logs should contain evidence of circuit breaker opening
    When I wait 200 milliseconds
    And I get the stock level for "Beans" in "store4"
    Then subsequent requests should succeed normally
    And the logs should contain evidence of circuit breaker recovery

  Scenario: Retry delays use exponential backoff
  To avoid overwhelming external services during recovery, retry delays increase exponentially up to a configured maximum. This gives the external service progressively more time to recover between retry attempts.

    Given resilience for the Location API is configured as:
      | Setting                                       | Value |
      | resilience.apis.location.backoff.maxAttempts  | 4     |
      | resilience.apis.location.backoff.initialDelay | 100ms |
      | resilience.apis.location.backoff.maxDelay     | 500ms |
      | resilience.apis.location.backoff.multiplier   | 2.0   |
    And "store5" is a tracked location
    And the Location API will fail 3 times then succeed for location "store5"
    When I get the stock level for "Beans" in "store5"
    Then the retry delays should approximately follow the pattern: "100ms, 200ms, 400ms"
    And the total request time should be less than "1500ms"


  Scenario: Network timeouts trigger retries and contribute to circuit breaker
  Network timeouts are transient failures that should be retried. When they occur repeatedly, they contribute to circuit breaker activation just like 5xx errors, protecting the system from hanging connections.

    Given resilience for the Location API is configured as:
      | Setting                                            | Value |
      | resilience.apis.location.backoff.maxAttempts       | 3     |
      | resilience.apis.location.backoff.initialDelay      | 50ms  |
      | resilience.apis.location.backoff.maxDelay          | 200ms |
      | resilience.apis.location.backoff.multiplier        | 2.0   |
      | resilience.apis.location.failFast.failureThreshold | 3     |
      | resilience.apis.location.failFast.quietPeriod      | 1s    |
    And "store6" is a tracked location
    And the Location API will timeout 2 times then succeed for location "store6"
    When I get the stock level for "Beans" in "store6"
    Then the response should be successful
    And the Location API should have been called 3 times for "store6"

  Scenario: Independent requests fail
  After retries are exhausted without success, the system fails the request rather than continuing indefinitely, preventing resource exhaustion and providing timely feedback to clients.

    Given resilience for the Location API is configured as:
      | Setting                                            | Value |
      | resilience.apis.location.backoff.maxAttempts       | 1     |
      | resilience.apis.location.backoff.initialDelay      | 50ms  |
      | resilience.apis.location.backoff.maxDelay          | 200ms |
      | resilience.apis.location.backoff.multiplier        | 2.0   |
      | resilience.apis.location.failFast.failureThreshold | 3     |
      | resilience.apis.location.failFast.quietPeriod      | 1s    |
    And "store2" is a tracked location
    And the Location API will always fail for location "store2"
    When I attempt to get the stock level for "Beans" in "store2"
    Then the response should fail with status 502
    When I attempt to get the stock level for "Beans" in "store2"
    Then the response should fail with status 502
    When I attempt to get the stock level for "Beans" in "store2"
    Then the response should fail with status 502
    When I attempt to get the stock level for "Beans" in "store2"
    Then the response should fail with status 502
    When I attempt to get the stock level for "Beans" in "store2"
    When I attempt to get the stock level for "Beans" in "store2"
    When I attempt to get the stock level for "Beans" in "store2"
    When I attempt to get the stock level for "Beans" in "store2"
    When I attempt to get the stock level for "Beans" in "store2"
    Then the response should fail with status 502
    And the Location API should have been called 3 times for "store2"
    And the logs should contain evidence of retries and circuit breaker opening