package com.czetsuyatech.nerv.exception.trace;

public record NervTraceContext(
    String traceId,
    String spanId
) {

  public static NervTraceContext empty() {
    return new NervTraceContext(null, null);
  }

  public boolean hasTraceId() {
    return traceId != null && !traceId.isBlank();
  }

  public boolean hasSpanId() {
    return spanId != null && !spanId.isBlank();
  }
}
