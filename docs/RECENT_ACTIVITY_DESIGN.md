# Recent Activity — Feature Design Document

## What is Recent Activity?

A **time-ordered event log** — things that *happened* to the user's portfolios,
not a snapshot of what they currently *own*.

> ❌ NOT a holdings view ("You own 10 AAPL, +23%")
> ✅ YES an event feed ("You bought 10 AAPL on 20 Feb at ₹150")

---

## Activity Types

### 1. `BUY` — Stock Purchase
User bought shares in a portfolio.

| Field | Example |
|---|---|
| symbol | `AAPL` |
| companyName | Apple Inc. |
| quantity | 10 shares |
| tradePrice | ₹150.00 |
| currentPrice | ₹185.00 |
| avgBuyingPrice | ₹148.00 (across all buys) |
| profitLoss | +₹370.00 |
| profitLossPercent | +24.67% |
| status | **WIN** |
| portfolioId | `portfolio-123` |
| portfolioName | Growth Fund |
| timestamp | 20 Feb 2026, 2:30 PM |

> *"Bought 10 AAPL @ ₹150.00 in Growth Fund"*

---

### 2. `SELL` — Stock Sale
User sold shares (partial or full exit).

| Field | Example |
|---|---|
| symbol | `RELIANCE` |
| quantity | 5 shares sold |
| tradePrice | ₹2,800.00 |
| avgBuyingPrice | ₹2,400.00 |
| profitLoss | +₹2,000.00 |
| status | **WIN** |
| timestamp | 19 Feb 2026, 11:00 AM |

> *"Sold 5 RELIANCE @ ₹2,800 — Profit ₹2,000 (+16.67%)"*

---

### 3. `DIVIDEND` — Dividend / Income Received
Dividend payout or interest credited.

| Field | Example |
|---|---|
| symbol | `HDFC` |
| companyName | HDFC Bank Ltd. |
| amount | +₹300.00 |
| portfolioName | Long Term Portfolio |
| timestamp | 18 Feb 2026 |

> *"Dividend received from HDFC — ₹300.00"*

---

### 4. `SIP` — Systematic Investment Plan Executed
Scheduled/recurring investment triggered.

| Field | Example |
|---|---|
| symbol | `NIFTY50-ETF` |
| amount | ₹5,000.00 |
| quantity | 12.5 units |
| navPrice | ₹400.00 |
| portfolioName | SIP Portfolio |
| timestamp | 1 Feb 2026 |

> *"SIP executed — Nifty50 ETF ₹5,000 (12.5 units @ ₹400)"*

---

### 5. `REBALANCE` — Portfolio Rebalanced
Auto or manual rebalancing of portfolio weights.

| Field | Example |
|---|---|
| portfolioId | `portfolio-123` |
| portfolioName | Balanced Fund |
| description | Equity reduced 60% → 55%, Debt increased |
| timestamp | 15 Feb 2026 |

> *"Growth Fund auto-rebalanced — Equity 60% → 55%"*

---

### 6. `ALERT` — Price / System Alert
A configured price target or system event was triggered.

| Field | Example |
|---|---|
| symbol | `TCS` |
| alertType | PRICE_TARGET |
| description | Crossed ₹4,000 target |
| currentPrice | ₹4,015.00 |
| timestamp | 17 Feb 2026 |

> *"TCS crossed your ₹4,000 price target — now at ₹4,015"*

---

## API Design

```
GET /api/v1/analysis/dashboard/recent-activity
    ?userId=xxx
    &type=BUY,SELL,DIVIDEND     ← filter by one or more types (comma-separated)
    &status=WIN                  ← WIN / LOSS / NEUTRAL
    &sector=Technology           ← sector filter
    &portfolioId=portfolio-123   ← specific portfolio, or omit for ALL
    &days=30                     ← rolling window in days (default 30, max 90)
    &sortBy=TIMESTAMP            ← TIMESTAMP | PROFIT_LOSS | AMOUNT
    &page=0&size=20              ← pagination
```

**Response shape:**
```json
{
  "items": [...],
  "totalItems": 48,
  "totalPages": 3,
  "hasNext": true,
  "totalWinning": 12,
  "totalLosing": 5,
  "totalNeutral": 3
}
```

---

## Data Sources

| Activity Type | Data Source | Field Used |
|---|---|---|
| BUY / SELL | `AnalysisHolding.transactions[]` where `type=BUY/SELL` | `transaction.date` |
| DIVIDEND | `AnalysisHolding.transactions[]` where `type=DIVIDEND` | `transaction.date` |
| SIP | `AnalysisHolding.transactions[]` where `type=SIP` | `transaction.date` |
| REBALANCE | `AnalysisEntity.lastUpdated` | entity update event |
| ALERT | `AnalysisEntity.additionalStats["alerts"]` | stored alert map |

---

## Holdings vs Recent Activity (Key Distinction)

| | Holdings | Recent Activity |
|---|---|---|
| **Shows** | What you currently own | What happened recently |
| **Time scope** | All time (no date filter) | Last 30 days (rolling window) |
| **Unit** | One row per symbol | One row per transaction event |
| **Primary sort** | By value / sector | By event timestamp DESC |
| **Endpoint** | `/dashboard/holdings` | `/dashboard/recent-activity` |

---

## Implementation Status

| Task | Status |
|---|---|
| `ActivityType` enum (BUY, SELL, DIVIDEND, REBALANCE, SIP, ALERT) | 🔲 Update enum |
| `ActivityItem` DTO with `tradePrice` + `days` filter | 🔲 Update DTO |
| `getRecentActivity()` from `transactions[]` flattened by date | 🔲 Refactor service |
| `getHoldings()` — rename current holdings-based impl | 🔲 Rename method |
| `/dashboard/holdings` endpoint | 🔲 Add controller endpoint |
