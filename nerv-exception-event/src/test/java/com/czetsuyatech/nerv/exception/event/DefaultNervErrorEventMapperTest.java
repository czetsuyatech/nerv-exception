package com.czetsuyatech.nerv.exception.event;

import static org.assertj.core.api.Assertions.assertThat;

import com.czetsuyatech.nerv.exception.core.NervException;
import com.czetsuyatech.nerv.exception.core.code.NativeNervErrorCodes;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DefaultNervErrorEventMapperTest {

  @Test
  void shouldMapTraceFieldsFromResolver() {

    NervEventTraceContextResolver resolver = new StubTraceContextResolver(
        "trace-1", "span-1", "parent-event-1");

    DefaultNervErrorEventMapper mapper = new DefaultNervErrorEventMapper(resolver);

    NervException exception = NervException.of(NativeNervErrorCodes.BAD_REQUEST);

    NervErrorEvent event = mapper.from(exception, "service-a", "dlq.topic", "key-1");

    assertThat(event.traceId()).isEqualTo("trace-1");
    assertThat(event.spanId()).isEqualTo("span-1");
    assertThat(event.parentEventId()).isEqualTo("parent-event-1");
  }

  @Test
  void shouldMapExceptionFieldsCorrectly() {

    NervEventTraceContextResolver resolver = new StubTraceContextResolver(null, null, null);

    DefaultNervErrorEventMapper mapper = new DefaultNervErrorEventMapper(resolver);

    NervException exception = NervException.of(
        NativeNervErrorCodes.BAD_REQUEST, "bad input", Map.<String, Object>of("field", "name"));

    NervErrorEvent event = mapper.from(exception, "service-a", "dlq.topic", "key-1");

    assertThat(event.code()).isEqualTo(NativeNervErrorCodes.BAD_REQUEST.code());
    assertThat(event.message()).isEqualTo("bad input");
    assertThat(event.retryable()).isEqualTo(NativeNervErrorCodes.BAD_REQUEST.retryable());
    assertThat(event.category()).isEqualTo(NativeNervErrorCodes.BAD_REQUEST.category());
    assertThat(event.source()).isEqualTo("service-a");
    assertThat(event.topic()).isEqualTo("dlq.topic");
    assertThat(event.key()).isEqualTo("key-1");
    assertThat(event.details()).containsEntry("field", "name");
  }

  @Test
  void shouldUseEmptyDetailsWhenExceptionHasNone() {

    NervEventTraceContextResolver resolver = new StubTraceContextResolver(null, null, null);

    DefaultNervErrorEventMapper mapper = new DefaultNervErrorEventMapper(resolver);

    NervException exception = NervException.of(NativeNervErrorCodes.INTERNAL_SERVER_ERROR);

    NervErrorEvent event = mapper.from(exception, "service-a", "dlq.topic", "key-1");

    assertThat(event.details()).isEmpty();
  }

  @Test
  void shouldHandleNullTraceFieldsFromResolver() {

    NervEventTraceContextResolver resolver = new NoOpNervEventTraceContextResolver();

    DefaultNervErrorEventMapper mapper = new DefaultNervErrorEventMapper(resolver);

    NervException exception = NervException.of(NativeNervErrorCodes.BAD_REQUEST);

    NervErrorEvent event = mapper.from(exception, "service-a", "dlq.topic", "key-1");

    assertThat(event.traceId()).isNull();
    assertThat(event.spanId()).isNull();
    assertThat(event.parentEventId()).isNull();
  }

  private record StubTraceContextResolver(
      String traceId,
      String spanId,
      String parentEventId) implements NervEventTraceContextResolver {
  }
}
