# Gateway streaming — log verification checklist

Run after opening a tab in the UI (replace namespace/pod names for your environment).

All backend spans use `[FLOW step=...]` — see [observability/DEVELOPER_GUIDE.md](../observability/DEVELOPER_GUIDE.md).

---

## 0. Shared — STOMP connect (all pages)

```bash
kubectl logs -n am-apps-preprod deploy/am-gateway | grep "gateway.stomp.connect"
```

Expect: `status=ok` with `user=<jwt-sub>`.

Flutter: `AmStompClient: ✅ Connected to STOMP broker.`

---

## 1. Dashboard (5 Kafka widget topics)

**UI:** [`dashboard_repository.dart`](../../../am-modern-ui/am_dashboard_ui/lib/data/repositories/dashboard_repository.dart)

```bash
# Subscribe + interest
kubectl logs -n am-apps-preprod deploy/am-gateway | grep -E "gateway.stomp.dashboard.subscribe|gateway.kafka.publish.user_watching"

# Analysis publish (5 widgets on subscribe)
kubectl logs -n am-apps-preprod deploy/am-analysis | grep -E "analysis.orchestrator.subscribe_dashboard_push|analysis.kafka.publish.dashboard_"

# Gateway relay + WS send
kubectl logs -n am-apps-preprod deploy/am-gateway | grep -E "gateway.kafka.relay.dashboard_|gateway.ws.send.dashboard_widget"
```

**Pass criteria:** Same `user=` on connect → subscribe → 5× `analysis.kafka.publish.dashboard_*` → 5× `gateway.kafka.relay.dashboard_*` → 5× `gateway.ws.send.dashboard_widget`.

Kafka UI: topics `dashboard-summary-update`, `dashboard-activity-update`, `dashboard-allocation-update`, `dashboard-movers-update`, `dashboard-history-update` show recent messages.

---

## 2. Portfolio (`/user/queue/portfolio`)

**UI:** [`portfolio_cubit.dart`](../../../am-modern-ui/am_portfolio_ui/lib/features/portfolio/presentation/cubit/portfolio_cubit.dart)

```bash
# STOMP register (both required)
kubectl logs -n am-apps-preprod deploy/am-gateway | grep -E "gateway.stomp.subscribe.received|gateway.kafka.publish.user_watching"

# Orchestrator chain
kubectl logs -n am-apps-preprod deploy/am-analysis | grep -E "analysis.kafka.consume.user_watching|analysis.kafka.publish.portfolio_stream|analysis.kafka.consume.stock_update"

# Gateway relay
kubectl logs -n am-apps-preprod deploy/am-gateway | grep -E "gateway.kafka.relay.portfolio_update|gateway.ws.send.queue_portfolio"
```

**Pass criteria:** `gateway.stomp.subscribe.received` includes `portfolioId=...` → `user_watching` → `analysis.kafka.publish.portfolio_stream` → Kafka topic `am-portfolio-stream` has messages → `gateway.kafka.relay.portfolio_update` → `gateway.ws.send.queue_portfolio`.

**If stuck:**

| Last log seen | Check |
|---------------|-------|
| No `gateway.stomp.subscribe.received` | UI did not SEND `/app/portfolio/subscribe` |
| `user_watching` only | Orchestrator consumer group / Redis interest |
| No relay | `am-portfolio-stream` empty — check `analysis.kafka.publish.portfolio_stream` or Mongo entity missing |
| Relay, no UI | JWT `user` ≠ Kafka `userId` or `portfolioId` mismatch in cubit |

---

## 3. Market (`/topic/stock/{symbol}`)

**UI:** [`price_service.dart`](../../../am-modern-ui/am_common/lib/core/services/price_service.dart)

```bash
# Upstream connect (no interest registry)
kubectl logs -n am-apps-preprod deploy/am-gateway | grep -E "gateway.stomp.market.subscribe|gateway.market.connect"

# Price relay
kubectl logs -n am-apps-preprod deploy/am-gateway | grep "gateway.kafka.relay.stock_update"
```

**Pass criteria:** `/app/market/subscribe` → `gateway.market.connect.proxy status=ok` → SUBSCRIBE `/topic/stock/...` in browser → `gateway.kafka.relay.stock_update` with `prices=N` → STOMP MESSAGE frames.

Kafka UI: `am-stock-price-update` has steady traffic.

**If stuck:**

| Symptom | Check |
|---------|-------|
| SUBSCRIBE then mass UNSUBSCRIBE | UI subscription lifecycle (PriceService dispose / syncSymbols) |
| SUBSCRIBE, no MESSAGE | Upstream connect failed; symbol mismatch in topic path vs payload |
| Kafka has data, no relay | Consumer group `am-gateway-stock-group` partition assignment |

---

## 4. Trade (`/user/queue/trade`)

**UI:** [`trade_controller_repository_impl.dart`](../../../am-modern-ui/am_trade_ui/lib/features/trade/internal/data/repositories/trade_controller_repository_impl.dart)

**No** `/app/trade/subscribe` — passive receive only.

```bash
# Relay only (producer is am-trade service)
kubectl logs -n am-apps-preprod deploy/am-gateway | grep "gateway.kafka.relay.trade_update"
```

**Pass criteria:** UI SUBSCRIBE `/user/queue/trade` → when `am-trade-update` message published with `userId` → `gateway.kafka.relay.trade_update` → STOMP MESSAGE.

**If stuck:**

| Symptom | Check |
|---------|-------|
| No relay | `am-trade-update` topic empty — trade service not publishing |
| Relay, no UI | JWT `user` ≠ JSON `userId`; destination header may be `/user/{id}/queue/trade` not exact match in UI filter |
| Relay err `missing_userId` | Kafka payload lacks `userId` field |

---

## 5. Kafka consumer groups (gateway health)

Verify in Kafka UI each group has active members and non-empty partition assignment:

| Group | Topic |
|-------|-------|
| `am-gateway-stock-group` | `am-stock-price-update` |
| `am-gateway-portfolio-group` | `am-portfolio-stream`, `am-portfolio-update` |
| `am-gateway-trade-group` | `am-trade-update` |
| `am-gateway-dashboard-summary-group` | `dashboard-summary-update` |
| `am-gateway-dashboard-activity-group` | `dashboard-activity-update` |
| `am-gateway-dashboard-allocation-group` | `dashboard-allocation-update` |
| `am-gateway-dashboard-movers-group` | `dashboard-movers-update` |
| `am-gateway-dashboard-history-group` | `dashboard-history-update` |

**Anti-pattern:** Multiple `@KafkaListener` methods sharing one `groupId` — causes `partitions: []` after rebalance. Gateway uses **one group per listener** (post-fix).

---

## 6. End-to-end smoke order

1. Login → confirm `gateway.stomp.connect`
2. Dashboard tab → checklist §1
3. Portfolio tab → checklist §2
4. Market tab → checklist §3
5. Trade tab → checklist §4 (may need a trade event in preprod)
