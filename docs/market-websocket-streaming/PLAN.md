# Plan — Market WebSocket live updates

| Field | Value |
|---|---|
| Kind | `fix` |
| Lead repos | `am-market`, `am-modern-ui`; gateway relay in `am-core-services` |
| Scope | Stocks and indexes subscribed by the Market UI |
| Status | Core implementation applied; gateway runtime validation and browser smoke test pending |
| Confirmed root cause | Two confirmed losses were found: the converter read only `marketFF` (dropping `indexFF`), and the streamer did not retain the requested-symbol map, allowing `Nifty 50` to be relayed while the UI subscribed to `NIFTY 50` |

## Plan review scores

- **Before adversarial review: 7/10.** It covered the main `indexFF` loss and the visible UI subscription gaps, but did not explicitly cover malformed/partial feeds, market-closed fallback, reconnect duplication, stale ordering, nullable UI fields, or provider limits.
- **After the gap updates below: 9/10.** The implementation and test paths now cover those cases. The remaining point is intentionally held back until runtime timestamp evidence and a real browser/Kafka smoke test confirm the measured startup latency and production-like symbol coverage.

## Simple problem statement

Kafka can keep receiving stock ticks while the UI receives no new index frames. The WebSocket can remain open; it only displays application messages that the gateway publishes for the subscribed topic. Missing matching messages look like a stopped socket.

The initial delay is also real in the current design: the browser sends STOMP subscriptions, then the gateway synchronously calls the market REST connect endpoint. The market service starts the Upstox connection asynchronously and builds an initial snapshot before returning. There is no STOMP “stream ready” acknowledgement, so the UI cannot distinguish “accepted” from “first upstream tick received”.

## Verified path

```text
Upstox marketFF/indexFF
  → UpstoxV3FeedConverter
  → StreamerManager / MarketDataProcessor
  → Kafka am-stock-price-update
  → am-gateway KafkaRelayService
  → /topic/stock/{symbol}
  → PriceService
  → MarketProvider / widgets
```

## Required changes

1. **Backend conversion** — update `am-market/am-market-data/market-data-provider/.../UpstoxV3FeedConverter.java` to extract LTP and OHLC from both `marketFF` and `indexFF`. Add tests proving both feed types become non-empty `OHLCQuote` values.
2. **Backend symbol contract** — preserve the requested UI symbol (`NIFTY 50`, `NIFTY BANK`, etc.) when mapping `NSE_INDEX|...` keys. Test aliases, spaces, and case.
3. **Kafka contract** — verify index quotes are emitted in the event consumed by the gateway. Do not create a second UI-only path unless the existing event cannot represent indexes.
4. **Gateway relay** — keep the generic relay, but add a test that every event symbol, including spaces, is sent to the exact `/topic/stock/{symbol}` destination. No ingress change is planned.
5. **UI subscription coverage** — remove or make explicit the `MarketProvider` 20-symbol cap. Subscribe every visible/selected symbol that the product promises. Add global-index subscriptions; they are currently fetched by REST but not added to the live stream.
6. **UI state updates** — ensure `PriceService` updates the cache and `MarketProvider` updates the state used by index cards, selected-index headers, and constituent/stock detail views. The current provider updates `_allIndicesData` only; selected-index data must not remain snapshot-only.
7. **Stock detail subscription** — update `am-modern-ui/am_market/ui/lib/features/stock_detail/presentation/pages/stock_detail_page.dart` (or the shared coordinator) to subscribe the opened stock and release it when the page closes. The current page reads `getPrice(widget.symbol)` but never requests that symbol.
8. **Live chart series** — decide and implement the expected chart behavior: merge incoming ticks into the visible `StockChart` series, or explicitly label the chart as historical while only the header is live. The current chart receives the one-time `_chartData` REST list and never consumes live ticks.
9. **Startup/readiness** — add correlated timestamps for STOMP receive, gateway proxy start/end, market stream start, Upstox `onOpen`, first converted quote, Kafka publish, gateway relay, and UI receipt. If the measured delay remains unacceptable, make the proxy start operation asynchronous and return a clear stream-accepted/readiness status.
10. **Feed freshness and ordering** — preserve a provider/arrival timestamp and reject or ignore out-of-order updates for the same symbol. Do not let a delayed Kafka message overwrite a newer UI price.
11. **Reconnect and lifecycle safety** — make symbol ownership explicit, resubscribe only once after reconnect, remove symbols when a detail page closes, and ensure a failed subscribe cannot leave a phantom UI subscription.
12. **Secrets and diagnostics** — redact tokens and authorization headers in client/server logs while adding enough correlation data to diagnose a missing tick.

## Files in scope

- `am-market/am-market-data/market-data-provider/src/main/java/com/am/marketdata/provider/upstox/converter/UpstoxV3FeedConverter.java`
- `am-market/am-market-data/market-data-provider/src/main/java/com/am/marketdata/provider/upstox/UpstoxMarketDataStreamer.java`
- `am-market/am-market-data/market-data-provider/src/main/java/com/am/marketdata/provider/upstox/UpstoxSymbolResolver.java`
- `am-market/am-market-data/market-data-provider/src/main/java/com/am/marketdata/provider/upstox/UpstoxIndexIdentifier.java`
- `am-market/am-market-data/market-data-service/src/main/java/com/am/marketdata/service/websocket/processor/MarketDataProcessor.java`
- `am-market/am-market-data/market-data-common/src/main/java/com/am/marketdata/common/mapper/OHLCMapper.java`
- `am-market/am-market-data/market-data-kafka/src/main/java/com/am/marketdata/kafka/producer/KafkaProducerService.java`
- `am-market/am-market-data/market-data-api/src/main/java/com/am/marketdata/api/service/MarketDataPollingService.java`
- `am-core-services/services/am-gateway/src/main/java/com/am/gateway/service/KafkaRelayService.java` (contract test/observability; behavior only if test fails)
- `am-core-services/services/am-gateway/src/main/java/com/am/gateway/controller/MarketStompController.java`
- `am-core-services/services/am-gateway/src/main/java/com/am/gateway/service/MarketStreamProxyService.java`
- `am-modern-ui/am_common/lib/core/services/price_service.dart`
- `am-modern-ui/am_common/lib/core/models/equity_price_mapper.dart`
- `am-modern-ui/am_common/lib/core/network/websocket/stomp_connection_cubit.dart`
- `am-modern-ui/am_market/common/lib/providers/market_provider.dart`
- `am-modern-ui/am_market/ui/lib/features/stock_detail/presentation/pages/stock_detail_page.dart`
- `am-modern-ui/am_market/ui/lib/shared/widgets/stock_chart.dart`
- Relevant Market UI widgets under `am-modern-ui/am_market/ui/lib/features/market` and `.../dashboard`
- Unit/integration tests in the three repos

## Acceptance checks

1. Kafka contains repeated stock and index symbols after one subscribe request.
2. Browser Network → WebSocket shows repeated `MESSAGE` frames for every subscribed symbol, including `NIFTY 50`.
3. The `symbol` in each payload exactly matches the topic and UI key.
4. A stock detail page and an index card visibly change when a new quote arrives.
5. A selected-index/constituent view also changes without a REST refresh.
6. More than 20 requested symbols are either all streamed or clearly rejected/reported; none silently disappear.
7. A reconnect rebinds topics and restarts the upstream subscription without duplicate listeners.
8. Logs show the end-to-end timestamps and identify whether delay is upstream, REST startup, Kafka, gateway, or UI.
9. An `indexFF` quote with LTP but missing optional OHLC does not disappear; an invalid/zero-LTP feed is rejected with a diagnostic reason.
10. Market-closed and provider-fallback modes are explicit: the UI does not claim live streaming when the backend intentionally returns a snapshot.
11. Reconnects do not create duplicate STOMP subscriptions or duplicate gateway upstream subscriptions.
12. Older/out-of-order ticks cannot overwrite a newer cached quote.
13. Missing `change`/`changePercent` fields do not crash the stock-detail UI.
14. Stock-detail open/close and tab changes do not leak subscriptions.
15. No access token or authorization header appears in browser, gateway, market, or Grafana logs.

## Edge-case matrix

| Case | Expected behavior | Test/owner |
|---|---|---|
| `indexFF` with LTP and no OHLC | Emit a quote using LTP and available previous-close data; do not silently drop it | Converter + mapper tests |
| `marketFF` stock update | Existing stock behavior remains unchanged | Converter regression test |
| Zero/negative/malformed LTP | Drop only the invalid item and record the reason; continue the batch | Converter/processor test |
| Unknown index alias or whitespace/case variation | Resolve to a known key or return an explicit unresolved-symbol result; never silently treat it as an equity | Resolver tests |
| Missing Kafka symbol/payload field | Do not publish an unusable event; expose a metric/log | Kafka producer test |
| Market closed / fallback polling | Publish only the mode’s supported data and expose snapshot vs live status | Polling-service test + UI smoke |
| More than 20 requested symbols or provider quota | Apply a documented visible-symbol policy or return a clear rejection; never silently omit symbols | UI subscription test |
| Global index visible | It is subscribed and has the same live-update path as Indian indexes | UI/provider test |
| STOMP reconnect | Rebind each topic once and re-send upstream interest once | PriceService/Cubit test |
| Detail page opens/closes repeatedly | Subscribe once, unsubscribe on dispose, and do not leak listeners | Stock-detail widget test |
| Payload symbol differs from topic symbol | Reject/log the mismatch or normalize through one canonical symbol map | Gateway/UI contract test |
| Older Kafka tick arrives late | Ignore it for the affected symbol | Cache/order test |
| Missing `change` or `changePercent` | Render price safely and derive values when possible; no runtime cast exception | Dart mapper/widget test |
| Browser receives no matching topic | Show stream state/last-update age rather than treating a quiet tab as proof of disconnect | UI smoke test |

## Not in scope

- Ingress, Helm, CORS, or health-value changes.
- Replacing Kafka or the STOMP protocol.
- Treating a quiet Network Messages tab alone as a disconnected socket.

## Implementation status

The converter, Kafka guard, gateway relay contract test, and Flutter subscription/state fixes are implemented on the requested working branches. No deployment or remote push was performed. Gateway Maven tests are currently blocked by missing GitHub Packages credentials; market backend and focused Flutter tests pass.
