# OpenAPI gap matrix — am-analysis

Audit date: 2026-07-23. Source: `AnalysisController` + springdoc defaults (pre-fix).

| Operation | Path | Gaps (before) | Fix (Phase A) |
|-----------|------|---------------|---------------|
| getDashboardSummary | GET `/v1/analysis/dashboard/summary` | No `@Operation`; generic spec info | Annotate + SwaggerConfig |
| getPortfolioOverviews | GET `.../portfolio-overviews` | `portfolioId` no example | `@Parameter(example)` |
| publishDashboardUpdate | POST `.../publish-update` | No op docs; empty body OK | `@Operation` |
| getDashboardTopMovers | GET `.../top-movers` | `timeFrame` as String | `Timeframe` enum |
| getDashboardPerformance | GET `.../performance` | `timeFrame` as String | `Timeframe` enum |
| getRecentActivity | GET `.../recent-activity` | `type`/`status`/`sortBy` as String | `ActivityType` / `ActivityStatus` / `ActivitySortBy` |
| getAllocation | GET `/{type}/{id}/allocation` | `type` String; `id` no example | `AnalysisEntityType` + id example |
| getPerformance | GET `/{type}/{id}/performance` | same | enums + Timeframe |
| getTopMoversByCategory | GET `/{type}/top-movers` | same | enums |
| getTopMoversByEntity | GET `/{type}/{id}/top-movers` | same | enums |
| Spec metadata | `/v3/api-docs` | title "OpenAPI definition" / v0 | SwaggerConfig |
| Security | all | bearer not declared in OpenAPI bean | bearer scheme in SwaggerConfig |
| Response DTOs | various | no `@Schema` examples | annotate key DTOs |

## Status after Phase A

| Check | Status |
|-------|--------|
| Stable operationId | Fixed via `@Operation(operationId=…)` |
| Param enums on wire | Fixed for type / timeFrame / activity filters |
| Open ids stay String + example | Fixed |
| Guidelines doc | See `openapi-spec-guidelines.md` |
| Compile | `mvn -pl services/am-analysis -am compile` OK (2026-07-24) |
| Live `/v3/api-docs` | **Verify locally** after starting the service |
