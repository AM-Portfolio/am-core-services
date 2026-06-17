# Dashboard Streaming — Phased Task List

Track execution here. Use task IDs in commits/PRs (e.g. `DASH-A0-03`).

**Rules:**
- Do not start a phase until the previous **phase gate** passes.
- Do not mark ✅ until validation rows pass — see [implementation-plan.md § Validation](./implementation-plan.md#validation-checklist--required-before-merging-functional-changes).

**Architecture reference:** [Three-layer model](./implementation-plan.md#three-layer-model--channel-vs-global-portfolio-vs-api-alias) — `CHANNEL:DASHBOARD_MAIN` (watch) ≠ `PORTFOLIO_GLOBAL_{userId}` (data) ≠ REST `id=ALL` (alias).

---

## Phase summary

| Phase | Module | Tasks | Gate |
|-------|--------|-------|------|
| [A](#phase-a--shared-foundation-am-kafka-lib) | am-kafka-lib | A-01…A-06 | — |
| [C](#phase-c--snapshot-infrastructure-am-analysis) | am-analysis | C-01…C-05 | — |
| [A0](#phase-a0--p0-production-blockers) | gateway + analysis | A0-01…A0-11 | **G0** |
| [D](#phase-d--dashboard-streaming-core-am-analysis) | am-analysis | D-01…D-10 | **G0** |
| [P1](#phase-p1--hardening) | analysis + gateway | P1-01…P1-09 | **G1** |
| [P2](#phase-p2--scale-quality--cleanup) | analysis + gateway | P2-01…P2-08 | **G2** |
| [B](#phase-b--previous-close-multi-window) | am-analysis-adapter | B-01…B-04 | **G3** (with E) |
| [E](#phase-e--history-widget-am-analysis) | am-analysis | E-01…E-04 | **G3** |
| [F](#phase-f--position-book-am-gateway) | am-gateway | F-01…F-05 | **G4** |
| [UI](#ui-frontend-am-modern-ui) | am-modern-ui | UI-A0, UI-P1, UI-E, UI-P2 | **G0** (with A0) |

**UI tasks:** Full tables in [ui-phased-tasks.md](./ui-phased-tasks.md).

---

## UI (frontend `am-modern-ui`)

See **[ui-phased-tasks.md](./ui-phased-tasks.md)** for full UI-A0 / UI-P1 / UI-E / UI-P2 tables.

| Phase | Summary | Status |
|-------|---------|--------|
| UI-A0 | `/app/dashboard/subscribe`, 5 widget queues, REST+stream hybrid | ✅ 9/9 |
| UI-P1 | JWT-only STOMP, channel exclusivity, reconnect | partial 1/4 |
| UI-E | History chart from `/queue/dashboard/history` | ⏳ 0/3 |
| UI-P2 | Remove `/topic/dashboard/{userId}`, update UI docs | ⏳ 0/3 |

---

## Phase A — Shared foundation (`am-kafka-lib`)

| ID | Task | Files | Validation | Status |
|----|------|-------|------------|--------|
| A-01 | Add dashboard widget Kafka topic constants | `KafkaTopics.java` | Build | ✅ |
| A-02 | Add `MarketDataKeys` (snapshot + prev-close prefixes) | `MarketDataKeys.java` | Build | ✅ |
| A-03 | Add shared `GainLossCalculator` | `GainLossCalculator.java` | Build | ✅ |
| A-04 | Add `PreviousCloseSnapshot` event model | `PreviousCloseSnapshot.java` | Build | ⏳ |
| A-05 | Add `InterestRegistryKeys.CHANNEL_DASHBOARD_MAIN` + `isDashboardChannel()` | `InterestRegistryKeys.java` | IR-1 | ✅ |
| A-06 | Add `AnalysisEntityKeys` — `GLOBAL_SOURCE_ID`, `globalEntityId(userId)` | `AnalysisEntityKeys.java` | DATA-1 | ✅ |

---

## Phase C — Snapshot infrastructure (`am-analysis`)

| ID | Task | Files | Validation | Status |
|----|------|-------|------------|--------|
| C-01 | MongoDB `DashboardSnapshot` document | `DashboardSnapshot.java` | Build | ✅ |
| C-02 | `DashboardSnapshotRepository` | `DashboardSnapshotRepository.java` | Build | ✅ |
| C-03 | `DashboardSnapshotService` persist + load (Redis → Mongo) | `DashboardSnapshotService.java` | DA-1…DA-5 | ✅ |
| C-04 | Wire snapshot layer into REST `get*()` methods | `DashboardAnalysisService.java`, `AnalysisServiceImpl.java` | REST smoke | ✅ |
| C-05 | Migrate snapshot service to `FlowLogger` | `DashboardSnapshotService.java` | OBS-4, OBS-5 | ⏳ |

---

## Phase A0 — P0 production blockers

**Goal:** Safe, correct main-dashboard channel before new features.

| ID | Task | Files | Deps | Validation | Status |
|----|------|-------|------|------------|--------|
| A0-01 | STOMP: `userId` from JWT `Principal`, reject payload mismatch | `PortfolioController.java` | — | SEC-1…SEC-3 | ✅ |
| A0-02 | Dashboard subscribe passes `CHANNEL_DASHBOARD_MAIN` | `PortfolioController.java` | A-05 | IR-1 | ✅ |
| A0-03 | `InterestRegistryService.register()` writes dashboard channel constant | kafka-lib `InterestRegistryService.java` | A-05 | IR-1 | ✅ |
| A0-04 | `hasActiveWatchers()` + `isDashboardChannel()` use `CHANNEL_DASHBOARD_MAIN` | kafka-lib `InterestRegistryService.java` | A-05 | IR-1, OR-2 | ✅ |
| A0-05 | Delete gateway duplicate registry; inject kafka-lib bean | Delete `gateway/InterestRegistryService.java` | A0-03 | IR-3, L5 | ✅ |
| A0-06 | Orchestrator `isDashboardChannel()` → `CHANNEL_DASHBOARD_MAIN` | `DemandDrivenOrchestrator.java` | A-05 | OR-1, OR-2 | ✅ |
| A0-07 | `triggerDashboardActivityUpdate()` (5s debounce) on `STOCK_UPDATE` | `DemandDrivenOrchestrator.java` | A0-06 | OR-5 | ✅ |
| A0-08 | Subscribe: push all 5 widgets (`publishDashboardSubscribeAll`) | `DemandDrivenOrchestrator.java`, `DashboardAnalysisService.java` | — | US-2, US-3 | ✅ |
| A0-09 | `publishDashboardHistory(userId, "1D")` using global portfolio entity | `DashboardAnalysisService.java` | A-06 | DA-5, US-2 | ✅ |
| A0-10 | `GlobalPortfolioResolver` — widgets read `PORTFOLIO_GLOBAL_{userId}` not `"ALL"` | `GlobalPortfolioResolver.java`, `DashboardAnalysisService.java` | A-06 | DATA-1 | ✅ |
| A0-11 | Compile + smoke: subscribe E2E | — | A0-01…A0-10 | Manual smoke, OBS-1 | partial |

---

## Phase D — Dashboard streaming core (`am-analysis`)

**Goal:** Per-widget Kafka publish + orchestrator routing complete.

| ID | Task | Files | Deps | Validation | Status |
|----|------|-------|------|------------|--------|
| D-01 | `publishDashboardSummary` (Kafka + snapshot) | `DashboardAnalysisService.java` | C-03 | DA-1 | ✅ |
| D-02 | `publishDashboardActivity` | `DashboardAnalysisService.java` | C-03 | DA-2 | ✅ |
| D-03 | `publishDashboardAllocation` | `DashboardAnalysisService.java` | C-03 | DA-4 | ✅ |
| D-04 | `publishDashboardMovers` | `DashboardAnalysisService.java` | C-03 | DA-3 | ✅ |
| D-05 | `onMarketUpdate()` portfolio vs dashboard channel split | `DemandDrivenOrchestrator.java` | A0-06 | OR-1, OR-2 | ✅ |
| D-06 | Summary debounce (1s) + movers debounce (5s) | `DemandDrivenOrchestrator.java` | D-05 | OR-3, OR-4 | ✅ |
| D-07 | Per-user `TriggerCalcEvent` on market move (Phase 1) | `DemandDrivenOrchestrator.java` | — | OR-7 | ✅ |
| D-08 | Gateway relay all 5 widget Kafka topics | `KafkaRelayService.java` | — | GW-1, GW-2 | ✅ |
| D-09 | `PORTFOLIO_UPDATE` listener | `DashboardUpdateListener.java` | D-01…D-04 | PU-1 | ✅ |
| D-10 | Complete remaining A0 + D-05…D-06 items | see A0-07…A0-10 | A0 | Gate G0 | partial |

---

## Phase P1 — Hardening

**Goal:** Production-safe fan-out, tracing, portfolio-update gating.

| ID | Task | Files | Deps | Validation | Status |
|----|------|-------|------|------------|--------|
| P1-01 | Split snapshot persist vs Kafka publish | `DashboardAnalysisService.java` | D-01…D-04 | PU-2 | ⏳ |
| P1-02 | `DashboardUpdateListener`: persist always; Kafka only if `CHANNEL_DASHBOARD_MAIN` | `DashboardUpdateListener.java` | P1-01, A0-04 | PU-1…PU-3 | ⏳ |
| P1-03 | Add `traceId` + `spanId` to `WidgetUpdateEvent` | `DashboardAnalysisService.java` | — | OBS-6 | ⏳ |
| P1-04 | Redis debounce keys (replace in-memory maps) | `DemandDrivenOrchestrator.java` | — | OR-3, OR-4, L4 | ⏳ |
| P1-05 | `analysis.orchestrator.dashboard_fanout` FlowSpan | `DemandDrivenOrchestrator.java` | — | OBS-2 | ⏳ |
| P1-06 | `analysis.orchestrator.subscribe_dashboard_push` FlowSpan | `DemandDrivenOrchestrator.java` | A0-08 | OBS-1 | ⏳ |
| P1-07 | Verify adapter uses `GainLossCalculator` on ingest | `am-analysis-adapter` | A-03 | L14 | ⏳ |
| P1-08 | Complete C-05 FlowLogger migration for snapshots | `DashboardSnapshotService.java` | — | OBS-4 | ⏳ |
| P1-09 | REST `id=ALL` alias → `GlobalPortfolioResolver` | `AnalysisController`, services | A0-10 | DATA-2 | ⏳ |

---

## Phase P2 — Scale, quality & cleanup

| ID | Task | Files | Deps | Validation | Status |
|----|------|-------|------|------------|--------|
| P2-01 | Symbol-aware tick fan-out | `DemandDrivenOrchestrator.java` + symbol cache | P1-04 | L3 | ⏳ |
| P2-02 | `@Async` parallel publish on subscribe | `DashboardAnalysisService.java` | A0-08 | US-3 | ⏳ |
| P2-03 | `DemandDrivenOrchestratorTest` | `src/test/java/...` | D-05 | T-1 | ⏳ |
| P2-04 | `InterestRegistryServiceTest` | `src/test/java/...` | A-05 | T-2 | ⏳ |
| P2-05 | `DashboardUpdateListenerTest` | `src/test/java/...` | P1-02 | T-3 | ⏳ |
| P2-06 | `PortfolioControllerSecurityTest` | gateway tests | A0-01 | T-4 | ⏳ |
| P2-07 | Deprecate legacy `DASHBOARD_UPDATE` relay | `KafkaRelayService.java` | UI migrated | L12 | ⏳ |
| P2-08 | Resolve `isStale` field on snapshot | `DashboardSnapshot.java` | — | L8 | ⏳ |

---

## Phase B — Previous-close multi-window (`am-analysis-adapter`)

| ID | Task | Files | Deps | Validation | Status |
|----|------|-------|------|------------|--------|
| B-01 | `PreviousCloseSnapshot` model | am-kafka-lib | A-04 | Build | ⏳ |
| B-02 | `PreviousCloseSnapshotListener` | am-analysis-adapter | B-01 | Kafka smoke | ⏳ |
| B-03 | `PreviousCloseRedisService` write + TTL 48h | am-analysis-adapter | B-02 | Redis HGET | ⏳ |
| B-04 | Gateway prev-close reader | `am-gateway/PreviousCloseRedisService.java` | B-03 | F-02 | ⏳ |

---

## Phase E — History widget (`am-analysis`)

| ID | Task | Files | Deps | Validation | Status |
|----|------|-------|------|------------|--------|
| E-01 | `HistoryStreamingService.getHistorySeries(userId, window)` | `HistoryStreamingService.java` | B-03, A0-09 | DA-5 | ⏳ |
| E-02 | `appendCurrentValue(userId, value)` for 1D | `HistoryStreamingService.java` | E-01 | History tick | ⏳ |
| E-03 | Orchestrator: history append on `STOCK_UPDATE` | `DemandDrivenOrchestrator.java` | E-02, A0-06 | Part E map | ⏳ |
| E-04 | Replace interim PerformanceAnalysisService stub | `DashboardAnalysisService.java` | E-01 | DA-5 | ⏳ |

---

## Phase F — Position book (`am-gateway`)

| ID | Task | Files | Deps | Validation | Status |
|----|------|-------|------|------------|--------|
| F-01 | `PositionEntry` + `PositionBook` models | am-gateway | — | Build | ✅ |
| F-02 | `PreviousCloseRedisService` reader | am-gateway | B-04 | Redis read | ⏳ |
| F-03 | `PositionBookClient` load holdings + prev-close | am-gateway | F-02 | Subscribe load | ⏳ |
| F-04 | `PositionBookService` inline P&L on `STOCK_UPDATE` | am-gateway | F-03 | `/queue/portfolio` | ⏳ |
| F-05 | Wire `PortfolioSubscriptionManager` load/evict | `PortfolioSubscriptionManager.java` | F-04 | Portfolio channel | ⏳ |

---

## Phase gates

| Gate | Complete first | Pass criteria |
|------|----------------|---------------|
| **G0** → dev/staging | A (A-05, A-06), C, all **A0**, D-10, **UI-A0** | SEC-1…3, IR-1…5, DATA-1, OR-1…7, US-1…3, DA-1…5, GW-1…2, UI-1…UI-9, smoke 1–4 |
| **G1** → production | G0 + all **P1** | PU-1…3, OBS-1…7, Redis debounce on 2+ pods |
| **G2** → scale | G1 + all **P2** | T-1…4, symbol filter or load test doc |
| **G3** → history live | G1 + all **B** + all **E** | 1D append + multi-window prev-close |
| **G4** → fast portfolio P&L | G1 + all **F** | Position book &lt;5ms |

---

## Sprint mapping

| Sprint | Focus | Task IDs |
|--------|-------|----------|
| **Sprint 1** | P0 blockers | A-05, A-06, A0-01…A0-11, **UI-A0-01…UI-A0-09** |
| **Sprint 2** | Hardening | P1-01…P1-09, C-05, **UI-P1-01…UI-P1-04** |
| **Sprint 3** | Tests + scale | P2-01…P2-08, **UI-P2-01…UI-P2-03** |
| **Sprint 4** | History | B-01…B-03, E-01…E-04, **UI-E-01…UI-E-03** |
| **Sprint 5** | Position book | B-04, F-02…F-05 |

---

## Progress tracker (manual)

Update counts when tasks complete:

| Phase | Done | Total |
|-------|------|-------|
| A | 5 | 6 |
| C | 4 | 5 |
| A0 | 10 | 11 |
| D | 9 | 10 |
| P1 | 0 | 9 |
| P2 | 0 | 8 |
| B | 0 | 4 |
| E | 0 | 4 |
| F | 1 | 5 |
| UI-A0 | 9 | 9 |
| UI-P1 | 1 | 4 |
| UI-E | 0 | 3 |
| UI-P2 | 0 | 3 |

**Next task:** `A0-11` manual E2E smoke; `A-04` PreviousCloseSnapshot; **P1** hardening
