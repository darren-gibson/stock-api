@section-Counts
@asciidoc
Feature: Count Endpoint
  *Purpose*
  The Count endpoint is designed to record a stock count adjustment for a specific product at a given location. By recording these adjustments, the endpoint ensures that stock levels reflect the most accurate and up-to-date inventory position. This is essential for reconciling discrepancies, supporting operational accuracy, and maintaining trust in inventory data.

  *Primary Use Cases:*

  1. [.underline]#Inventory Reconciliation#: Adjusts stock levels to match physical inventory following stock takes or manual verification. Helps correct discrepancies caused by miscounts, damages, or shrinkage.
  2. [.underline]#Error Correction#: Updates stock figures when errors from previous transactions, system failures, or unrecorded movements are identified, ensuring data accuracy.
  3. [.underline]#Operational Readiness#: Ensures that the stock data is accurate and complete for business-critical processes like audits, restocking, or sales forecasting. Enables effective decision-making by reflecting the true inventory position.
  4. [.underline]#Audit Preparation#: Provides a reliable mechanism to reconcile inventory before internal or external audits, ensuring compliance with regulatory and operational standards.

  Scenario: Root
