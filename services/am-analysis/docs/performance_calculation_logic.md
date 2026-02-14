# Performance Calculation Logic

This document details the advanced performance calculation methodology implemented in the `am-analysis` service. The logic is designed to handle complex trading scenarios such as multi-leg trades, legging in/out, and partial profit booking, ensuring accurate "True P&L" reporting.

## Core Philosophy: "Time-Travel" Analysis

The calculation engine does not rely on a static snapshot of your current holdings. Instead, it reconstructs the portfolio state for **every single day** in the historical window.

### Key Concepts

1.  **Time-Aware Holdings**: 
    - Every holding (trade lot) has a `startDate` (Acquisition) and an optional `endDate` (Disposal).
    - A holding is only considered "Active" on a given day if: `startDate <= currentDay <= endDate`.
    
2.  **Invested Capital vs. Market Value**:
    - **Market Value**: The sum of `Quantity * Current Price` for all active holdings.
    - **Invested Capital (Cost Basis)**: The sum of `Quantity * Average Price` (or Acquisition Price) for all active holdings.
    - This separation allows us to distinguish between **Portfolio Growth** (Market movement) and **Capital Flows** (Deposits/Withdrawals).

3.  **True P&L Formula**:
    - Instead of a simple `(End Value - Start Value)` which is flawed when you add money mid-stream, we use:
    - **`Profit`** = `(Change in Market Value)` - `(Change in Invested Capital)`
    - This isolates the profit generated strictly by market movements.

---

## Detailed Logic Flow

The `PerformanceCalculator` component executes the following steps:

1.  **Fetch Data**: Retrieve historical price data for all symbols involved in the portfolio (past and present).
2.  **Iterate Dates**: Loop from `fromDate` to `toDate` (e.g., last 1 year).
3.  **Daily Calculation**:
    - For each day, iterate through **ALL** holdings (including closed ones).
    - **Check**: Is this holding active today?
    - **Sum**: If active, add its `(Price * Qty)` to `DailyMarketValue` and its `(Cost * Qty)` to `DailyInvestedValue`.
4.  **Aggregate**:
    - Capture `FirstValue` (Market Value on Day 1).
    - Capture `LastValue` (Market Value on Today).
    - Capture `FirstInvested` (Cost Basis on Day 1).
    - Capture `LastInvested` (Cost Basis on Today).
5.  **Compute Returns**:
    - `Total P&L` = `(LastValue - FirstValue) - (LastInvested - FirstInvested)`
    - `Total Return %` = `Total P&L / Initial Capital`

---

## Examples

### Scenario 1: Buy and Hold (Simusple)
*   **Day 1**: Buy 10 AAPL @ $100. (Market: $1,000, Invested: $1,000).
*   **Day 30**: AAPL rises to $150. (Market: $1,500, Invested: $1,000).

**Calculation**:
*   Market Change: $1,500 - $1,000 = **$500**.
*   Invested Change: $1,000 - $1,000 = **$0**.
*   **True P&L**: $500 - $0 = **$500**. (Correct).

---

### Scenario 2: Adding Capital (Legging In)
*   **Day 1**: Buy 10 AAPL @ $100. (Market: $1,000, Invested: $1,000).
*   **Day 15**: AAPL is at $110. Portfolio Value: $1,100. P&L: $100.
*   **Day 16**: **Buy 10 more AAPL @ $110**. (Deposit $1,100).
    *   New Market Value: $1,100 (Old) + $1,100 (New) = $2,200.
    *   New Invested Value: $1,000 (Old) + $1,100 (New) = $2,100.
*   **Day 30**: AAPL stays at $110.

**Naive Calculation (WRONG)**:
*   End Value ($2,200) - Start Value ($1,000) = **$1,200 Profit**. (Incorrect, you just added money).

**True P&L Calculation (CORRECT)**:
*   Market Change: $2,200 (End) - $1,000 (Start) = **$1,200**.
*   Invested Change: $2,100 (End) - $1,000 (Start) = **$1,100**.
*   **True P&L**: $1,200 - $1,100 = **$100**. (Correct, only the profit from the first lot count, second lot is flat).

---

### Scenario 3: Partial Sale (Legging Out)
*   **Day 1**: Buy 20 AAPL @ $100. (Market: $2,000, Invested: $2,000).
*   **Day 15**: AAPL rises to $150. Portfolio Value: $3,000.
*   **Day 16**: **Sell 10 AAPL @ $150**. (Cash out $1,500).
    *   Active Holdings reduced to 10 shares.
    *   New Market Value: 10 * $150 = $1,500.
    *   New Invested Value: 10 * $100 = $1,000.
*   **Day 30**: AAPL stays at $150.

**Calculation**:
*   Market Change: $1,500 (End) - $2,000 (Start) = **-$500**.
*   Invested Change: $1,000 (End) - $2,000 (Start) = **-$1,000**.
*   **True P&L**: (-$500) - (-$1,000) = -$500 + $1,000 = **$500**.
*   (This correctly captures the $500 profit from the remaining shares. The profit from the sold shares is realized and no longer tracked in the *active* portfolio curve, assuming expected behavior of "Current Holdings Analysis"). 
    *   *Note: If the system retains closed positions as "Inactive Holdings" in history, the sold portion's profit is captured up to the sell date.*

---


---

## Entity Type Handling

The calculation engine uses a unified approach, but the behavior differs logically based on the **Analysis Entity Type**.

### 1. Direct Asset Analysis (Indices, Mutual Funds, Single Stocks)
When analyzing a single instrument (e.g., `NIFTY_50`, `AAPL`, or a specific Mutual Fund), the entity is modeled as a container with **one single holding** that spans the entire requested timeframe.

*   **Logic**: The "Time Travel" loop iterates dates, finds the single active asset, and records its value.
*   **Result**: The performance chart directly mirrors the **Historical Price Chart** of the asset.
*   **Complexity**: Low. (1 Holding, Constant Quantity).
*   **Example**:
    *   **Entity**: "Nifty 50 Index"
    *   **Holding**: {Symbol: "NIFTY_50", Qty: 1, Start: 1990, End: Future}
    *   **Calculation**: On Day X, Value = Price(Day X) * 1.
    *   **Outcome**: Pure price history. No "Cost Basis" divergence because Qty never changes.

### 2. Composite Entity Analysis (Portfolios, Baskets, Trade Ideas)
When analyzing a User Portfolio, Market Basket, or Trade Strategy, the full power of the engine is utilized. These entities contain **multiple holdings** with changing lifecycles.

*   **Logic**: The loop sums the weighted values of *subset* of holdings active on any given day.
*   **Result**: A constructed "Synthetic" performance index unique to that user's behavior.
*   **Complexity**: High. (Many Holdings, Varying Start/End Dates).
*   **Example**:
    *   **Entity**: "Aggressive Tech Basket"
    *   **Holdings**: 
        *   AAPL (Jan-Mar)
        *   NVDA (Feb-Dec)
        *   MSFT (Jan-Dec)
    *   **Calculation**:
        *   **Jan**: Sum(AAPL, MSFT)
        *   **Feb**: Sum(AAPL, MSFT, NVDA) - *Capital Injection Logic Triggers*
        *   **Apr**: Sum(MSFT, NVDA) - *Capital withdrawal/realization Logic Triggers*
    *   **Outcome**: A custom curve reflecting the manager's timing and selection.

---

## Code Reference
The core logic resides in:
`com.am.analysis.service.calculator.PerformanceCalculator.java`

Key methods:
- `calculate(...)`: Orchestrates the loop.
- `dailyTotalValue += (price * qty)`: Tracks market movement.
- `dailyInvestedValue += (cost * qty)`: Tracks capital basis.

