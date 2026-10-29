# PLAN — WebSocket gateway 401 caused by ingress path stripping

| Field | Value |
|-------|-------|
| Kind | `fix` |
| Slug | `websocket-gateway-auth-fix` |
| Lead repo | `am-gitops` / `am-infra` (Traefik middleware desired state and environment parity) |
| Supporting repos | `am-core-services` (`services/am-gateway` ingress values and endpoint contract); `am-modern-ui` (public URL + token/log hardening) |
| Target env | `preprod` promotion target; route parity and read-only smoke required for `dev`, `preprod`, and `prod` |
| Branch | Implementation target: `am-modern-ui` on `fix/backlog-market-ui`; investigation pack remains in core on `hotfix/websocketGateway` |
| Delivery rating | `8.5/10` current review; middleware implementation and live protocol verification pending |
| Design rating | `8.8/10` current review; production source-of-truth and credentialed smoke pending |
| Scorecard overall | `8.5/10` current adversarial review |
| Agent satisfied | `no` — execution started after user confirmation; browser/prod credentialed verification pending |

Execute gate: **Delivery 10 + Design 10 + overall ≥ 9.5 + all dims ≥ 9.0 + Agent satisfied: yes** + user confirmation. No implementation or deployment before that gate.

## Goal

Restore the dev/preprod/prod STOMP WebSocket handshake through the public module contract `/gateway/v1/streams`. The current dev failure is verified as Traefik HTTP 500 because the ingress references `am-apps-dev-dev-strip-prefix@kubernetescrd`, but no matching Middleware exists in `am-apps-dev`. The expected result is `/gateway/v1/streams` externally, exactly one `/gateway` strip, backend `/v1/streams` internally, HTTP `101 Switching Protocols`, and a successful STOMP `CONNECTED` frame.

## Source plan (pointer)

| Field | Value |
|-------|-------|
| Origin | `none` — greenfield bug pack from the current conversation and live diagnosis |
| Pointer | User-reported browser console error plus read-only dev cluster verification |
| Summary | 1. Keep the generic `/gateway` module route. 2. Publish the gateway WebSocket as `/gateway/v1/streams`; a deployed environment middleware must strip exactly `/gateway`. 3. Fix the missing dev Middleware and verify the authoritative preprod/prod Middleware resources before rollout. 4. Do not make `/` public or add a path-specific bypass. 5. Keep STOMP `Authorization` header authentication. 6. Remove the browser URL token as follow-up hardening. 7. Verify with a real WebSocket/STOMP handshake. |
| Catalog | Core APIs and analysis domain; `am-gateway` is an existing service in `am-core-services`; streaming UI is an existing feature in `am-modern-ui`. The UI is the change-owning repo for this fix. |
| Context files | `am-core-services/services/am-gateway/.am.yaml`; `services/am-gateway/helm/values.dev.yaml`; `application-security.yml`; `WebSocketConfig.java`; `am-modern-ui/README.md`; `am-modern-ui/am_library/lib/core/network/websocket/am_stomp_client.dart`. `am-core-services` has no root README. |
| Analysis source | `am-core-services`: local; `am-modern-ui`: local; `am-gitops`: local; live dev ingress/logs: read-only cluster evidence |
| Coverage | `partial` for the original 401 plan: it handled the UI path but missed the verified dev 500 caused by a missing middleware. The updated plan adds GitOps/infra ownership and environment parity. |
| AM gaps | The UI path, middleware existence, middleware prefix list, namespace/name convention, cache rollout, SockJS subpaths, auth retry behavior, and production read-only visibility must all be verified together. |

## Images

- `images/` — no UI preview requested; this is a transport/configuration fix.

## Prerequisites (done / pending / blocked)

| Check | Status | Notes |
|-------|--------|-------|
| MCP needed / connected | `not needed` | Read-only `kubectl` evidence was sufficient. |
| Downstream APIs exist | `done` | Gateway endpoint and STOMP broker exist in `WebSocketConfig`; ingress service exists in dev. |
| Postman collection found | `done` | `postman/AM-Gateway.postman_collection.json`; its WebSocket alias should be corrected in P1. |
| Browser token exposure | `pending` | The reported console URL contains a credential-bearing token; revoke/rotate it before any further handshake testing. |

## Bug adjacent-surface map

### P0 — this slice

- Make the public endpoint contract `wss://<host>/gateway/v1/streams` and fallback `https://<host>/gateway/v1/streams-fallback`.
- Update `am-modern-ui/am_common/lib/core/config/env_domains.dart` so the deployed default uses `$wsBase/gateway/v1/streams`; retain an explicit local override for direct `am-gateway` port `8091`.
- Keep the generic `/gateway` ingress route, but make the strip-prefix Middleware an explicit infrastructure dependency. The Middleware must exist in the target namespace and include `/gateway`.
- Keep the legacy `/v1/streams` ingress path during rollout for compatibility; do not call it canonical and do not remove it until active-client/cache evidence is complete.
- Verify the three failure states separately: missing Middleware → Traefik `500`; no strip or double strip → gateway path mismatch/`401`; correct single strip → backend `/v1/streams` and `101`.
- Add endpoint-contract and browser/STOMP regression checks for `101` + `CONNECTED`.
- Render/inspect the dev, preprod, and prod gateway ingress values and live Middleware resources; fail the rollout if the annotation points to a missing resource, the namespace is wrong, or `/gateway` is absent from the prefix list.
- Roll out the UI with a cache/version check; keep the legacy route decision deferred until no supported client still emits `/v1/streams`.
- Verify gateway logs show backend `request.path=/v1/streams`, proving `/gateway` was stripped exactly once, and no HTTP 401.

### P1 — follow-up hardening

- Stop appending `access_token` to the WebSocket URL in `am-modern-ui/am_library/lib/core/network/websocket/am_stomp_client.dart`; retain STOMP `Authorization` headers.
- Redact query strings in `RequestLoggingFilter` so credentials cannot be logged.
- Redact the client-side connection URL log and never print the token-bearing URL.
- Stop/defer automatic reconnect on authentication failure until a refreshed token is available; keep bounded reconnect for transient network failures.
- Update the Postman WebSocket collection and environment to use the routed `/gateway/v1/streams` endpoint for remote environments; retain a direct local endpoint only where the local service is exposed without ingress.
- Update `.env.example` so it does not advertise the unused `AM_WS_STREAM` variable.

### P2 — later or never

- Add a dedicated route-level Helm mechanism so WebSocket paths and REST-prefixed paths can have different middleware chains.
- Add automated deployment smoke coverage for raw WebSocket and generated SockJS `/info`, `/websocket`, and XHR routes.
- Restrict gateway `allowedOriginPatterns` from `*` to approved UI origins as a separate security hardening change.

## Planned file changes

Implementation is split by ownership; no deployment is included in this planning pass:

1. `am-gitops/manifests/dev/traefik-dev-cors.yaml` (or an adjacent authoritative dev routing manifest) — define `Middleware/dev-strip-prefix` in `am-apps-dev` with `/gateway` plus the existing module prefixes; ensure the name matches `am-gateway` Helm values.
2. `am-infra/k8s/ingress-routing.yaml` — update the preprod shared `strip-prefix-apps` contract to include `/gateway`, or document the authoritative replacement if that manifest is no longer the live source.
3. Production GitOps/infra routing source — locate the authoritative prod Middleware resource, verify its namespace/name, and add `/gateway` only if absent. Do not invent a second production routing source.
4. `am-core-services/services/am-gateway/helm/values.dev.yaml`, `values.preprod.yaml`, and `values.prod.yaml` — keep the public `/gateway` path and align middleware references with the verified resource names; retain `/v1/streams` during compatibility rollout.
5. `am-modern-ui/am_common/lib/core/config/env_domains.dart` — keep the deployed default `wsStream` at `/gateway/v1/streams`.
6. `am-modern-ui/am_app/web/config.local.json.example` — document the direct local override `ws://localhost:8091/v1/streams` when local ingress is absent.
7. `am-modern-ui/am_library/lib/core/network/websocket/am_stomp_client.dart` and `stomp_connection_cubit.dart` — retain the already-applied token/log/reconnect hardening, then verify it after the route is green.
8. Tests/static checks — render Helm, assert middleware existence/prefixes, verify `101` + `CONNECTED`, fallback paths, cache behavior, and auth retry behavior.

Supporting documentation/observability changes are separate and do not change gateway behavior:

- `am-core-services/services/am-gateway/spt.yaml`, `docs/gateway-streaming/README.md`, and `docs/gateway-streaming/streaming-guide.md` — align remote examples to `/gateway/v1/streams`.
- `am-core-services/postman/AM-Gateway.postman_collection.json` and `AM-Core-Services.postman_environment.json` — use the routed remote endpoint; keep a direct local URL only for direct-port tests.
- `am-modern-ui/.env.example` — remove or correct the unused `AM_WS_STREAM` documentation.
- Request logging configuration/filter — redact query strings before any P1 rollout if the shared logging library is in scope.

No Java gateway or generic health change is planned. The universal Helm ingress template already renders the route; the missing piece is the environment Middleware resource and its parity with the annotation. The existing `/gateway` route remains the module boundary and must strip only that module prefix.

## First-run / virtual value UX

N/A. This bug affects transport establishment; no seeded capital, entitlement, or empty-book journey changes.

## Latency & consistency

- Handshake expectation: ingress forwards the request without path rewriting; the gateway upgrades to WebSocket, then processes STOMP `CONNECT`.
- Authentication expectation: the STOMP `Authorization` header is evaluated after the WebSocket upgrade; a missing/invalid header must produce a STOMP rejection, not an ingress-path HTTP 401.
- Reconnect expectation: the existing UI reconnect loop may retry; repeated failures must stop once the route is preserved.
- Long-lived expectation: the UI heartbeat runs every 30 seconds, so the acceptance test must keep a connection open beyond one heartbeat interval and verify the proxy does not close it.
- Auth-failure expectation: an expired or rejected token must not trigger an unbounded five-second retry storm; the client must wait for token refresh or logout.
- Reconnect consistency: after a transient reconnect, market subscriptions and dashboard/portfolio interest registrations must be restored or explicitly reported unavailable.
- No new p95/p99 target is invented. The regression gate is protocol state (`101` then `CONNECTED`) and absence of HTTP `401`.

## Current system (verified)

- `WebSocketConfig` registers `/v1/streams` and `/v1/streams-fallback` and allows STOMP `Authorization`/`token` native headers: `services/am-gateway/src/main/java/com/am/gateway/config/WebSocketConfig.java`.
- Spring Security permits `/v1/streams` paths, not `/`: `services/am-gateway/src/main/resources/application-security.yml`.
- Dev Helm attaches `am-apps-dev-dev-strip-prefix@kubernetescrd` to the gateway ingress: `services/am-gateway/helm/values.dev.yaml`.
- Read-only dev-cluster inspection shows the live `am-gateway-dev` ingress has both `/v1/streams` and `/gateway` paths, but `kubectl get middleware -n am-apps-dev` returns no resources. Traefik logs therefore report the referenced middleware does not exist and the `/gateway` handshake returns HTTP `500` before the gateway pod.
- Read-only preprod inspection shows `am-gateway-preprod` references `am-apps-preprod-strip-prefix-apps`, which exists, but its live `prefixes` list does not contain `/gateway`; the canonical route is therefore not proven to be stripped correctly even where an edge probe returns `101`.
- The gateway service itself registers `/v1/streams`; direct pod evidence shows it can return `101 Switching Protocols` when reached with that internal path.
- Earlier production public read-only probes on 2026-10-29 showed `wss://am.asrax.in/gateway/v1/streams` returning `101 Switching Protocols`, while `wss://am.asrax.in/v1/streams` returned `401`; this is transport evidence only, not proof that the production Middleware source is correct.
- Production read-only probe `https://am.asrax.in/gateway/v1/streams-fallback/info` returned `200`, confirming the public SockJS base route is reachable. The production kubeconfig exposed only infrastructure/Traefik resources and no application ingress objects, so middleware internals still require source parity plus a credentialed production STOMP smoke.
- Earlier preprod public read-only probes on 2026-10-29 showed `/gateway/v1/streams` returning `101`, `/v1/streams` returning `401`, and `/gateway/v1/streams-fallback/info` returning `200`; current live middleware inspection shows `/gateway` is absent from the shared prefix list, so backend-path and authenticated STOMP verification remain mandatory.
- The web client appends `?access_token=...` but also passes `Authorization` via `stompConnectHeaders`: `am-modern-ui/am_library/lib/core/network/websocket/am_stomp_client.dart` and `am_common/lib/core/network/websocket/stomp_connection_cubit.dart`.
- `am-gitops/dev/apps/am-gateway.yaml` consumes the Helm values from `am-core-services`; it is the rollout reference, not the source of the route annotation.
- `services/am-gateway/spt.yaml` already declares the public gateway base as `/gateway`, confirming the module-prefix contract.
- `am-modern-ui/am_common/lib/core/config/env_domains.dart` currently derives `wsStream` as `$wsBase/v1/streams`, bypassing the declared `/gateway` module prefix.
- `ConfigService.override('wsStream')` takes precedence over the derived default; the local example must use the `services.wsStream` key, while the stale `.env.example` variable is not read by the code.
- `StompConnectionCubit` schedules a fixed five-second reconnect with the same token after both disconnect and error; it does not currently distinguish an auth failure from a transient network failure.
- `AmStompClient` logs the full connection URL, so the current query-token workaround can leak credentials to browser/client logs.
- Dev, preprod, and prod Helm values use different strip-prefix middleware names; only dev live behavior has been verified so far.
- `am-pipelines/helm/universal-chart/templates/ingress.yaml` already supports the generic route; no chart extension is needed for the canonical `/gateway/v1/streams` URL.

## Target journeys

1. Browser opens the environment-specific `wss://<host>/gateway/v1/streams` in dev, preprod, or prod; generic ingress strips `/gateway`; gateway receives `/v1/streams`, returns `101`, and STOMP `CONNECT` succeeds.
2. Browser subscribes/sends portfolio, dashboard, or market STOMP frames after connection; existing gateway controllers continue unchanged.
3. SockJS fallback uses `/gateway/v1/streams-fallback`; generic ingress strips `/gateway` and forwards `/v1/streams-fallback` in every environment.
4. Invalid or missing STOMP authorization is rejected after the WebSocket route is reached, with no accidental public `/` route.

## Identity & ownership

- Traefik owns external path routing and middleware.
- `am-gateway` owns WebSocket endpoint registration and STOMP Principal binding.
- The UI owns the STOMP connection URL and native STOMP headers.
- JWT user identity comes from the STOMP `Authorization` header; client payload `userId` is not an authority source.

## State and consistency

- P0 changes no application state or event contracts.
- A failed handshake must not create a Redis interest registration or Kafka `am-user-watching` event.
- Existing reconnect behavior remains unchanged after a successful `CONNECTED` frame.

## Isolation

- Do not permit `/` globally to hide the route bug.
- Keep the WebSocket route scoped to the gateway service and existing host `am-dev.asrax.in`.
- Do not expose or persist JWTs in query strings.

## Failure modes

- Middleware missing or wrong namespace/name: Traefik cannot build the router and returns HTTP `500`; the gateway pod receives no handshake. This is the current dev failure and is a deployment-blocking check.
- Middleware exists but does not list `/gateway`: the gateway receives `/gateway/v1/streams` and the upgrade fails with a route mismatch.
- Middleware strips the wrong prefix or strips twice: the gateway receives `/` or another unexpected path and returns `401`/`404` before STOMP.
- Strip-prefix remains: gateway receives `/`, returns 401; deployment fails verification.
- Route preserved but STOMP header absent: WebSocket may upgrade, then STOMP CONNECT is rejected; this is a separate authentication test.
- Route preserved but token invalid/expired: STOMP CONNECT is rejected; no subscription actions occur.
- SockJS path stripped: fallback transport fails; must be tested separately.
- SockJS generated subpath or CORS failure: `/info`, `/websocket`, or XHR transport fails even though the base fallback URL responds; test the full fallback sequence.
- Runtime override remains stale: `services.wsStream` or a cached `config.local.json` keeps the old `/v1/streams` URL; inspect the resolved config in the built web app.
- Stale browser bundle: an already-open or cached UI continues using the old URL; rollout must include cache/version verification and a compatibility window.
- Auth rejection retry loop: the same expired token is retried every five seconds; stop/defer retries until auth state changes.
- Long-lived proxy timeout: initial `101` succeeds but the idle connection drops before/around the heartbeat interval; run a sustained connection smoke.
- Rollout mismatch: ingress changes before the gateway/UI are compatible; rollback is Helm values revert plus Argo sync.

## Corner-case matrix

| Case | Trigger | Expected | Covered by |
|------|---------|----------|------------|
| Missing Middleware | Ingress annotation references a resource absent from the namespace | Traefik `500` is detected before rollout; no false claim that the backend is unhealthy | Test Plan 1 |
| Wrong Middleware name/namespace | Helm annotation and CR metadata differ by environment | Render/live check fails with the exact name and namespace shown | Test Plan 1 |
| Prefix omitted | Middleware exists but `/gateway` is absent from `prefixes` | Backend path is wrong; test fails before UI promotion | Test Plan 1 |
| Double/wrong strip | More than one rewrite or wrong prefix applied | Gateway sees neither `/` nor `/gateway/v1/streams`; reject promotion | Test Plan 1/2 |
| Raw WebSocket happy path | `wss://.../gateway/v1/streams` with STOMP Authorization | `101`, then `CONNECTED`; gateway path is `/v1/streams` | Test Plan 2 |
| Current regression | Strip-prefix attached | No `101`; gateway logs `/` + 401 | Test Plan 1 |
| Missing STOMP auth | CONNECT without Authorization/token | STOMP rejection; no Redis/Kafka watch event | Test Plan 3 |
| Invalid/expired JWT | CONNECT with invalid token | STOMP rejection; no controller invocation | Test Plan 4 |
| SockJS fallback | `/gateway/v1/streams-fallback` | Generic `/gateway` strip forwards the service endpoint | Test Plan 5 |
| URL token cleanup | Web build after P1 | URL has no `access_token`; STOMP header remains | Test Plan 6 |
| Query log redaction | Request logging enabled | No credential value in gateway logs | Test Plan 7 |
| Unrelated route | `/gateway` request | No regression to its intended behavior | Test Plan 8 |
| Preprod/prod middleware parity | Render and inspect each env values file | `/gateway` is stripped exactly once in every promotion target | Test Plan 9 |
| Cached/old UI build | Existing tab or cached bundle still uses `/v1/streams` | Rollout detects old client; no premature legacy-route removal | Test Plan 10 |
| Auth retry storm | Expired token or STOMP auth rejection | No unbounded five-second retries; waits for token refresh/logout | Test Plan 11 |
| SockJS generated transport | `/info`, `/websocket`, or XHR fallback request | Route, CORS, and transport all succeed through `/gateway` | Test Plan 12 |
| Sustained connection | Connection remains open beyond 30-second heartbeat | Heartbeat succeeds; no proxy idle disconnect; subscriptions remain live | Test Plan 13 |
| Production route | Upgrade probe against `am.asrax.in` | `/gateway/v1/streams` returns `101`; legacy `/v1/streams` is not accepted as canonical | Test Plan 13 |
| Production SockJS base | `/gateway/v1/streams-fallback/info` | `200` SockJS info response | Test Plan 14 |

## Out of scope

- Changing JWT signing/validation design.
- Adding a new WebSocket broker or horizontal-scaling solution.
- Changing Kafka relay behavior, Redis interest semantics, or STOMP destinations.
- Direct production mutation or direct live-cluster edits.

## Open questions

- `decided`: P0 is the dev ingress path fix. P1 URL/logging hardening follows after the handshake regression is green.
- `decided`: do not add `/` to `public-paths`.
- `decided`: promotion values should be checked for the same middleware pattern before preprod rollout; no production change is authorized by this plan.
- `open`: rollout owner and cache/version window for retiring the broken direct `/v1/streams` client path.
- `open`: exact UI behavior after expired/invalid STOMP auth (pause reconnect until refresh, or force logout).
- `open`: preprod/prod public route probes pass, but live production middleware internals require the correct application-cluster read-only view and credentialed STOMP validation.

## Review roles

| Role | When | Looks for |
|------|------|-----------|
| Agent architect | Before user review | Evidence, route ownership, rollback, adversarial gaps |
| User / tech owner | Review gate | P0 vs P1 scope, deployment target, acceptable UI token cleanup |
| Optional `/review` | If asked | Senior code/config review |

## UI previews (modern-ui)

- Consent: `not-requested`
- Gate: `n/a` — no visual UI change
- Widget/page cites: `n/a`
- Previews: `n/a — not requested`

## Services

| Service | Existing or new | Impl notes | Unit/tests in this slice |
|---------|-----------------|------------|--------------------------|
| `am-gateway` | existing | Internal endpoint remains `/v1/streams`; public contract is its existing `/gateway` module base | Endpoint-contract assertion + live STOMP smoke |
| `am-modern-ui` | existing | Use `/gateway/v1/streams`; P1 removes URL token append; retain STOMP Authorization | Dart unit/static assertion + browser WebSocket smoke |
| `am-gitops` | existing | Owns dev/prod desired-state routing manifests and Middleware resources where applicable | Middleware existence, namespace/name parity, Argo health |
| `am-infra` | existing | Owns shared preprod/cluster routing source where applicable | `/gateway` prefix parity and read-only render/cluster check |

## Test Plan (Postman or MCP)

Collection: `am-core-services/postman/AM-Gateway.postman_collection.json` (P1 update needed for routed WebSocket path). Browser/WebSocket smoke is required because ordinary HTTP requests do not prove an upgrade.

| Row | Request or MCP call | Method + path | Expected | Owning service | Result |
|-----|---------------------|---------------|----------|----------------|--------|
| 1 | Render/inspect Helm ingress plus live Middleware resources for dev, preprod, and prod | N/A | Annotation target exists in the correct namespace, includes `/gateway`, and strips it exactly once; missing resources fail the gate | am-gateway + am-gitops/am-infra | dev failure verified: missing Middleware; preprod/prod pending |
| 2 | Browser DevTools or Postman WebSocket/STOMP client | `wss://am-dev.asrax.in/gateway/v1/streams` | `101 Switching Protocols`, then `CONNECTED` | am-gateway + UI | not verified |
| 3 | STOMP connect without auth | same | No `CONNECTED`; STOMP `ERROR` or close; no HTTP path 401 from `/`; no Redis/Kafka watch event | am-gateway | not verified |
| 4 | STOMP connect with invalid/expired auth | same | No `CONNECTED`; explicit rejection; no controller/subscription side effects | am-gateway | not verified |
| 5 | SockJS client and generated transports | `https://am-dev.asrax.in/gateway/v1/streams-fallback/info`, `/websocket`, and XHR paths | `/gateway` strip, CORS headers, and fallback transport all succeed | am-gateway | not verified |
| 6 | Web build after P1; inspect browser frames/logs | `wss://.../gateway/v1/streams` | URL has no `access_token`; STOMP `CONNECT` contains native `Authorization`; client logs contain no token | am-modern-ui | not verified |
| 7 | Gateway/client log inspection | N/A | No raw query token or token-bearing URL; successful request path `/v1/streams` | am-gateway/observability + UI | not verified |
| 8 | Existing `/gateway` route smoke | `/gateway` | No regression to its intended behavior | am-gateway | not verified |
| 9 | Preprod public route probe | `wss://am-preprod.asrax.in/gateway/v1/streams` | `101 Switching Protocols`; legacy `/v1/streams` remains non-canonical | edge + am-gateway | transport-only probe verified 2026-10-29; Middleware/STOMP auth pending |
| 10 | Cached/old UI build check | Old bundle/tab plus new bundle | Rollout detects old `/v1/streams` client; legacy-route removal is not premature | am-modern-ui + ops | not verified |
| 11 | Auth retry policy | Expired token / rejected CONNECT | No unbounded five-second retries; reconnect resumes only after token refresh/logout | am-modern-ui | not verified |
| 12 | Sustained connection + heartbeat | Same WebSocket for >30 seconds | Heartbeat succeeds; no proxy idle close; market/dashboard/portfolio subscriptions remain live or restore | am-gateway + UI | not verified |
| 13 | Production public route probe | `wss://am.asrax.in/gateway/v1/streams` with Upgrade headers | `101 Switching Protocols`; legacy `/v1/streams` remains non-canonical and returns the known failure without path fix | edge + am-gateway | verified read-only 2026-10-29 |
| 14 | Production SockJS base probe | `https://am.asrax.in/gateway/v1/streams-fallback/info` | `200` SockJS info response through the module prefix | edge + am-gateway | verified read-only 2026-10-29 |
| 15 | Preprod SockJS base probe | `https://am-preprod.asrax.in/gateway/v1/streams-fallback/info` | `200` SockJS info response through the module prefix | edge + am-gateway | verified read-only 2026-10-29 |

## Deploy

Default promotion target is `preprod` after the user review gate. Dev is the reproduction environment and must be fixed/verified first through the existing Argo/Helm path. No production deployment is included.

## Agent scorecard

| Dimension | Score /10 | Evidence or gap |
|-----------|-----------|-----------------|
| Product / features | 9 | Clear user-visible recovery; no UI redesign required |
| Architecture / design | 8 | Diagram pack created; canonical module-prefix flow is explicit and no route-specific ingress mechanism is required for P0 |
| Data / identity / contracts | 9 | STOMP Authorization and endpoint paths cited |
| State / money / consistency | 9 | No state mutation; no ghost watch on failed handshake |
| Reliability / failure | 7 | Auth retry storm, sustained heartbeat, and reconnect recovery now need explicit tests/policy |
| Security / authz | 7 | URL/client/server query-token leakage and permissive origin policy remain unresolved |
| Observability | 7 | Existing path/status logs are useful, but query redaction is pending |
| Testability | 7 | Expanded executable smoke rows are defined; none are run yet |
| Operability / rollout | 8 | Preprod/prod public route probes pass; live production middleware internals, cache rollout, and legacy-client window remain pending |
| TODO / implementability | 9 | File-level tasks, edge cases, and acceptance criteria are explicit |
| **Overall (mean)** | **8.0** | Honest adversarial review; implementation and credentialed STOMP verification pending |

## Adversarial review

The following gaps were found by comparing the plan with the current source and rollout topology:

| Severity | Evidence-backed gap | Required plan treatment |
|----------|---------------------|-------------------------|
| Major | Dev ingress references `am-apps-dev-dev-strip-prefix`, but the live `am-apps-dev` namespace has no Middleware resources; Traefik returns `500`. | Add the Middleware to the authoritative dev GitOps manifest, sync it, and verify the exact annotation target before UI rollout. |
| Major | Preprod has a Middleware resource, but its live shared prefix list omits `/gateway`; prod app resources are not visible through the available prod kubeconfig. | Add/verify `/gateway` in preprod, obtain the correct prod app-cluster read-only view or authoritative GitOps evidence, and block promotion on any unresolved target. |
| High | During rollout, cached or already-open UI builds may continue requesting `/v1/streams` while the canonical route is `/gateway/v1/streams`. | Add staged rollout, cache/version check, explicit old-client compatibility window, and rollback criteria. Do not remove the legacy path until the active-client window is verified. |
| Major | P1 token removal depends on the browser actually sending `Authorization` in the STOMP `CONNECT` frame; browser WebSocket handshake headers cannot be assumed. | Capture the browser STOMP frame and assert: no URL token, native `Authorization` present, `101` followed by `CONNECTED`; test missing/expired token behavior separately. |
| High | `StompConnectionCubit` retries the same token every five seconds after disconnect/error, including authentication failures. | Add an auth-failure retry policy: stop or defer retries until token refresh/logout, while retaining bounded transient-network reconnects. |
| High | `AmStompClient` logs the complete connection URL, and `RequestLoggingFilter` logs the raw query string. | Redact client URL/query values and server query strings; invalidate the token shown in the reported browser console before further testing. |
| High | SockJS fallback generates `/info`, `/websocket`, and XHR subpaths; checking only `/gateway/v1/streams-fallback` does not prove fallback works. | Test the generated SockJS subpaths plus CORS response headers through the `/gateway` route. |
| Medium | `ConfigService.override('wsStream')` wins over the new default, and `.env.example` still documents an `AM_WS_STREAM` variable that the code does not read. | Test runtime config precedence for dev/preprod/prod and local overlay; update or remove the stale env example. |
| Medium | Postman’s environment and gateway streaming docs still use direct/alias paths. | Update `am-core-services/postman/AM-Core-Services.postman_environment.json`, `AM-Gateway.postman_collection.json`, `docs/gateway-streaming/README.md`, and `streaming-guide.md` together. |
| Medium | The plan accepts “STOMP rejection” without defining the observable protocol result. | Define failure as no `CONNECTED`, either an STOMP `ERROR` frame or close, no controller side effect, and no Redis/Kafka watch event. |
| Medium | Long-lived connections rely on the existing heartbeat/reconnect behavior but the route test is short-lived. | Add a sustained connection test beyond the 30-second heartbeat interval and verify reconnect/resubscribe for market, dashboard, and portfolio consumers. |
| Out of scope / track | `setAllowedOriginPatterns("*")` is permissive existing gateway behavior, not the cause of this 401. | Keep it out of P0, but record a separate origin-restriction security follow-up rather than silently treating it as solved. |

- Blocker/Major count: 2
- Agent satisfied: `no`

## Rating log (agent architect loop)

| Pass | Delivery | Design | Overall | Gaps then patched |
|------|----------|--------|---------|------------------|
| 2 | 8/10 | 8/10 | 8.2/10 | Reframed P0 around the existing `/gateway` module contract, moved implementation ownership to `am-modern-ui:fix/backlog-market-ui`, and listed exact files; implementation and protocol verification remain pending. |
| 3 | 8/10 | 8/10 | 7.9/10 | Found and added environment-parity, stale-client rollout, browser STOMP-header, auth-retry, query-leak, SockJS-subpath/CORS, config-precedence, and sustained-connection checks. |
| 4 | 8/10 | 8/10 | 8.0/10 | Added read-only preprod/prod route evidence: module-prefixed WebSocket upgrade `101`, legacy path `401`, and SockJS `/info` `200`; credentialed STOMP and live prod middleware inspection remain pending. |
| 5 | 8.5/10 | 8.8/10 | 8.5/10 | Corrected the plan after direct dev and preprod cluster evidence: dev Middleware is missing, preprod omits `/gateway`, prod visibility is limited, and promotion is blocked until all environments are verified. |

## User review gate

Review [PLAN.md](./PLAN.md), [architecture.drawio](./architecture.drawio), and [TODO.md](./TODO.md). Explicit confirmation is required before implementation, commit, or deployment.

## REPORT gate

Do not write `REPORT.md` until the route regression and WebSocket/STOMP test rows are verified.
