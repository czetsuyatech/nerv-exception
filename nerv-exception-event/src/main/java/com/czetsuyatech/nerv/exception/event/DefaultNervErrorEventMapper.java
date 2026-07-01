package com.czetsuyatech.nerv.exception.event;

import com.czetsuyatech.nerv.exception.api.NervErrorCode;
import com.czetsuyatech.nerv.exception.core.NervException;
import java.time.Instant;
import java.util.Map;

public class DefaultNervErrorEventMapper implements NervErrorEventMapper {

  private final NervEventTraceContextResolver traceContextResolver;

  public DefaultNervErrorEventMapper(
      NervEventTraceContextResolver traceContextResolver) {

    this.traceContextResolver = traceContextResolver;
  }

  @Override
  public NervErrorEvent from(
      NervException exception,
      String source,
      String topic,
      String key) {

    NervErrorCode errorCode = exception.getErrorCode();

    return new NervErrorEvent(
        errorCode.code(),
        exception.getMessage(),
        errorCode.retryable(),
        errorCode.category(),
        traceContextResolver.traceId(),
        traceContextResolver.spanId(),
        traceContextResolver.parentEventId(),
        source,
        topic,
        key,
        Instant.now(),
        exception.getDetails() == null
            ? Map.of()
            : exception.getDetails());
  }
}
