@section-Sales
@asciidoc
Feature: Sale Endpoint
    *Purpose*
    The Sale endpoint is designed to record the sale of a product at a tracked location. By recording each sale, the endpoint ensures that stock levels are accurately updated in real-time, reflecting the reduction in inventory due to the sale. This is critical for maintaining up-to-date stock levels, enabling effective inventory management, and supporting other business functions such as replenishment, reporting, and analytics.

    Sales can only be recorded at **tracked locations**. An attempt to record a sale at an untracked location will result in an error, as untracked locations do not maintain stock figures or support transactional updates.

    *Primary Use Cases:*

    1. [.underline]#Real-Time Stock Adjustment#: Updates stock levels to reflect products sold at a tracked location. Ensures consistency between physical stock and system records.
    2. [.underline]#Sales Tracking#: While not the authoritative source for sales analytics, this endpoint provides valuable insights for just-in-time inventory management levels. Supports financial reconciliation and audit processes by maintaining a detailed history of transactions.
    3. [.underline]#Replenishment Planning#: Triggers alerts or actions for low-stock thresholds based on the updated stock levels. Helps distribution centres prepare restock orders to maintain availability.

    Scenario: Root
