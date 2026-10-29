# TODO — WebSocket gateway 401 caused by ingress path stripping

Pack: `docs/websocket-gateway-auth-fix/`. Implementation is underway after user confirmation; deployment and live mutation remain gated.

## Plan loop

- [x] Ingest source plan from current conversation/live diagnosis
- [x] Lookup `am-repo-catalog` (`domains.md` + `repos.md`)
- [x] Read available context packs (`am-gateway/.am.yaml`, `am-modern-ui/README.md`)
- [x] Analyze local source and read-only dev cluster evidence
- [x] Coverage check: `handles` for P0; `partial` for P1 hardening
- [x] Create new pack because no existing `docs/*/PLAN.md` matches
- [x] Find existing Postman collection
- [x] Confirm implementation branch `am-modern-ui:fix/backlog-market-ui` is checked out and clean
- [x] Record core investigation branch `hotfix/websocketGateway`
- [x] Bug adjacent-surface map, latency, corners, review roles
- [x] PLAN.md + images/ decision (`n/a`, no preview requested)
- [x] Architecture diagram via `am-architecture-drawio`
- [x] UI previews default off (`not-requested`)
- [x] Resolve route ownership: public `/gateway/v1/streams` → internal `/v1/streams`
- [x] Verify current dev failure: ingress references a missing `am-apps-dev-dev-strip-prefix` Middleware and returns HTTP 500
- [x] Verify preprod drift: `strip-prefix-apps` exists but its live prefix list omits `/gateway`
- [x] Make the environment Middleware resource a first-class P0 dependency in desired state; live name/namespace/prefix verification remains required before promotion
- [x] Raise Delivery/Design/scorecard after new dev-cluster evidence
- [x] Adversarial review updated with missing Middleware and environment-parity gaps
- [ ] Agent satisfied: no — user review is required before implementation/deploy
- [x] User confirmed Execute; deployment and production mutation remain separately gated

## P0 — public module endpoint contract

- [ ] Revoke/rotate the token exposed in the browser console before further testing.
- [x] Update `am-modern-ui/am_common/lib/core/config/env_domains.dart` so deployed `wsStream` is `/gateway/v1/streams`.
- [x] Add/document a local override for direct `am-gateway` port `8091` (`/v1/streams`) if local topology does not expose `/gateway`.
- [x] Keep the generic `/gateway` ingress route and define a matching strip-prefix Middleware for dev, preprod, prod, and DR.
- [x] Add `/gateway` to the shared module-prefix lists without changing unrelated module routes.
- [ ] Verify missing Middleware, wrong namespace/name, missing prefix, and double-strip behavior before UI promotion.
- [ ] Mark the dedicated `/v1/streams` ingress path deprecated; remove it only after compatibility verification.
- [x] Align gateway streaming docs and Postman remote examples with `/gateway/v1/streams`; `spt.yaml` already declares the `/gateway` module base.
- [x] Add endpoint-contract assertion: default UI `wsStream` ends with `/gateway/v1/streams`.
- [ ] Render/inspect dev, preprod, and prod ingress values plus live Middleware resources; verify `/gateway` is stripped exactly once before promotion.
- [x] Run read-only production upgrade probe: `/gateway/v1/streams` returned `101`; legacy `/v1/streams` returned `401`.
- [x] Run read-only production SockJS info probe: `/gateway/v1/streams-fallback/info` returned `200`.
- [x] Run read-only preprod upgrade and SockJS probes: `/gateway/v1/streams` returned `101`; fallback `/info` returned `200`; legacy `/v1/streams` returned `401`.
- [ ] Run credentialed production STOMP `CONNECTED` smoke after approval; do not use the exposed token.
- [ ] Add staged rollout/cache-version checks for old bundles still requesting `/v1/streams`.
- [ ] Sync the dev GitOps Middleware through the approved path after user confirmation; do not `kubectl apply` directly.
- [ ] Build/deploy the UI and gateway values through the approved Helm/Argo path after user confirmation.
- [ ] Verify raw WebSocket: `101 Switching Protocols`.
- [ ] Verify STOMP: `CONNECTED` with `Authorization` native header.
- [ ] Verify gateway logs show backend `/v1/streams`, no `/` 401, and no controller side effects before CONNECT.
- [ ] Verify `/gateway/v1/streams-fallback` SockJS route.
- [ ] Verify generated SockJS `/info`, `/websocket`, and XHR subpaths plus CORS headers.
- [ ] Verify a sustained connection beyond the 30-second heartbeat interval and market/dashboard/portfolio subscription recovery.
- [ ] Invalidate the token exposed in the reported browser console before testing.
- [ ] Verify rollback by reverting only the Helm ingress change if smoke fails.

## P1 — am-modern-ui / token and log hardening

- [x] Remove `access_token` URL construction from `am_library/lib/core/network/websocket/am_stomp_client.dart`.
- [x] Retain `stompConnectHeaders: {'Authorization': 'Bearer ...'}`.
- [x] Add a Dart unit/static assertion that URL redaction removes token query parameters.
- [x] Redact the client-side connection URL log.
- [x] Stop/defer reconnect on authentication failure; retain bounded transient-network reconnect.
- [ ] Redact query strings in `am-observability-lib` `RequestLoggingFilter` or configure a safe allowlist.
- [ ] Rotate/revoke any token exposed in browser/server logs.
- [x] Update `postman/AM-Gateway.postman_collection.json` to document `/gateway/v1/streams` as the remote routed endpoint; retain a direct local endpoint only where the service is exposed without ingress.
- [x] Update `am-core-services/postman/AM-Core-Services.postman_environment.json` and stale gateway streaming docs.
- [x] Remove or correct the unused `AM_WS_STREAM` entry in `.env.example`.

## Per service test loop

### Service: `am-gateway`

- [ ] Unit/static test Helm route rendering and middleware annotation names
- [ ] Verify dev Middleware exists in `am-apps-dev` and includes `/gateway`
- [ ] Verify preprod/prod authoritative Middleware source and live resources; current prod kubeconfig does not expose application resources
- [ ] WebSocket/STOMP smoke test
- [ ] Invalid/missing auth regression
- [ ] SockJS fallback regression
- [ ] `am test` for the slice verified

### Service: `am-modern-ui` (P1)

- [x] Dart unit/static test for URL/header behavior
- [x] Direct Dart analysis for changed files
- [x] `am_common` full test suite: 30 tests passed
- [x] `am_library` focused URL-redaction tests: 2 tests passed
- [ ] `am_library` package-wide analyzer/test baseline: existing `test/am_library_test.dart` references missing `Calculator`
- [ ] Browser WebSocket smoke after build
- [ ] Browser frame inspection proves native STOMP `Authorization` is present without a URL token.
- [ ] Auth-failure retry and token-refresh behavior verified.

## Deploy and report

- [ ] User confirms Execute
- [ ] Target preprod after dev evidence is green; production requires separate confirmation and credentialed smoke evidence
- [ ] Run Test Plan rows
- [ ] Write `REPORT.md` only after tests are verified
