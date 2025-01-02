@section-Architecture @asciidoc
Feature: Architecture Overview - Location API
  ==== Overview
  The Stock API utilises the Location API to maintain all location-related data. This includes the definition of new locations, retrieval of location details, and the assignment of roles to locations. The Location API is designed to support a hierarchical structure of locations, enabling businesses to map their physical, virtual, and logical locations accurately within the system.

  The Stock API stores only Location-Id's and depends on the Location API to provide detailed information about each location. This separation of concerns ensures that the Stock API remains lightweight and focused on stock-related operations, while the Location API manages the complexity of location management.

  Scenario: The cache-control header in the responses from the get location endpoint is honoured by the stock API
    The Location API controls the duration that a location response can be cached by the Stock API. This ensures that the Stock API always has access to the most up-to-date location information when required.
    Given the Location API responds to get location requests with the following cache-control header:
      | Location Id | Cache-Control |
      | location1   | max-age=3600  |
      | location2   | no-cache      |
    When I get the stock level for "Beans" in "location1" multiple times
    Then the Location API should have been called no more than once for "location1"
    When I get the stock level for "Beans" in "location2" multiple times
    Then the Location API should have been called more than once for "location2"