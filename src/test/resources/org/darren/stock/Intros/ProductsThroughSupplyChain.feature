@ProductsThroughSupplyChain @asciidoc @order-1
Feature: Movement of Stock throughout the supply chain

Stock enters the network via a deliver at a location such as a `Receiving` location within a `Distribution Centre`. The stock may then be moved to a `Picking` location for later picking.  During picking the products may be placed on a `Cage` which is a moveable location that can be moved to the `Dispatch` area, and then Products can be moved onto other `Cages` that are optimised for transportation.  `Cages` are loaded onto a `Truck` for delivery to a shop.  The Shop receives the `Cages` moving the products `Cages` to the `Shop` and the `Products` are moved to the `Shop Floor` for sale.

[mermaid]
-----
---
config:
    look: handDrawn
    theme: neutral
---
flowchart LR
    subgraph key["Key"]
        t["(building)"]
        u["(untracked)"]
        m["(moveable)"]
    end

    subgraph estate[" "]

        subgraph sc["Supply Chain"]
            direction TB
            subgraph dcs["Distribution Centres"]
                direction TB
                dc1["Castle Donington"]
                dc2["Bradford"]
                D1["Dispatch"]
                R1["Receiving"]
                D2["Dispatch"]
                R2["Receiving"]
                cages1@{ shape: procs, label: "Cages-1"}
                cages2@{ shape: procs, label: "Cages-2"}
                cages3@{ shape: procs, label: "Cages-3"}
                cages4@{ shape: procs, label: "Cages-4"}
                R1-->cages1
                D1-->cages2
                R2-->cages3
                D2-->cages4
            end

            subgraph fleet["Transport"]
                direction TB
                truck1["Truck-1"]
                truckn@{ shape: procs, label: "Truck-n"}
                cages5@{ shape: procs, label: "Cages-5"}
                cages6@{ shape: procs, label: "Cages-6"}
                truck1 --> cages5
                truckn --> cages6
            end
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
            bs2["Backstage 2"]
            K["Shop Floor 2"]
            L["Shelf-c"]
            M["Shelf-d"]
            cages7@{ shape: procs, label: "Cages-7"}
        end

        region1 --> store1 & store2
        store1 --> E & F
        F --> G & H
        store2 --> bs2 & K
        K --> L & M
        A["Estate"] --> s1 & sc
        dc1 --> D1
        dc1 --> R1
        dc2 --> D2
        dc2 --> R2
        bs2 --> cages7

    end

    style estate fill:none,stroke:none;

    classDef tracked fill:#00C853;

    classDef movable fill:yellow;

    class store1,store2,dc1,dc2,t tracked;
    class cages1,cages2,cages3,cages4,cages5,cages6,cages7,m movable;
-----

    Background:
        Given the following locations exist:
            | Location Id       | Parent Location Id | Role                |
            | Castle Donington  |                    | Distribution Centre |
            | Ambient Warehouse | Castle Donington   | Warehouse           |
            | Bay-3             | Ambient Warehouse  | Storage Unit        |
            | Ambient Receiving | Castle Donington   | Receiving           |
            | Cage-6712         | Ambient Receiving  | Cage                |
            | Dispatch          | Castle Donington   | Dispatch            |
            | Truck-1           |                    | Truck               |
            | Cambridge         |                    | Shop                |

    Scenario: Delivery of Stock to a Distribution Centre is put away to a picking bay
      Stock is delivered on a truck to a `Receiving` location at a `Distribution Centre`.  The stock is then moved to a `Picking` location for later picking.

      Given the following are the current stock levels:
          | Location Id | Product    | Stock Level |
          | Bay-3       | product123 | 50          |
      When there is a delivery of 400 "product123" to "Ambient Receiving"
      And 400 "product123" is moved from "Ambient Receiving" to "Bay-3"
      Then the stock levels should be updated as follows:
          | Location Id       | Product    | Stock Level |
          | Bay-3             | product123 | 450         |
          | Ambient Receiving | product123 | 0           |

      Scenario: Stock is picked and transported to Store
        Stock is picked  from the Picking location and placed on a Cage, the cage is transported to the Dispatch area, Placed on a Truck and delivered to a Shop.  The movement of the location from a parent location to another results in the total stock for the parent location being updated.
          Given the following are the current stock levels:
              | Location Id | Product    | Stock Level |
              | Bay-3       | product123 | 450         |
              | Cage-6712   | product123 | 0           |
          When 50 "product123" is moved from "Bay-3" to "Cage-6712"
          And "Cage-6712" is moved to "Dispatch"
          And "Cage-6712" is moved to "Truck-1"
          Then the total stock levels should be updated as follows:
              | Location Id      | Product    | Stock Level |
              | Castle Donington | product123 | 400         |
              | Truck-1          | product123 | 50          |
              | Cage-6712        | product123 | 50          |
          When "Truck-1" is moved to "Cambridge"
          And "Cage-6712" is moved to "Cambridge"
          And 50 "product123" is moved from "Cage-6712" to "Cambridge"
          Then the stock levels should be updated as follows:
              | Location Id | Product    | Stock Level |
              | Bay-3       | product123 | 400         |
              | Cage-6712   | product123 | 0           |
              | Cambridge   | product123 | 50          |