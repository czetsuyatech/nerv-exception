package com.czetsuyatech.nerv.exception.trace;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;

public class MicrometerNervTraceContextResolver implements NervTraceContextResolver {

  private final Tracer tracer;

  public MicrometerNervTraceContextResolver(Tracer tracer) {
    this.tracer = tracer;
  }

  @Override
  public NervTraceContext current() {
    Span span = tracer.currentSpan();

    if (span == null) {
      return NervTraceContext.empty();
    }

    return new NervTraceContext(
        span.context().traceId(),
        span.context().spanId()
    );
  }
}
