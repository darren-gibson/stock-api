@section-TrackedAndUntrackedLocations @asciidoc

Feature: Tracked and Untracked Locations

  The Tracked and Untracked Locations feature defines how inventory is managed across various areas within an inventory location hierarchy. Locations can be designated as either tracked or untracked based on their role in the inventory system and operational needs.

  Tracked locations record all inventory movements and stock levels, providing detailed visibility and control. In contrast, untracked locations represent areas where stock can be placed but are not individually tracked for movements. This differentiation is essential for balancing operational efficiency with inventory visibility.

  The objective of this feature is to enable effective inventory management by assigning appropriate roles to locations, supporting both operational processes and system integrations.

  The Tracked and Untracked Locations feature ensures that:

  * Locations are clearly defined as either tracked or untracked.
  * The role of a location is determined by its parent location but can be adjusted locally.
  * Untracked locations contribute to overall stock visibility without tracking specific transactions.
  * Tracked locations provide comprehensive inventory control through movement and stock level tracking.

  === Characteristics of Tracked and Untracked Locations

  Locations in the inventory system have the following attributes:

  * *Role Assignment*: Locations can be assigned the role `TrackedInventoryLocation` in the Location API, indicating that they are tracked. Locations without this role are untracked by default.
  * *Inheritance*: A location's tracked/untracked setting is initially inherited from its parent location but can be changed to suit operational needs.
  * *Operational Capabilities*:
    - **Tracked Locations**: Fully capable of performing all stock transactions, including movements, deliveries, and adjustments.
    - **Untracked Locations**: Limited to observations and stock assertions. Most transactions are not supported.
  * *Error Handling for Unsupported Transactions*: Attempting unsupported transactions at an untracked location will result in:
    - A redirect to the nearest tracked parent location for processing.
    - An error response if no tracked parent exists.
  * *Stock Visibility*:
    - Tracked Locations: Maintain precise stock figures at the location level.
    - Untracked Locations: Stock levels are represented as the sum of all child tracked locations.

  === Importance of Tracked and Untracked Locations

  Differentiating between tracked and untracked locations adds value by:

  * Providing flexibility in inventory management based on operational needs.
  * Enhancing visibility into stock distribution across hierarchical locations.
  * Supporting efficient stocktaking and planning processes.

  === Use Cases for Tracked and Untracked Locations

  The following table outlines common scenarios for applying tracked and untracked locations:

  [cols="3,7", options="header"]
  |===
  | Use Case Category           | Description
  | Stock Management            a| * Assigning roles to locations to balance visibility and operational complexity.
  | Inventory Assertions        a| * Recording observations such as "no stock" or "low stock" at untracked locations.
  | Transaction Management      a| * Redirecting transactions from untracked locations to their nearest tracked parent.
  | Capacity Planning           a| * Documenting the maximum stock each location can hold to support replenishment and allocation planning.
  | Stock take Support          a| * Including both tracked and untracked locations in stock counting processes.
  |===

  === Key Elements of Tracked and Untracked Locations

  Locations in the inventory system include the following details:

  * *Name and Description*: A unique identifier and description for the location.
  * *Parent Location*: The higher-level location that the current location belongs to.
  * *TrackedInventoryLocation Role*: The role assignment indicating whether the location is tracked or untracked.
  * *Capacity*: The maximum amount of stock the location can hold.
  * *Assertions*: Any statements or observations about the stock level in the location.

  === Purpose

  The Tracked and Untracked Locations feature supports the following objectives:

  * Ensuring clear distinctions between tracked and untracked locations for effective inventory management.
  * Enhancing operational efficiency by assigning appropriate roles to locations.
  * Facilitating stock visibility, planning, and control at all levels of the location hierarchy.

  This functionality enables organisations to optimise inventory management processes, maintain control over stock levels, and enhance operational agility.

Scenario: Root
