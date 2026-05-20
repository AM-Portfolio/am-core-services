# Observability — Developer Onboarding Guide

> Practical, copy-paste handbook for any developer integrating `am-observability-lib` into a service. Read the [ARCHITECTURE.md](ARCHITECTURE.md) first if you want the "why"; this document is the "how".

---

## 0. TL;DR — the 4-step adoption checklist

1. **Add the Maven dependency.**
2. **Replace your `logback-spring.xml` with the shared include.**
3. **Paste the tracing/OTLP block into your `application.yml`.**
4. **Wrap each public entry point** (REST controller method, `@KafkaListener`, scheduled job, MCP tool) **in a `FlowLogger` scope.**

Everything else (MDC, Feign/Kafka/SDK propagation, request access log, global exception handler) is auto-configured.

---

## 1. Add the dependency

In your service's `pom.xml`:

```xml
<dependency>
    <groupId>com.am.libraries</groupId>
    <artifactId>am-observability-lib</artifactId>
    <version>${project.version}</version>
</dependency>
```

That's enough to pull in:

- `io.micrometer:micrometer-tracing-bridge-otel`
- `io.opentelemetry:opentelemetry-exporter-otlp`
- `net.logstash.logback:logstash-logback-encoder`
- The Spring auto-configuration (`ObservabilityAutoConfiguration`).

If your service has its own `spring-boot-starter-actuator`, you keep it; the lib does not override it.

---

## 2. `logback-spring.xml`

Replace whatever your service has at `src/main/resources/logback-spring.xml` with **exactly** this:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <include resource="am-logback-include.xml"/>
</configuration>
```

For `am-mcp-server` (stdio transport — stdout must stay clean for MCP framing), activate the `mcp-stdio` Spring profile and the include automatically switches console output to `STDERR`:

```xml
<configuration>
    <springProfile name="mcp-stdio">
        <include resource="am-logback-include.xml"/>
    </springProfile>
    <springProfile name="!mcp-stdio">
        <include resource="am-logback-include.xml"/>
    </springProfile>
</configuration>
```

(Both branches use the same include; the profile is consumed inside `am-logback-include.xml`.)

---

## 3. `application.yml`

Add this block to `src/main/resources/application.yml` (or `application.yaml`):

```yaml
spring:
  application:
    name: my-service                       # MUST be set — flows the `service` MDC field

management:
  tracing:
    enabled: true
    sampling:
      probability: ${TRACING_SAMPLING_PROBABILITY:1.0}
    propagation:
      type: w3c
  otlp:
    tracing:
      endpoint: ${OTEL_EXPORTER_OTLP_TRACES_ENDPOINT:http://localhost:4318/v1/traces}
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus

otel:
  service:
    name: ${spring.application.name}
    namespace: am-core-services

am:
  observability:
    enabled: true                          # master switch
    request-log:
      enabled: true                        # per-request access log
      ignore-paths: /actuator/**,/favicon.ico
    sanitize:
      preview-bytes: 256
```

For local dev, you don't need to set anything else — the OTLP endpoint defaults to `localhost:4318` (matches `docker-compose.dev.yml`).

### Caller info in JSON logs (Loki / ELK / Datadog)

Every JSON log line includes four extra top-level fields by default:

| Field | Example | Notes |
|---|---|---|
| `caller.class` | `com.am.analysis.controller.AnalysisController` | fully-qualified class name |
| `caller.method` | `getDashboardSummary` | method that invoked the SLF4J logger |
| `caller.file` | `AnalysisController.java` | source file name |
| `caller.line` | `46` | line number of the `log.info(...)` call |

This lets Loki users jump from a noisy log to the exact source line:

```logql
{service="am-analysis"} | json | level="ERROR" | caller_method="getDashboardSummary"
```

(Loki rewrites `caller.line` → `caller_line` on ingestion; the JSON keys stay dotted.)

**Performance note**: logback resolves caller data by building a stack trace per event — a few microseconds per line. Acceptable for our volume. To disable on a hot path service, add the `caller-off` Spring profile:

```yaml
spring:
  profiles:
    include: caller-off
```

or via env: `SPRING_PROFILES_ACTIVE=caller-off,<other profiles>`. The JSON encoders will then skip the `<callerData/>` provider and the four `caller.*` fields disappear from every line.

---

## 4. Use `FlowLogger` at entry points

Inject the logger:

```java
import com.am.observability.flow.FlowLogger;
import com.am.observability.flow.FlowSpan;

@RestController
@RequiredArgsConstructor
public class OrderController {
    private final FlowLogger flowLogger;
    private final OrderService orderService;

    @GetMapping("/v1/orders/{id}")
    public ResponseEntity<Order> getOrder(@PathVariable String id,
                                          @RequestHeader("X-User-Id") String userId) {
        try (FlowSpan span = flowLogger.start("orders.http.get",
                "userId", userId, "orderId", id)) {
            Order order = orderService.findById(id, userId);
            flowLogger.complete(span, "amount", order.getAmount());
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            // Re-throw — global advice handles the response.
            // FlowSpan.close() will auto-emit a failure line if not completed.
            throw e;
        }
    }
}
```

The `try-with-resources` is the contract. If the block exits normally without calling `complete`, the span auto-emits an `ok` line at close. If it exits with an exception and `fail`/`complete` wasn't called, it auto-emits a `err` line with the throwable.

### `FlowLogger` API reference

```java
FlowSpan start(String stepName, Object... kv);     // open a step, push MDC, start timer
void     step (String stepName, Object... kv);     // point-in-time checkpoint inside a step
void     complete(FlowSpan s, Object... kv);       // close with status=ok and elapsed
void     fail (FlowSpan s, Throwable t, Object... kv); // close with status=err, log cause
```

`kv` is a varargs of alternating key/value pairs that go into the JSON log as named fields. Keys are dotted lowercase. Values are auto-stringified.

### Inside Kafka listeners

```java
@KafkaListener(topics = "orders-events", groupId = "billing-service")
public void onOrder(ConsumerRecord<String, String> record) {
    try (FlowSpan span = flowLogger.start("billing.kafka.consume.orders_event",
            "topic", record.topic(),
            "partition", record.partition(),
            "offset", record.offset(),
            "key", record.key())) {
        OrderEvent event = mapper.readValue(record.value(), OrderEvent.class);
        flowLogger.step("billing.process.charge", "amount", event.amount());
        billingService.charge(event);
        flowLogger.complete(span, "amount", event.amount());
    } catch (Exception e) {
        // span auto-fails; rethrow so Spring's error handler runs
        throw e;
    }
}
```

Trace context is **already** restored from the `traceparent` Kafka header by the auto-instrumentation — you don't need to copy headers manually.

### Inside scheduled jobs

```java
@Scheduled(fixedDelay = 60_000)
public void rollupHourly() {
    try (FlowSpan span = flowLogger.start("reports.schedule.rollup_hourly")) {
        long n = reportService.runHourlyRollup();
        flowLogger.complete(span, "rows", n);
    }
}
```

Trace context is created fresh for each scheduled invocation.

---

## 5. Step naming convention (mandatory)

Use `<service>.<area>.<verb>[.detail]`, lowercase, dot-separated:

- service prefix: short module name (`gateway`, `analysis`, `billing`, `orders`, `mcp`, ...).
- area: the transport / subsystem (`http`, `stomp`, `kafka.consume`, `kafka.publish`, `redis`, `mongo`, `schedule`, `tool`, `market`).
- verb: an action (`get`, `list`, `publish`, `consume`, `fetch`, `enrich`, `charge`, ...).
- detail: optional disambiguator (`portfolio_update`, `dashboard.summary`).

Examples (good): `analysis.http.dashboard.summary`, `billing.kafka.consume.orders_event`, `mcp.tool.get_top_movers`.

Examples (bad): `GetOrder`, `do-stuff`, `analysis-summary-fetch`, `AnalysisHttpDashboardSummary`.

A step name is your primary index in log queries. Treat it as a public API of your service.

---

## 6. What you get for free vs what you must do

| Concern | Automatic | Manual |
|---|---|---|
| `traceId`/`spanId` in MDC for every log | yes | – |
| `traceparent` injected on outbound RestTemplate / RestClient / WebClient | yes | – |
| `traceparent` injected on outbound Feign | yes (auto-config in `am-feign-lib`) | – |
| `traceparent` injected on outbound KafkaTemplate / restored on `@KafkaListener` | yes | – |
| `traceparent` injected on `java.net.http` generated SDK (`am-portfolio-client-lib`, `am-market-client-lib`, `am-trade-client-lib`, `am-analysis-client-lib`) | yes (one bean in your `*ClientConfig.java`) | bean must be present (already wired in core libs) |
| `traceparent` in STOMP frames | yes (gateway only) | – |
| Per-request access log line | yes | configurable via `am.observability.request-log.*` |
| Global exception handler returning `ApiResponse.error(...)` | yes | – |
| MDC propagation across `@Async` / `CompletableFuture` | yes via `MdcTaskDecorator` | use the supplied `TaskDecorator` bean if you define a custom `Executor` |
| Business-flow checkpoints (`FlowLogger`) | – | **You write these** at each entry point |
| Step names | – | **You choose them** following section 5 |
| Sensitive-field masking | yes via `Sanitizer` | **Call `Sanitizer.preview(...)`** when logging request/response bodies |

---

## 7. Sensitive data — the do-not-log list

Never log these values at any level:

- JWTs, OAuth tokens, API keys, refresh tokens.
- Passwords (raw or hashed).
- Card numbers, full PAN, CVV.
- Bank account numbers, IBAN.
- Full raw Kafka message bodies at INFO/WARN — log byte size + sanitized preview at DEBUG only.
- Full request body for HTTP at INFO — only headers (filtered by `Sanitizer`) and shape (size, content-type).

Use:

```java
log.debug("payload preview: {}", Sanitizer.preview(rawJson));        // truncated + masked
log.info ("received order size={} bytes user={}", json.length(), userId);
```

The `Sanitizer` masks any field whose name matches: `password`, `pwd`, `secret`, `token`, `authorization`, `apikey`, `api_key`, `pan`, `cvv`, `account`, `iban`, `ssn`.

If you need to add more, extend `am.observability.sanitize.extra-fields` in `application.yml`.

---

## 8. Error handling

You do **not** write per-controller `try/catch` that returns `ResponseEntity.status(500)`. The library installs an `ObservabilityControllerAdvice` that:

1. Catches the throwable.
2. Logs it via `flowLogger.fail(...)` so it inherits the current `flow.id` and `traceId`.
3. Returns `ApiResponse.error(code, message)` from `am-common-lib` with the correct HTTP status.

If you want a custom HTTP status for a domain exception, annotate your exception:

```java
@ResponseStatus(HttpStatus.NOT_FOUND)
public class OrderNotFoundException extends RuntimeException { ... }
```

The advice respects the annotation.

`e.printStackTrace()` is **banned** in service code (the advice will catch and stack-trace anyway, with context).

---

## 9. Outbound HTTP via generated SDKs

If your service consumes one of the OpenAPI-generated client libs (`am-portfolio-client-lib`, `am-market-client-lib`, `am-trade-client-lib`, `am-analysis-client-lib`), the interceptor is already wired in `am-core-services`. Nothing to do.

If your service creates its **own** generated client via `am-sdk-generator`, add this single line in your `*ClientConfig.java`:

```java
@Bean
ApiClient apiClient(TraceContextSdkInterceptor traceInterceptor) {
    ApiClient client = new ApiClient();
    client.setBasePath(baseUrl);
    client.setRequestInterceptor(traceInterceptor);   // <-- this line
    return client;
}
```

`TraceContextSdkInterceptor` is auto-configured by the lib; just inject it.

---

## 10. Running locally with traces

From `am-core-services/`:

```powershell
docker compose -f docker-compose.dev.yml up -d otel-collector jaeger
mvn spring-boot:run -pl services/am-analysis
```

Open the Jaeger UI at <http://localhost:16686>, choose service `am-analysis`, find your trace.

Issue a request:

```powershell
curl -H "traceparent: 00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01" `
     http://localhost:8080/v1/analysis/dashboard/summary?userId=u-123
```

Confirm:

- Each JSON log line in the service shows `"traceId":"0af7651916cd43dd8448eb211c80319c"`.
- Jaeger displays a single trace with spans for HTTP → Mongo → outbound SDK.
- Subscribing in the gateway and triggering a calculation produces a trace that connects gateway → Kafka → analysis.

### grep-friendly query (no UI required)

```powershell
type service-logs.json | jq 'select(.traceId=="0af7651916cd43dd8448eb211c80319c") | "\(.["@timestamp"]) [\(.service)] [\(.["flow.step"] // "-")] \(.message)"'
```

---

## 11. Common questions

**Q. My logs are plain text instead of JSON.**
You forgot step 2 — the `logback-spring.xml` is not including `am-logback-include.xml`. Verify the file content matches section 2 exactly.

**Q. `traceId` is missing from my logs.**
The first few lines emitted during Spring context startup are before the filter chain is initialized — that's expected. Anything inside controller / listener / service code after startup must have it. If not, you're probably logging from a custom thread pool — switch the executor to `am.observability.task.executor` or apply `MdcTaskDecorator` to your `Executor`.

**Q. My downstream service shows a different `traceId`.**
Confirm the call goes through one of: Spring `RestTemplate`/`RestClient`/`WebClient`/Feign, or a generated SDK with `TraceContextSdkInterceptor` wired. If you're using raw `HttpURLConnection` or another HTTP library, you must propagate `traceparent` manually using `Tracer.currentSpan()`.

**Q. I want to start a new trace inside a Kafka listener (for example, for a fan-out).**
Use `tracer.nextSpan().name("billing.fanout").start()` or simply call a service method that's already `@Observed`. Don't generate UUIDs by hand.

**Q. How do I add a custom field to every log line?**
Either put it in MDC inside a `FlowLogger.start(...)` (preferred — scoped) or, for application-wide constants, set `logging.structured.json.context.included-mdc-keys` in `application.yml`. Avoid mutating MDC manually outside flow scopes.

**Q. Performance hit?**
With 100% sampling, expect single-digit % overhead. JSON encoding is the dominant cost — that's why we provide a `text` format escape hatch for dev. In high-throughput services, set `TRACING_SAMPLING_PROBABILITY=0.1`.

**Q. Can I disable observability entirely for a service?**
Yes: `am.observability.enabled=false`. The auto-config will back out and no filters / interceptors are registered.

---

## 12. Migration checklist for an existing service

Use this checklist when retrofitting any pre-existing service:

- [ ] `pom.xml` has `am-observability-lib` dependency.
- [ ] `logback-spring.xml` reduced to `<include resource="am-logback-include.xml"/>`.
- [ ] `application.yml` has the `management.tracing` + `management.otlp` + `otel.service` + `am.observability` blocks.
- [ ] `spring.application.name` is set.
- [ ] Every REST controller method is wrapped in `FlowLogger.start(...)` with an `analysis.http.*`-style step name.
- [ ] Every `@KafkaListener` is wrapped with a `kafka.consume.<topic>` step.
- [ ] Every `@Scheduled` is wrapped with a `schedule.<job>` step.
- [ ] No `e.printStackTrace()` left in code.
- [ ] No full request/response bodies logged at INFO (use `Sanitizer.preview`).
- [ ] No `UUID.randomUUID()` used to populate `traceId`/`spanId` in event payloads — pull from `tracer.currentSpan()` instead.
- [ ] Any generated `ApiClient` has `TraceContextSdkInterceptor` set as its request interceptor.
- [ ] Local run shows the trace in Jaeger and consistent `traceId` in JSON logs.
- [ ] Sampling probability tuned for prod (default `1.0` is for dev only).

---

## 13. Library API quick reference

| Symbol | Package | Purpose |
|---|---|---|
| `FlowLogger` | `com.am.observability.flow` | Inject this. Open business-flow checkpoints. |
| `FlowSpan` | `com.am.observability.flow` | `AutoCloseable` returned by `start(...)`. |
| `FlowMarkers.FLOW` | `com.am.observability.flow` | SLF4J `Marker` for filtering flow vs technical logs. |
| `Sanitizer` | `com.am.observability.sanitize` | `preview(String)`, `mask(Map)`, `maskJson(String)`. |
| `TraceContextSdkInterceptor` | `com.am.observability.http` | Inject into your `*ClientConfig` when building a generated `ApiClient`. |
| `MdcTaskDecorator` | `com.am.observability.mdc` | Apply to custom `ThreadPoolTaskExecutor`s. |
| `ObservabilityControllerAdvice` | `com.am.observability.error` | Already registered globally — no manual use needed. |

---

## 14. Where to file issues / get help

- File issues against `am-observability-lib` in this repo with logs that include the `traceId`.
- For Cursor agents working in another repo: read [ARCHITECTURE.md](ARCHITECTURE.md) first, then this guide.
- The plan file `.cursor/plans/observability_for_am-core-services_*.plan.md` lists the in-progress implementation tasks.
