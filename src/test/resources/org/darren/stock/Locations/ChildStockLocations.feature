@section-Locations @asciidoc
Feature: Locations are nested in a hierarchy

  *Stock Holding Locations: Tracked vs Untracked Locations*
  In the context of a retail environment, such as a shop with shelves as part of an overall retail estate,
  stock holding locations can be categorised into `tracked` and `Untracked` locations.

  1. Tracked Locations
  These are physical or logical locations where every stock movement is meticulously recorded within the
  inventory management system. They allow for real-time visibility and control over stock levels.

  Retail Example:
  * Backroom Stockroom: A dedicated space in the shop where new inventory is stored before it is placed on the shelves. Every item entering or leaving the stockroom is logged.
  * Distribution Centre (part of the estate): A centralised warehouse where the retailer’s overall stock is held.  The system tracks every dispatch or receipt of goods to and from this location.
  * Online Fulfilment Zone (Logical Tracked Location): A portion of the shop's inventory allocated for online orders. This area is tracked separately to ensure that online and in-store stocks remain accurate.

  Key Features:

  * Ensures complete accountability for stock.
  * Required for financial and operational reporting.
  * Supports replenishment planning and stock audits.

  2. Untracked Locations
  These are areas where stock can be temporarily held or stored without formal tracking of individual movements.
  These locations are used for simplicity and operational convenience, rather than for ongoing stock control.

  Retail Example:
  * Shop Floor Shelves: Once stock is placed on the shelves for customers to purchase, it is no longer tracked individually in the system. Stock counts here are typically done manually during periodic checks or when items are restocked.
  * Transit Area (Logical Location): When goods are being moved from the stockroom to the shop floor, they might pass through a staging area where they are not formally tracked.
  * Customer Returns Hold Area: Items awaiting inspection or re-shelving are temporarily held here, without detailed tracking in the system.

  Key Features:
  * Used for convenience in areas where granular tracking adds little value.
  * Stock visibility is lower, often requiring manual checks or approximations.
  * Suitable for operational zones with high stock turnover or temporary storage.

  *Summary*
  In a retail context:
  * Tracked Locations (e.g., stockrooms and distribution centres) provide a high level of visibility and control,
    essential for accurate inventory management.
  * Untracked Locations (e.g., shop floor shelves and transit areas)
    simplify operations by focusing on the flow and availability of stock rather than its precise tracking.

  This dual approach ensures both efficiency and control, with tracked locations supporting centralised stock
  visibility and Untracked locations facilitating flexible, customer-facing operations.

  [mermaid]
  -----
  ---
  config:
    look: handDrawn
    theme: neutral
  ---
  flowchart LR
      subgraph key["Key"]
          t["(tracked)"]
          u["(untracked)"]
      end

      subgraph estate[" "]
          subgraph dcs["Distribution Centres"]
              direction TB
              dc1["Castle Donington"]
              dc2["Bradford"]
              P["Picking Location"]
          end

          subgraph s1["Stores"]
              direction TB
              store2["Cambridge"]
              store1["Royston"]
              region1["South East"]
              E["Backstage 1"]
              F["Shop Floor 2"]
              G["Shelf-a"]
              H["Shelf-b"]
              J["Backstage 2"]
              K["Shop Floor 2"]
              L["Shelf-c"]
              M["Shelf-d"]
          end

          region1 --> store1 & store2
          store1 --> E & F
          F --> G & H
          store2 --> J & K
          K --> L & M
          A["Estate"] --> s1 & dcs
          dc1 --> P

      end


      style estate fill:none,stroke:none;

      classDef tracked fill:#00C853;

      class store1,store2,dc1,dc2,t tracked;
  -----

  Background: A store has multiple locations that a product can be held
    Given the following locations exit:
      | location      | parentLocation | type      |
      | Estate        |                | Untracked |
      | Stores        | Estate         | Untracked |
      | South East    | Stores         | Untracked |
      | Cambridge     | South East     | Tracked   |
      | Backstage 1   | Cambridge      | Untracked |
      | Shop floor 1  | Cambridge      | Untracked |
      | Shelf-a       | Shop floor 1   | Untracked |
      | Shelf-b       | Shop floor 1   | Untracked |
      | Royston       | South East     | Tracked   |
      | Backstage 2   | Royston        | Untracked |
      | Shop floor 2  | Royston        | Untracked |
      | Shelf-c       | Shop floor 2   | Untracked |
      | Shelf-d       | Shop floor 2   | Untracked |
      | Milton Keynes | Estate         | Tracked   |

  Scenario: the stock in a region equals the Stock of all Stores
    Given the stock level of "Beans" in "Cambridge" is 10
    And the stock level of "Beans" in "Royston" is 20
    Then the current stock level of "Beans" in "South East" will equal 30

  Scenario: ensure the the stock levels in other stores don't affect this store
    Given the stock level of "Beans" in "Cambridge" is 5
    And the stock level of "Beans" in "Royston" is 7
    And the stock level of "Beans" in "Milton Keynes" is 20
    Then the current stock level of "Beans" in "South East" will equal 12