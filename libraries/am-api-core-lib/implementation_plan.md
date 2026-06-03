# Enterprise Centralized Response & Exception Handling Plan

This plan outlines the architecture for a highly modular, 10/10 enterprise-grade centralized response and exception handler for the Java/Spring Boot microservices. 

The goal is to provide a "plug-and-play" mechanism where downstream services require **zero-to-minimal code changes** while enforcing a strict, uniform API contract for all 2XX (Success), 4XX (Client Errors), and 5XX (Server Errors) responses.

## User Review Required

> [!IMPORTANT]
> Please review the proposed **Standard Response Envelope** and the **Library Name** (`am-web-core-lib`). Once approved, this library will be built and distributed to all Java microservices.

## Open Questions

> [!WARNING]
> 1. Do you have a specific standard for error codes (e.g., `AM-ERR-4004`) or should we generate standard HTTP-based codes (`BAD_REQUEST`, `NOT_FOUND`)?
> 2. For the 2XX wrapper, do you want to wrap *all* responses by default, or only responses that are not already wrapped in a standard `ApiResponse` object? (The plan assumes wrapping all unwrapped objects automatically).
> 3. Does the system use Micrometer Tracing or Spring Cloud Sleuth for the `trace_id` generation in MDC?

## Architectural Design

To achieve a 10/10 modularity rating, we will not pollute existing services with boilerplate. Instead, we will create a dedicated shared library (Spring Boot Auto-configuration module) that services simply include as a dependency.

### 1. The Standard Envelope (The Contract)
All APIs will return a uniform structure regardless of success or failure.

```json
{
  "status": "SUCCESS", // or "ERROR"
  "data": { ... },     // The actual payload (null on error)
  "error": {           // Null on success
    "code": "ERR_VALIDATION",
    "message": "Invalid input provided",
    "details": ["'email' must be a valid email address"]
  },
  "meta": {
    "traceId": "a1b2c3d4e5f6", // Extracted automatically from MDC
    "timestamp": "2026-06-03T10:15:30Z"
  }
}
```

### 2. Handling 2XX (Success Series)
Instead of forcing developers to wrap their responses in controllers (e.g., `return new ApiResponse<>(myObject)`), they will simply return the raw DTO. 
We will implement a `ResponseBodyAdvice<Object>` that intercepts the outgoing response before it hits the message converter, automatically wrapping it in the standard envelope if it's a 2XX response.

### 3. Handling 4XX & 5XX (Error Series)
We will implement a global `@RestControllerAdvice` that catches exceptions and translates them into the standard envelope with `status: "ERROR"`.
- **4XX Series:** Handled by catching `MethodArgumentNotValidException`, `ConstraintViolationException`, `NoHandlerFoundException`, `AccessDeniedException`, and custom domain exceptions (e.g., `ResourceNotFoundException`).
- **5XX Series:** Handled by a fallback `@ExceptionHandler(Exception.class)` which logs the full stack trace (for machines) and returns a generic "Internal Server Error" message with the `traceId` to the client.

### 4. Modularity & Plug-and-Play (The Magic)
We will package this into `am-web-core-lib`. 
By leveraging Spring's `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`, any service that adds this library to its `pom.xml` will automatically inherit the global exception handler and response wrapper without needing `@Import` or component scanning.

---

## Proposed Changes

### `am-core-services/libraries/am-web-core-lib`

This will be a new Maven module (or integrated into an existing shared web module).

#### [NEW] `pom.xml`
Defines dependencies: `spring-boot-starter-web`, `spring-boot-starter-validation`, `slf4j-api`.

#### [NEW] `com/portfolio/web/model/ApiResponse.java`
The generic wrapper class containing `status`, `data`, `error`, and `meta`.

#### [NEW] `com/portfolio/web/model/ErrorDetails.java`
DTO for the error object containing `code`, `message`, and `details` list.

#### [NEW] `com/portfolio/web/advice/GlobalResponseWrapper.java`
Implements `ResponseBodyAdvice`. Intercepts all successful controller returns and wraps them in `ApiResponse.success(body)`.

#### [NEW] `com/portfolio/web/advice/GlobalExceptionHandler.java`
The `@RestControllerAdvice` class containing methods like:
- `handleValidationExceptions(...)` -> 400
- `handleAccessDenied(...)` -> 403
- `handleDomainExceptions(...)` -> 400/404/409 (based on a base exception class)
- `handleAllExceptions(...)` -> 500 (Catch-all)

#### [NEW] `com/portfolio/web/exception/BaseDomainException.java`
An abstract base exception class that developers can extend to throw business logic errors (e.g., `AccountLockedException`).

#### [NEW] `com/portfolio/web/config/AmWebAutoConfiguration.java`
The `@AutoConfiguration` class that conditionally loads the advices.

#### [NEW] `src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
Registers `AmWebAutoConfiguration` so it boots automatically when the JAR is present.

---

## Verification Plan

### Automated Tests
- Create a dummy `@RestController` inside the library's test scope.
- Write `@WebMvcTest` cases to assert:
  - Returning a string/object is wrapped in `{"status": "SUCCESS", "data": ...}`.
  - Throwing a generic `RuntimeException` results in a 500 status and the `{"status": "ERROR"}` envelope.
  - Triggering a validation failure results in a 400 status with `error.details` populated.

### Integration Verification
- Add the library dependency to `am-gateway` or `am-analysis`.
- Trigger an existing endpoint and verify the JSON output structure has seamlessly upgraded to the new enterprise standard.
