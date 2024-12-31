@section-StockLevel @asciidoc
Feature: Get Stock Level for Product at Location Endpoint

    The **Get Stock Level for Product at Location** endpoint provides precise, real-time information on the inventory levels of specific products at designated locations. This capability is essential for businesses to manage inventory efficiently, optimise operations, and make informed decisions based on accurate stock data.

    The objective of this feature is to deliver a reliable and standardised mechanism for retrieving stock level information. Whether tracking inventory at a warehouse, retail outlet, or distribution center, this endpoint ensures visibility into product availability at any point in time.

    The Get Stock Level for Product at Location endpoint enables:

    * Accurate monitoring of stock levels for individual products at specific locations.
    * Timely access to inventory data for operational and strategic decision-making.
    * Real-time updates to reflect the most current inventory status.
    * Flexibility to query stock levels either for a location including all its child locations or exclusively for the specified location.

    === Pending Adjustments in Stock Levels

    In addition to providing the current stock level, this endpoint considers known discrepancies in the stock. These discrepancies, referred to as `pendingAdjustment`, reflect any outstanding adjustments required to reconcile the inventory.

    Key behaviors include:
    - Accounting for mismatches caused by delayed transactions, such as sales recorded before stock replenishment updates.
    - Highlighting areas where stock data may need reconciliation to align with physical inventory.

    By addressing pending adjustments, businesses can improve inventory accuracy, reduce operational confusion, and ensure better alignment between system-reported and actual stock levels.

    === Hierarchical Location Support

    Locations are organised in a hierarchical structure, allowing for efficient inventory management across estates. By default, this endpoint returns the stock levels for the specified location and all its child locations. Optionally, the query can be restricted to return stock levels only for the specific location.

    The following diagram illustrates an example retail hierarchy:

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

    === Use Cases for Stock Level Retrieval

    This feature supports a range of operational and strategic needs, such as:

    [cols="3,7", options="header"]
    |===
    | Scenario Category           | Specific Use Cases
    | Inventory Management         a| * Stock Replenishment: Identifying low-stock products to trigger restocking.
    * Overstock Mitigation: Monitoring excess inventory to avoid waste.
    | Customer Experience          a| * Order Fulfilment: Ensuring product availability for customer orders.
    * Real-Time Availability: Providing customers with accurate product stock data.
    | Operational Planning         a| * Supply Chain Optimisation: Aligning inventory with demand forecasts.
    * Seasonal Adjustments: Preparing stock levels for seasonal or promotional demands.
    | Compliance and Reporting     a| * Audit Preparation: Ensuring accurate inventory data for regulatory audits.
    * Performance Metrics: Tracking inventory turnover and availability rates.
    |===

    === Key Elements of Stock Level Queries

    Stock level queries typically include the following considerations:

    * *Product Identifier*: The unique ID or SKU of the product being queried.
    * *Location Identifier*: The specific location where stock levels are being checked.
    * *Stock Count*: The quantity of the product available at the specified location.
    * *Pending Adjustment*: Any outstanding discrepancy value affecting stock accuracy.
    * *Timestamp*: The time the stock data was last updated.

    === Purpose

    The **Get Stock Level for Product at Location** endpoint is designed to:

    * Provide accurate, real-time insights into product availability at different locations.
    * Highlight any pending adjustments to ensure better stock reconciliation.
    * Support operational efficiency by ensuring inventory data is up-to-date.
    * Enhance decision-making capabilities for both day-to-day operations and strategic planning.

    By leveraging this functionality, organisations can streamline inventory management, reduce stockouts and overages, and deliver a superior customer experience through reliable stock level information.

Scenario: Root
