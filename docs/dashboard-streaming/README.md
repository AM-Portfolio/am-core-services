# Dashboard Streaming — Documentation

Implementation plan and phased task tracker for live dashboard WebSocket streaming (`am-analysis` + `am-gateway` + **`am-modern-ui`**).

## Documents

| Document | Purpose |
|----------|---------|
| [implementation-plan.md](./implementation-plan.md) | Architecture, three-layer model, Parts A–M, risks, validation checklist |
| [phased-tasks.md](./phased-tasks.md) | **Backend task list** — IDs, status, gates, sprints |
| [ui-phased-tasks.md](./ui-phased-tasks.md) | **Flutter UI task list** — `am_dashboard_ui`, `am_app`, STOMP migration |

## Quick start

1. Read **implementation-plan.md** for design (channel vs global portfolio vs API alias).
2. Execute **backend** tasks from [phased-tasks.md](./phased-tasks.md): **A → C → A0 → D → P1 → P2 → B → E → F**.
3. Execute **UI** tasks from [ui-phased-tasks.md](./ui-phased-tasks.md) in parallel with backend **A0** (Sprint 1).
4. Do not mark a task ✅ until its validation rows pass.

## Current focus

**Sprint 1 (P0):** Backend `A-05`, `A-06`, `A0-01` … `A0-11` + UI `UI-A0-01` … `UI-A0-09`.

## Related docs

- [USER_CONTEXT_SECURITY_GUIDE.md](../USER_CONTEXT_SECURITY_GUIDE.md)
- [observability/DEVELOPER_GUIDE.md](../observability/DEVELOPER_GUIDE.md)
