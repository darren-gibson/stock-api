@section-Locations @asciidoc
Feature: Location Endpoint

  ==== Purpose
  In the Stock API, locations are central to the management and tracking of inventory. Locations represent any physical or logical place where stock is stored, processed, or moved, ranging from high-level distribution centres to more granular sub-locations such as picking locations within warehouses. The hierarchical structure of locations allows for precise tracking of stock and supports diverse operational needs.

  ==== Hierarchical Structure of Locations
  1. Primary Locations:
      * Represent high-level entities such as distribution centres, retail stores, or warehouses.
      * Serve as the main containers for stock-related operations and transactions.
  2. Nested Locations:
      * Locations can contain *sub-locations* or *nested locations* to represent more specific areas within the primary location.
      * Examples include:
          ** Picking Locations: Zones within a warehouse where stock is picked for orders.
          ** Storage Racks: Physical shelving units in a warehouse.
          ** Store Sections: Departments or shelves within a retail store (e.g., "Aisle 3, Shelf 2").
          ** Temporary Locations: Zones for stock awaiting processing, such as a quarantine area for damaged goods.

  ==== Stock and Nested Locations
  *Stock Organisation*

  Nested locations provide finer granularity for stock management. For example a warehouse (primary location) may have multiple picking locations, each holding specific stock for faster order fulfilment.
  This structure allows businesses to track where specific units of stock are stored within a larger location.

  *Stock Consolidation*

  The total stock for a product at a primary location includes the sum of all nested locations.  Businesses can manage stock at both the high level (e.g., total stock in a warehouse) and the granular level (e.g., stock in each picking zone).

  Use Cases for Nested Locations:

  * Order Fulfilment: Quickly locate stock within picking zones.
  Inventory Optimisation: Track and balance stock across sub-locations.
  * Quality Control: Isolate defective or damaged stock in specific quarantine zones.
  * Hierarchical Location Relationships

  The following diagram illustrates how nested locations fit within a primary location:

  [Source, plaintext]
  -----
  Primary Location: Warehouse (WH-001)
  +--------------------------------------------------+
  | Picking Zone 1 (PK-001)                          |
  |   - Product A: 50 units                          |
  |   - Product B: 30 units                          |
  |                                                  |
  | Picking Zone 2 (PK-002)                          |
  |   - Product A: 20 units                          |
  |   - Product C: 40 units                          |
  |                                                  |
  | Quarantine Area (QA-001)                         |
  |   - Product A (damaged): 5 units                 |
  +--------------------------------------------------+

  Primary Location Stock Summary:
  - Product A: 75 units (PK-001: 50, PK-002: 20, QA-001: 5)
  - Product B: 30 units (PK-001: 30)
  - Product C: 40 units (PK-002: 40)
  -----

  In this structure:

  Each nested location tracks its own stock independently. The primary location aggregates stock across all nested locations for reporting and management.

  ==== Operational Benefits
  1. Enhanced Visibility:
      * Nested locations provide a more detailed view of stock, enabling precise inventory tracking.
      * Businesses can identify where specific items are stored within a larger facility.
  2. Optimised Operations:
      * Picking locations streamline order fulfilment by organising stock in easily accessible zones.
      * Sub-locations for damaged goods or returns ensure operational efficiency without contaminating usable stock.
  3. Scalability:
      * Hierarchical locations support businesses as they expand, allowing the system to handle increasing complexity in inventory management.
  4. Audit and Reconciliation:
      * Nested locations improve the accuracy of audits by narrowing discrepancies to specific sub-locations.
      * Reconciliation efforts are simplified as stock movements can be traced at a granular level.

  ==== Key Takeaways
  * Locations in the Stock API are hierarchical, allowing for both high-level and granular management of inventory.
  * Nested locations represent specific areas within a primary location, such as picking zones, storage racks, or quarantine areas.
  * This structure supports precise tracking, operational efficiency, and better decision-making.

  By defining both primary and nested locations, inventory can be managed with greater control and flexibility.

  Scenario: Root
