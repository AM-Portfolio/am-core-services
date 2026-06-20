# Gateway WebSocket Streaming — Documentation

Live streaming from Kafka through **am-gateway** to Flutter over STOMP/WebSocket (`/v1/streams`).

Companion to [dashboard-streaming](../dashboard-streaming/README.md) (widget-level dashboard topics).

## Documents

| Document | Purpose |
|----------|---------|
| [streaming-guide.md](./streaming-guide.md) | E2E flows: portfolio, market, trade, dashboard; `KafkaRelayService` matrix; UI triggers; failure modes |
| [log-verification-checklist.md](./log-verification-checklist.md) | Copy-paste kubectl grep commands per stream |

## Quick reference — STOMP destinations

| UI page | STOMP SUBSCRIBE (receive) | STOMP SEND (register / upstream) | Kafka topic(s) |
|---------|---------------------------|----------------------------------|----------------|
| **Dashboard** | `/user/queue/dashboard/summary`, `activity`, `allocation`, `movers`, `history` | `/app/dashboard/subscribe` | `dashboard-*-update` (5) |
| **Portfolio** | `/user/queue/portfolio` | `/app/portfolio/subscribe` | `am-portfolio-stream`, `am-portfolio-update` |
| **Market** | `/topic/stock/{symbol}` | `/app/market/subscribe` | `am-stock-price-update` |
| **Trade** | `/user/queue/trade` | *(none — passive relay)* | `am-trade-update` |

## Gateway code map

| Component | Path |
|-----------|------|
| STOMP controllers | [`services/am-gateway/.../controller/`](../services/am-gateway/src/main/java/com/am/gateway/controller/) |
| Kafka → WebSocket relay | [`KafkaRelayService.java`](../services/am-gateway/src/main/java/com/am/gateway/service/KafkaRelayService.java) |
| Interest registry + `am-user-watching` | [`PortfolioSubscriptionManager.java`](../services/am-gateway/src/main/java/com/am/gateway/service/PortfolioSubscriptionManager.java) |
| Market upstream proxy | [`MarketStreamProxyService.java`](../services/am-gateway/src/main/java/com/am/gateway/service/MarketStreamProxyService.java) |
| Kafka topic constants | [`KafkaTopics.java`](../libraries/am-kafka-lib/src/main/java/com/am/kafka/config/KafkaTopics.java) |

## Related docs

- [ARCHITECTURE.md](../ARCHITECTURE.md) — platform overview
- [dashboard-streaming](../dashboard-streaming/README.md) — dashboard widget publishers (am-analysis)
- [observability/DEVELOPER_GUIDE.md](../observability/DEVELOPER_GUIDE.md) — `[FLOW]` log format

## What's next (P0)

1. Deploy am-gateway (per-topic consumer groups) + am-analysis (orchestrator group split) + am-modern-ui (subscribe fixes).
2. Run [log-verification-checklist.md](./log-verification-checklist.md) on preprod for each tab: Dashboard, Portfolio, Market, Trade.
3. P1: wire `/app/portfolio/heartbeat`, trade STOMP destination matching (Spring user prefix), optional `/app/trade/subscribe` if demand-driven trade push is needed.
