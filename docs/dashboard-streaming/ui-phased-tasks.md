# Dashboard Streaming — UI Phased Task List

Track execution here. Use task IDs in commits/PRs (e.g. `UI-A0-03`).

**Backend dependency:** UI tasks assume matching backend tasks in [phased-tasks.md](./phased-tasks.md) (especially **A0** before full E2E).

**Package map** (folder `am_analysis_ui` is sparse; real code lives elsewhere):

| Package | Path | Role |
|---------|------|------|
| `am_dashboard_ui` | `am-modern-ui/am_dashboard_ui` | Dashboard page, REST + STOMP |
| `am_analysis` / `am_analysis_ui` | `am-modern-ui/am_analysis/ui` | Analysis REST only (no STOMP today) |
| `am_library` | `am-modern-ui/am_library` | `AmStompClient` |
| `am_common` | `am-modern-ui/am_common` | `StompConnectionCubit`, JWT on CONNECT |
| `am_app` | `am-modern-ui/am_app` | `AppShell` — global STOMP lifecycle |

---

## Phase summary

| Phase | Package | Tasks | Gate |
|-------|---------|-------|------|
| [UI-A0](#phase-ui-a0--p0-dashboard-streaming) | am_dashboard_ui + am_app | UI-A0-01…UI-A0-09 | **G0-UI** (with backend G0) |
| [UI-P1](#phase-ui-p1--hardening) | am_app + am_common | UI-P1-01…UI-P1-04 | G1 |
| [UI-E](#phase-ui-e--history-widget) | am_dashboard_ui | UI-E-01…UI-E-03 | G3 |
| [UI-P2](#phase-ui-p2--cleanup) | am_dashboard_ui + docs | UI-P2-01…UI-P2-03 | G2 |
| [UI-M](#phase-ui-m--market-via-gateway) | am_common + am_market | UI-M-01…UI-M-06 | M-GATE |

---

## Phase UI-M — Market via gateway

| ID | Task | Files | Validation | Status |
|----|------|-------|------------|--------|
| UI-M-01 | `PriceService` uses `AmStompClient` + `/topic/stock/{symbol}` (not raw `marketWs`) | `price_service.dart`, `equity_price_mapper.dart` | M-01, M-03 | ✅ |
| UI-M-02 | Gateway proxies am-market connect via STOMP `/app/market/subscribe` (no client REST) | `price_service.dart`, `MarketStreamProxyService.java` | M-02 | ✅ |
| UI-M-03 | `MarketProvider` explicit subscribe/unsubscribe on load/dispose | `market_provider.dart` | M-04, M-05 | ✅ |
| UI-M-04 | Remove `StreamService` from indices performance view | `indices_performance_view.dart` | M-01 | ✅ |
| UI-M-05 | Deprecate `marketWs` in env/config examples | `env_domains.dart`, `.env.example`, config JSON | M-01 | ✅ |
| UI-M-06 | STOMP reconnect resubscribe for active stock topics | `price_service.dart` | M-07 | ✅ |

---

## Phase UI-A0 — P0 dashboard streaming

| ID | Task | Files | Backend deps | Validation | Status |
|----|------|-------|--------------|------------|--------|
| UI-A0-01 | Send `/app/dashboard/subscribe` when `DashboardPage` mounts; `/app/dashboard/unsubscribe` on dispose | `dashboard_repository.dart`, `dashboard_page.dart` | A0-02, A0-03 | UI-1 | ✅ |
| UI-A0-02 | Gate on `StompConnectionCubit` / `AmStompClient.isConnected` before subscribe | `dashboard_repository.dart` | — | UI-2 | ✅ |
| UI-A0-03 | Subscribe to `/user/queue/dashboard/summary` | `dashboard_repository.dart` | GW-1, D-01 | UI-3 | ✅ |
| UI-A0-04 | Subscribe to `/user/queue/dashboard/activity` | `dashboard_repository.dart` | GW-1, D-02 | UI-4 | ✅ |
| UI-A0-05 | Subscribe to `/user/queue/dashboard/allocation` | `dashboard_repository.dart` | GW-1, D-03 | UI-5 | ✅ |
| UI-A0-06 | Subscribe to `/user/queue/dashboard/movers` | `dashboard_repository.dart` | GW-1, D-04 | UI-6 | ✅ |
| UI-A0-07 | Subscribe to `/user/queue/dashboard/history` | `dashboard_repository.dart` | A0-09 | UI-7 | ✅ |
| UI-A0-08 | Parse **direct widget DTO** from frame body (no `json['summary']` wrapper) | `dashboard_repository.dart` | GW-2 | UI-8 | ✅ |
| UI-A0-09 | Per-widget `StreamProvider`s: REST seed → merge STOMP updates | `dashboard_provider.dart`, `dashboard_page.dart` | US-2, US-3 | UI-9 | ✅ |

---

## Phase UI-P1 — Hardening

| ID | Task | Files | Backend deps | Validation | Status |
|----|------|-------|--------------|------------|--------|
| UI-P1-01 | Remove `userId` from STOMP subscribe bodies; JWT only | `app_shell.dart`, `portfolio_cubit.dart`, `dashboard_repository.dart` | A0-01, SEC-1…3 | UI-SEC-1 | partial |
| UI-P1-02 | **Channel exclusivity:** skip global portfolio subscribe when dashboard active | `app_shell.dart`, `global_portfolio_wrapper.dart` | IR-5 | UI-IR-1 | partial |
| UI-P1-03 | Resubscribe all 5 dashboard queues + re-send `/app/dashboard/subscribe` on STOMP reconnect | `dashboard_repository.dart`, `stomp_connection_cubit.dart` | UI-A0-01 | UI-10 | ⏳ |
| UI-P1-04 | Keep `PORTFOLIO/ALL` **REST-only** for allocation | `dashboard_repository.dart` | DATA-2, L16 | UI-DATA-1 | ✅ |

---

## Phase UI-E — History widget

| ID | Task | Files | Backend deps | Validation | Status |
|----|------|-------|--------------|------------|--------|
| UI-E-01 | Wire performance chart to `/user/queue/dashboard/history` stream | `dashboard_page.dart` (via `historyStreamProvider`) | E-01, E-04 | UI-11 | partial |
| UI-E-02 | Timeframe selector: 1D live append vs 1W/1M full series | `dashboard_page.dart` | E-02, E-03 | UI-12 | ⏳ |
| UI-E-03 | REST fallback when snapshot/stream miss | `dashboard_repository.dart` | DA-5 | UI-13 | ✅ |

---

## Phase UI-P2 — Cleanup

| ID | Task | Files | Backend deps | Validation | Status |
|----|------|-------|--------------|------------|--------|
| UI-P2-01 | Remove legacy `/topic/dashboard/{userId}` (removed in UI-A0) | `dashboard_repository.dart` | P2-07 | UI-14 | ✅ |
| UI-P2-02 | Update `am_dashboard_ui/docs/implementation_plan.md` | `am_dashboard_ui/docs/*` | P2-07 | Docs | ⏳ |
| UI-P2-03 | Align `EnvDomains.wsStream` everywhere | `env_domains.dart`, config JSON | — | UI-15 | ⏳ |

---

## Progress tracker (manual)

| Phase | Done | Total |
|-------|------|-------|
| UI-A0 | 9 | 9 |
| UI-P1 | 1 | 4 |
| UI-E | 1 | 3 |
| UI-P2 | 1 | 3 |
| UI-M | 6 | 6 |

**Next UI task:** `UI-P1-01` (portfolio cubit JWT-only), `UI-P1-03` (reconnect)
