# NERV Exception

A lightweight, production-ready exception handling framework for Spring applications with first-class support for HTTP APIs, OpenFeign, Kafka, distributed tracing, retry-aware error propagation, and standardized error contracts.

`nerv-exception` provides a consistent failure model across synchronous and asynchronous communication channels while remaining framework-agnostic at its core.

---

## Why NERV Exception?

Most distributed systems suffer from inconsistent error handling:

* Different services return different error payloads
* Retry behavior is undocumented
* Feign clients lose downstream context
* Kafka failures are difficult to correlate
* Trace information is not consistently exposed
* Error codes are scattered across services

NERV Exception solves these problems through a unified error contract built around `NervErrorCode`.

---

## Features

### Core

* Centralized exception handling
* Strongly typed application error codes
* Standardized error responses
* Retry-aware error contracts
* Error categorization
* Extensible architecture
* Framework-agnostic core API

### Spring Web

* Automatic MVC exception handling
* Consistent HTTP error payloads
* Automatic status mapping
* Distributed trace exposure

### OpenFeign

* Automatic error decoding
* Retry-aware exception conversion
* Downstream error preservation
* Custom error code registries

### Kafka

* Structured error events
* Dead-letter queue publishing
* Trace-aware error propagation
* Kafka header mapping

### Observability

* Micrometer Tracing integration
* OpenTelemetry integration
* Distributed trace support
* Span-aware diagnostics

### Spring Boot

* Zero-configuration setup
* Auto-configuration
* Conditional integrations
* No component scanning

### Security Exception Handling

`nerv-exception` provides built-in handling for the core Spring Security exceptions:

| Spring Security Exception | Native Error Code       |        HTTP Status |
| ------------------------- | ----------------------- | -----------------: |
| `AuthenticationException` | `AUTHENTICATION_FAILED` | `401 Unauthorized` |
| `AccessDeniedException`   | `ACCESS_DENIED`         |    `403 Forbidden` |

These exceptions represent the fundamental authentication and authorization semantics supported by Spring Security and are handled automatically by the library.

### Newer Spring Security Exceptions

Newer versions of Spring Security introduce additional exception types such as `AuthorizationDeniedException`. Since these are framework-specific implementations rather than core security abstractions, they are **not handled by the library by default**.

If your application uses these newer APIs, register an application-specific exception handler with a higher precedence:

```java
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityExceptionHandler {

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<NervErrorResponse> handle(
            AuthorizationDeniedException ex,
            HttpServletRequest request) {

        NervException nervException =
                NervException.of(NativeNervErrorCodes.ACCESS_DENIED, ex);

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(NervErrorResponseMapper.toResponse(nervException, request));
    }
}
```

This approach keeps `nerv-exception` independent of framework-specific implementations while allowing applications to support newer Spring Security features without changing the library.


---

## Architecture

```text
HTTP Request
    ↓
NervException
    ↓
NervExceptionHandler
    ↓
NervErrorResponse
    ↓
Feign
    ↓
NervFeignErrorDecoder
    ↓
RetryableException / NervDownstreamException
    ↓
Kafka
    ↓
NervErrorEvent
    ↓
DLQ
```

---

## Modules

| Module                               | Description                                  |
| ------------------------------------ | -------------------------------------------- |
| `nerv-exception-api`                 | Core contracts and abstractions              |
| `nerv-exception-core`                | Base exceptions, mappers, native error codes |
| `nerv-exception-spring-web`          | Spring MVC integration                       |
| `nerv-exception-spring-feign`        | OpenFeign integration                        |
| `nerv-exception-event`               | Error event model and event mapping          |
| `nerv-exception-spring-kafka`        | Kafka integration and DLQ publishing         |
| `nerv-exception-spring-boot-starter` | Auto-configuration                           |

---

## Module Dependency Flow

```text
starter
 ├─ spring-web
 │   └─ core
 │       └─ api
 │
 ├─ spring-feign
 │   └─ core
 │       └─ api
 │
 └─ spring-kafka
     └─ event
         └─ core
             └─ api
```

---

## Design Principles

### No Component Scanning

NERV Exception never relies on package scanning.

### No Enable Annotations

No:

```java
@EnableNervException
```

is required.

### Auto Configuration Only

All beans are created through:

```java
NervExceptionAutoConfiguration
```

### Optional Integrations

Feign, Kafka, and tracing support activate only when their dependencies are available.

### Retry Is Part of the Contract

Retryability belongs to the error code itself.

### Tracing Is Delegated

Tracing is provided by Micrometer/OpenTelemetry.

NERV Exception consumes trace information but does not implement a tracing system.

---

# Installation

## Maven

```xml
<dependency>
    <groupId>com.czetsuyatech</groupId>
    <artifactId>nerv-exception-spring-boot-starter</artifactId>
    <version>${nerv-exception.version}</version>
</dependency>
```

---

# Configuration

The following values match the defaults in `NervExceptionProperties`. Override them in your application's `application.yml` as needed:

```yaml
nerv:
  exception:
    enabled: true
    include-details: true
    expose-internal-message: true
    include-cause: true
    include-stack-trace: false
    kafka:
      enabled: true
      source: application
      dlq-topic-suffix: .DLQ
```

| Property (under `nerv.exception`) | Default | Purpose |
| -------------------------------- | ------- | ------- |
| `enabled` | `true` | Enable NERV exception handling. |
| `include-details` | `true` | Include exception details in the response. |
| `expose-internal-message` | `true` | Expose the original message for non-NERV exceptions where supported by the mapper. |
| `include-cause` | `true` | Include the cause class name in details when a cause is available. |
| `include-stack-trace` | `false` | Include the stack trace in details. |
| `kafka.enabled` | `true` | Enable Kafka integration when its required dependencies and beans are available. |
| `kafka.source` | `application` | Source name for Kafka error events. |
| `kafka.dlq-topic-suffix` | `.DLQ` | Suffix appended to dead-letter topic names. |

`expose-internal-message` and `include-cause` now default to `true`. Set both to `false` to retain the previous defaults.

## Service Origin and Version

The default Spring Boot origin resolver populates `origin` using:

| Field | Source | Fallback |
| ----- | ------ | -------- |
| `service` | `spring.application.name` | `unknown-service` |
| `instance` | Local hostname | `unknown-instance` |
| `version` | Spring Boot `BuildProperties.getVersion()` | `unknown-version` |
| `environment` | Active Spring profiles, joined with commas | `default` |

To capture the service version, add a `build-info` execution to **`org.springframework.boot:spring-boot-maven-plugin` in the executable service module's `pom.xml`** (for example, `app/pom.xml` in a multi-module application):

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
            <executions>
                <execution>
                    <id>build-info</id>
                    <goals>
                        <goal>build-info</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

If the plugin already exists, merge this execution into its existing `<executions>` and retain its configuration and other executions. The plugin version is managed when using `spring-boot-starter-parent`.

The goal generates `target/classes/META-INF/build-info.properties` from the service's Maven project metadata, including `${project.version}`. Spring Boot loads that resource into a `BuildProperties` bean, which NERV uses automatically. See the [Spring Boot build-info documentation](https://docs.spring.io/spring-boot/maven-plugin/build-info.html).

Rebuild and restart the service after adding the execution. For local IDE runs, execute Maven's `process-resources` phase before launching and ensure the generated resources are on the runtime classpath. From a multi-module project root with an `app` module:

```shell
mvn -pl app -am process-resources
```

For a service with Maven version `2.9.0-SNAPSHOT`, the response can then contain:

```json
"origin": {
  "service": "nba-responses-api",
  "instance": "Raiden",
  "version": "2.9.0-SNAPSHOT",
  "environment": "local"
}
```

If `version` remains `unknown-version`, verify that the generated file contains `build.version` and is present on the running application's classpath. Configure build information in each executable service module so NERV captures that service's version.

---

# Quick Start

## Define an Error Code

```java
public enum PaymentErrorCode implements NervErrorCode {

    PAYMENT_TIMEOUT(
        "PAYMENT_TIMEOUT",
        "Payment provider timed out",
        504,
        true,
        "INTEGRATION"
    ),

    PAYMENT_NOT_FOUND(
        "PAYMENT_NOT_FOUND",
        "Payment not found",
        404,
        false,
        "BUSINESS"
    );
}
```

---

## Throw an Exception

```java
throw new NervException(PaymentErrorCode.PAYMENT_TIMEOUT);
```

---

## Standard HTTP Response

```json
{
  "code": "PAYMENT_TIMEOUT",
  "message": "Payment provider timed out",
  "status": 504,
  "retryable": true,
  "category": "INTEGRATION",
  "traceId": "0af7651916cd43dd8448eb211c80319c",
  "spanId": "b9c7c989f97918e1",
  "path": "/payments/timeout",
  "timestamp": "2026-06-23T00:00:00Z"
}
```

---

# NervErrorCode

The entire framework revolves around a single abstraction:

```java
public interface NervErrorCode {

    String code();

    String message();

    int status();

    boolean retryable();

    String category();
}
```

Every error in the system derives from this contract.

---

# Spring Web Integration

Enable:

```yaml
nerv:
  exception:
    web:
      enabled: true
```

Features:

* Global exception handling
* Automatic status mapping
* Standardized responses
* Trace-aware diagnostics

No additional configuration is required.

---

# OpenFeign Integration

Enable:

```yaml
nerv:
  exception:
    feign:
      enabled: true
```

Features:

* Automatic error decoding
* Retry-aware exception conversion
* Custom error code resolution
* Downstream error preservation

---

## Retry-Aware Error Decoding

When a downstream service returns:

```json
{
  "code": "PAYMENT_TIMEOUT",
  "retryable": true
}
```

`NervFeignErrorDecoder` automatically converts the response into:

```java
RetryableException
```

allowing retry frameworks such as:

* Resilience4j
* Spring Retry
* Feign Retryer

to retry the request.

Non-retryable errors become:

```java
NervDownstreamException
```

---

## Downstream Error Preservation

Remote failures preserve:

* traceId
* spanId
* timestamp
* path
* details

allowing easier troubleshooting across service boundaries.

---

# Error Code Registry

Applications can register custom error code enums.

```java
@Bean
NervErrorCodeRegistry applicationErrorCodeRegistry() {
    return new EnumNervErrorCodeRegistry(
        PaymentErrorCode.values(),
        CustomerErrorCode.values(),
        OrderErrorCode.values()
    );
}
```

Resolution order:

```text
Application Registry
        ↓
Native Registry
```

Duplicate error codes are detected during startup.

---

# Error Categories

| Category       | Definition                                                                      | Typical HTTP Status                                                                                                           |
| -------------- | ------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------- |
| **VALIDATION** | The request is invalid and must be corrected before retrying.                   | **400 Bad Request**, **422 Unprocessable Entity**                                                                             |
| **BUSINESS**   | The request is valid but cannot be completed due to business or resource state. | **404 Not Found**, **409 Conflict**, **410 Gone**, **422 Unprocessable Entity**                                               |
| **SECURITY**   | Authentication or authorization failure.                                        | **401 Unauthorized**, **403 Forbidden**                                                                                       |
| **DEPENDENCY** | Failure caused by a downstream service or infrastructure dependency.            | **408 Request Timeout**, **429 Too Many Requests**, **502 Bad Gateway**, **503 Service Unavailable**, **504 Gateway Timeout** |
| **SYSTEM**     | Unexpected application failure or configuration problem.                        | **500 Internal Server Error**                                                                                                 |


## Examples

| Exception                          | Category   | HTTP |
|------------------------------------| ---------- | ---- |
| `MethodArgumentNotValidException`  | VALIDATION | 400  |
| `ConstraintViolationException`     | VALIDATION | 400  |
| `HttpMessageNotReadableException`  | VALIDATION | 400  |
| `IllegalArgumentException`         | VALIDATION | 400  |
| `EntityNotFoundException`          | BUSINESS   | 404  |
| `DuplicateKeyException`            | BUSINESS   | 409  |
| `OptimisticLockException`          | BUSINESS   | 409  |
| `BusinessRuleViolationException`   | BUSINESS   | 422  |
| `BadCredentialsException`          | SECURITY   | 401  |
| `AccessDeniedException`            | SECURITY   | 403  |
| `FeignException.BadGateway`        | DEPENDENCY | 502  |
| `FeignException.ServiceUnavailable` | DEPENDENCY | 503  |
| `SocketTimeoutException`           | DEPENDENCY | 504  |
| `CannotGetJdbcConnectionException` | DEPENDENCY | 503  |
| `KafkaException`                   | DEPENDENCY | 503  |
| `NullPointerException`             | SYSTEM     | 500  |
| `IllegalStateException`            | SYSTEM     | 500  |
| `Configuration/startup errors`      | SYSTEM     | 500  |


---

# Kafka Integration

Enable:

```yaml
nerv:
  exception:
    kafka:
      enabled: true
      source: payment-service
      dlq-topic-suffix: .dlq
```

Features:

* Structured error events
* Dead-letter publishing
* Kafka header mapping
* Trace-aware diagnostics

---

## Error Event

```json
{
  "code": "PAYMENT_TIMEOUT",
  "message": "Payment provider timed out",
  "category": "INTEGRATION",
  "retryable": true,
  "source": "payment-service",
  "traceId": "0af7651916cd43dd8448eb211c80319c",
  "spanId": "b9c7c989f97918e1",
  "parentEventId": "payment-timeout-failed",
  "timestamp": "2026-06-23T00:00:00Z"
}
```

---

## Kafka Headers

| Header                 | Description             |
| ---------------------- | ----------------------- |
| `nerv-trace-id`        | Trace identifier        |
| `nerv-span-id`         | Span identifier         |
| `nerv-source`          | Originating service     |
| `nerv-parent-event-id` | Parent event identifier |
| `nerv-error-code`      | Error code              |
| `nerv-error-category`  | Error category          |

---

# Distributed Tracing

NERV Exception integrates with Micrometer Tracing.

Supported providers include:

* OpenTelemetry
* Brave
* Custom Micrometer implementations

Tracing is exposed through:

```java
public interface NervTraceContextResolver {

    NervTraceContext current();
}
```

Default implementation:

```java
MicrometerNervTraceContextResolver
```

Fallback:

```java
NoOpNervTraceContextResolver
```

---

## OpenTelemetry

Recommended dependency:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-opentelemetry</artifactId>
</dependency>
```

Propagation uses standard OpenTelemetry headers:

```text
traceparent
tracestate
```

No custom propagation configuration is required.

---

# Extension Points

| Contract                        | Purpose                         |
| ------------------------------- | ------------------------------- |
| `NervErrorCode`                 | Application error codes         |
| `NervErrorCodeRegistry`         | Remote error code resolution    |
| `NervTraceContextResolver`      | Distributed tracing integration |
| `NervEventTraceContextResolver` | Event tracing integration       |

---

# Requirements

* Java 21+
* Spring Boot 4.x

Optional:

* OpenFeign
* Apache Kafka
* Micrometer Tracing
* OpenTelemetry

---

# License

Apache License 2.0
