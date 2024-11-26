Feature: Locations are nested in a hierarchy

#  # Stock Holding Locations: Tracked vs Untracked Locations
#  In the context of a retail environment, such as a shop with shelves as part of an overall retail estate,
#  stock holding locations can be categorised into `tracked` locations and `untracked` locations.
#
#  ## 1. Tracked Locations
#  These are physical or logical locations where every stock movement is meticulously recorded within the
#  inventory management system. They allow for real-time visibility and control over stock levels.
#
#  ### Retail Example:
#  - Backroom Stockroom: A dedicated space in the shop where new inventory is stored before it is placed on the shelves.
#    Every item entering or leaving the stockroom is logged.
#  - Distribution Centre (part of the estate): A centralised warehouse where the retailer’s overall stock is held.
#    The system tracks every dispatch or receipt of goods to and from this location.
#  - Online Fulfilment Zone (Logical Tracked Location): A portion of the shop's inventory allocated for online orders.
#    This area is tracked separately to ensure that online and in-store stocks remain accurate.
#
#  Key Features:
#  - Ensures complete accountability for stock.
#  - Required for financial and operational reporting.
#  - Supports replenishment planning and stock audits.
#
#  ## 2. Untracked Locations
#  These are areas where stock can be temporarily held or stored without formal tracking of individual movements.
#  These locations are used for simplicity and operational convenience, rather than for ongoing stock control.
#
#  ### Retail Example:
#  - Shop Floor Shelves: Once stock is placed on the shelves for customers to purchase, it is no longer tracked individually
#    in the system. Stock counts here are typically done manually during periodic checks or when items are restocked.
#  - Transit Area (Logical Location): When goods are being moved from the stockroom to the shop floor, they might pass
#    through a staging area where they are not formally tracked.
#  - Customer Returns Hold Area: Items awaiting inspection or re-shelving are temporarily held here, without detailed
#    tracking in the system.
#
#  Key Features:
#  - Used for convenience in areas where granular tracking adds little value.
#  - Stock visibility is lower, often requiring manual checks or approximations.
#  - Suitable for operational zones with high stock turnover or temporary storage.
#
#  # Summary
#  In a retail context:
#  - Tracked Locations (e.g., stockrooms and distribution centres) provide a high level of visibility and control,
#    essential for accurate inventory management.
#  - Untracked Locations (e.g., shop floor shelves and transit areas)
#    simplify operations by focusing on the flow and availability of stock rather than its precise tracking.
#
#  This dual approach ensures both efficiency and control, with tracked locations supporting centralised stock
#  visibility and untracked locations facilitating flexible, customer-facing operations.
#
#  Estate (untracked)
#  ├── Stores (untracked)
#  │   ├── South East (untracked)
#  │   │   ├── Cambridge (tracked)
#  │   │   │   ├── Backstage 1 (untracked)
#  │   │   │   ├── Shop Floor 2 (untracked)
#  │   │   │   │   ├── Shelf-a (untracked)
#  │   │   │   │   └── Shelf-b (untracked)
#  │   │   ├── Royston (tracked)
#  │   │   │   ├── Backstage 2 (untracked)
#  │   │   │   ├── Shop Floor 2 (untracked)
#  │   │   │   │   ├── Shelf-c (untracked)
#  │   │   │   │   └── Shelf-d (untracked)
#  └── Distribution Centres (untracked)
#      └── Castle Donington (tracked)
#          └── Location-a (untracked)

  Background: A store has multiple locations that a product can be held
    Given the following locations exit:
      | location     | parentLocation | type      |
      | Estate       |                | untracked |
      | Stores       | Estate         | untracked |
      | South East   | Stores         | untracked |
      | Cambridge    | South East     | tracked   |
      | Backstage 1  | Cambridge      | untracked |
      | Shop floor 1 | Cambridge      | untracked |
      | Shelf-a      | Shop floor 1   | untracked |
      | Shelf-b      | Shop floor 1   | untracked |
      | Royston      | South East     | tracked   |
      | Backstage 2  | Royston        | untracked |
      | Shop floor 2 | Royston        | untracked |
      | Shelf-c      | Shop floor 2   | untracked |
      | Shelf-d      | Shop floor 2   | untracked |

  Scenario: the stock in store includes the stock on the shelves
    Given the stock level of Beans in Shelf-a is 10
    And the stock level of Beans in Shelf-b is 20
    Then the current stock level of Beans in Cambridge store will equal 30

  Scenario: ensure the the stock levels in other stores don't affect this store
    Given the stock level of Beans in Shelf-a is 5
    And the stock level of Beans in Shelf-b is 7
    And the stock level of Beans in Shelf-c is 99
    Then the current stock level of Beans in Cambridge store will equal 12

  Scenario: Sales occur at the Store Level and don't know which shelf the customer has picked up the product from.
            Therefore the product may have been removed from either Shelf.  The current stock level of the Store is
            decremented by one, but the stock level on the Shelf is no longer clear.

#    Given the stock level of Beans in location-1 is 10
#    And the stock level of Beans in location-2 is 20
#    When there is a sale of 5 Beans in the Cambridge store
#    Then the current stock level of Beans in Cambridge store will equal 25
#    And the current stock level of Beans in location-1 will be between 5 and 10


