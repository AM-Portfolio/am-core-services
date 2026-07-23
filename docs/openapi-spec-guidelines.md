# OpenAPI / springdoc guidelines (SPT + agents)

OpenAPI published by each service (`/v3/api-docs` for Java) is the **contract** for SPT Try-it, load payloads, and agents. Incomplete docs produce bad generated payloads.

SPT registration: [`docs/spt-onboarding.md`](spt-onboarding.md). SPT PoC onboarding: `am-agents/poc/spt/docs/SERVICE-ONBOARDING.md`.

## Definition of done (every public operation)

- Stable `operationId` + `@Operation(summary, description)`
- Every path/query param: `description`, `example` (or enum), correct `required`
- **Closed domain codes** → Java `enum` on the controller/DTO (springdoc emits `enum: […]`)
- **Open catalogs** (portfolio id, symbol) → `String` + realistic `example` — never enum
- Request bodies: typed DTO + `@Schema(example, requiredMode)` — never raw `Map`
- Timestamps: `Instant` / `OffsetDateTime` → `format: date-time`; `LocalDate` → `date`
- Bearer security declared in `OpenAPI` bean
- Spec `info.title` / `info.version` set (not default “OpenAPI definition”)

## Grow later without re-annotating

| Kind | Do this | When it grows |
|------|---------|----------------|
| Closed codes (entity type, timeframe, activity type) | Enum on the API | Add one enum constant |
| Wire ≠ name (`1D`) | `@JsonValue` / `@JsonCreator` on enum | Same |
| Open ids / symbols | `String` + example once | No schema change |
| DB-driven lists | Meta values API or String | Not OpenAPI enum |
| Avoid | `@Schema(allowableValues=…)` on `String` | Forces annotation edits every time |

Canonical timeframe for analysis REST: `com.am.kafka.config.Timeframe` (`1D`…`5Y`).

## Cookbook

```java
@Operation(summary = "…", operationId = "getDashboardTopMovers")
@GetMapping("/dashboard/top-movers")
public ResponseEntity<TopMoversResponse> getDashboardTopMovers(
    @Parameter(description = "Performance window", example = "1D")
    @RequestParam(defaultValue = "1D") Timeframe timeFrame) { … }

@PathVariable AnalysisEntityType type  // not String
```

`SwaggerConfig`: `OpenAPI` info + `Components` security scheme `bearer-jwt`.

## Verify

1. `GET /v3/api-docs` — constrained params show `enum`, times show `format`
2. Swagger UI Try-it — dropdowns for enums; examples prefilled for strings
3. Update gap matrix for the service when closing gaps

## What not to do

- Hand-maintain a parallel OpenAPI YAML as source of truth (springdoc from Java is truth)
- Free `String` for closed sets that already have enums
- Bare `String timestamp` without `date-time` format
