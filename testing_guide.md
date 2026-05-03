# AM Core Services - Testing Guide & API Reference

This document provides a comprehensive list of API endpoints for the core services. Use this for Postman collections or manual testing.

---

## 1. AM Analysis Service (`am-analysis`)
**Base URL:** `http://<host>:8080/api/v1/analysis`
**Auth Required:** `Authorization: Bearer <token>`

### 📊 Dashboard Endpoints
| Endpoint | Method | Params | Description |
| :--- | :--- | :--- | :--- |
| `/dashboard/summary` | `GET` | `userId` | High-level summary of user wealth and performance. |
| `/dashboard/portfolio-overviews` | `GET` | `userId`, `portfolioId` (opt) | Card data for portfolio displays. |
| `/dashboard/top-movers` | `GET` | `userId`, `timeFrame` (1D, 1M, etc.) | Biggest gainers/losers for the user. |
| `/dashboard/performance` | `GET` | `userId`, `timeFrame` | Chart data for user performance. |
| `/dashboard/recent-activity` | `GET` | `userId`, `page`, `size`, `status` | Paginated feed of trades/events. |

### 🔍 Deep Analysis Endpoints
#### **Get Allocation**
*   **Path:** `GET /{type}/{id}/allocation`
*   **Headers:** `Authorization`, `groupBy` (STOCK, SECTOR, ASSET_CLASS)
*   **Variables:**
    *   `type`: `PORTFOLIO`, `STOCK`, `USER`
    *   `id`: The unique identifier (e.g., `port-123`)

#### **Get Performance**
*   **Path:** `GET /{type}/{id}/performance`
*   **Params:** `timeFrame` (1D, 1W, 1M, 1Y, ALL)
*   **Variables:**
    *   `type`: `PORTFOLIO`, `STOCK`
    *   `id`: The unique identifier

---

## 2. AM Gateway Service (`am-gateway`)
**WebSocket URL:** `ws://<host>:8091/portfolio`
**Protocol:** STOMP
**Auth Required:** `Authorization: Bearer <token>`


### 📡 WebSocket Topics
| Topic | Payload | Description |
| :--- | :--- | :--- |
| `/portfolio/subscribe` | `{"userId": "...", "portfolioId": "..."}` | Start live tracking for a portfolio. |
| `/portfolio/heartbeat` | `{"userId": "..."}` | Keep the session alive in Redis. |
| `/portfolio/unsubscribe` | `{"userId": "..."}` | Stop tracking. |

### 📥 Real-time Output
*   **Topic to Listen:** `/user/topic/portfolio-stream`
*   **Payload Received:** Live P&L, current price updates, and valuation changes.

---

## 3. AM MCP Server (`am-mcp-server`)
**Type:** JSON-RPC / Spring AI
**Tools Exposed:** These are "AI Endpoints" that can be tested via an MCP client.

| Tool Name | Arguments | Purpose |
| :--- | :--- | :--- |
| `getPortfolioAnalysis` | `portfolioId` | AI-driven insights on a specific portfolio. |
| `getMarketOutlook` | `symbol` | Technical analysis summary for a stock. |
| `calculateTradeImpact` | `symbol`, `quantity`, `price` | Predicts how a trade changes your portfolio. |

---

## 🛠️ Testing from Frontend
The following endpoints **MUST** be tested via the UI to verify the full flow:

1.  **WebSocket Connection**: Open the Portfolio Dashboard in the Flutter UI. Verify that `am-gateway` logs a `onSubscribe` event and data starts flowing.
2.  **Auth Token Extraction**: Verify that the UI correctly passes the `Authorization` header to `am-analysis`. If the token is missing or malformed, the API will return `401 Unauthorized` or `403 Forbidden`.
3.  **Real-time Recalculation**: Change a stock price in the backend and verify the UI updates **without a page refresh** (verifies Kafka $\rightarrow$ Gateway $\rightarrow$ UI flow).

---

## 📝 Postman Payload Examples

### Recent Activity Filter
```json
// GET /api/v1/analysis/dashboard/recent-activity?userId=user123&status=WIN&size=10
{
  "status": "success",
  "data": {
    "items": [...],
    "totalPages": 5
  }
}
```

### WebSocket Subscribe (STOMP)
```javascript
stompClient.send("/portfolio/subscribe", {}, JSON.stringify({
    'userId': 'user_99',
    'portfolioId': 'main_portfolio'
}));
```

---

## 💻 Running Services Locally

To test the services locally on your machine, follow these steps:

### 1. Build the Project
Before running, build the necessary libraries and domain models:
```bash
npm run build:libs
npm run build:domain
npm run build:core
```

### 2. Run Automated Tests
To execute the Java unit and integration tests:
```bash
npm run test
```

### 3. Start Services for Manual Testing
To start the Gateway, Analysis, and MCP services all at once:
```bash
npm run dev
```
*(Alternatively, you can run them individually: `npm run run:analysis`, `npm run run:gateway`, `npm run run:mcp`)*

---

## 🎯 Testing the API via Postman (Example)

Once your services are running locally (using `npm run dev`), you can test the Analysis Service endpoints.

### Example: Dashboard Summary API

**Endpoint:** `GET http://localhost:8080/api/v1/analysis/dashboard/summary`

**Required Query Parameters:**
- `userId`: The ID of the user (e.g., `user123`)

**Required Headers:**
- `Authorization`: `Bearer <your_auth_token>`

**Expected Successful Response (200 OK):**
```json
{
  "status": "success",
  "data": {
    "totalValue": 150240.50,
    "dayGain": 1200.75,
    "dayGainPercentage": 0.8,
    "totalGain": 25000.00,
    "totalGainPercentage": 19.9,
    "cashBalance": 12500.00,
    "investedAmount": 125240.50
  }
}
```
