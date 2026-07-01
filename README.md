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
