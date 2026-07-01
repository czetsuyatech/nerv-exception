package com.czetsuyatech.nerv.exception.kafka;

import static org.assertj.core.api.Assertions.assertThat;

import com.czetsuyatech.nerv.exception.event.NervErrorEvent;
import com.czetsuyatech.nerv.exception.event.NervEventErrorHeaders;
import java.nio.charset.StandardCharsets;
import org.apache.kafka.common.header.Headers;
import org.junit.jupiter.api.Test;

class NervKafkaHeaderMapperTest {

  private final NervKafkaHeaderMapper mapper = new NervKafkaHeaderMapper();

  @Test
  void shouldMapEventToKafkaHeaders() {
    NervErrorEvent event = NervErrorEvent.builder()
        .traceId("trace-123")
        .spanId("span-456")
        .source("payment-service")
        .parentEventId("parent-event-1")
        .build();

    Headers headers = mapper.from(event);

    assertThat(headerValue(headers, NervEventErrorHeaders.TRACE_ID))
        .isEqualTo("trace-123");

    assertThat(headerValue(headers, NervEventErrorHeaders.SPAN_ID))
        .isEqualTo("span-456");

    assertThat(headerValue(headers, NervEventErrorHeaders.SOURCE))
        .isEqualTo("payment-service");

    assertThat(headerValue(headers, NervEventErrorHeaders.PARENT_EVENT_ID))
        .isEqualTo("parent-event-1");
  }

  @Test
  void shouldSkipNullValues() {
    NervErrorEvent event = NervErrorEvent.builder()
        .traceId("trace-123")
        .spanId(null)
        .build();

    Headers headers = mapper.from(event);

    assertThat(headers.lastHeader(NervEventErrorHeaders.TRACE_ID))
        .isNotNull();

    assertThat(headers.lastHeader(NervEventErrorHeaders.SPAN_ID))
        .isNull();
  }

  private String headerValue(Headers headers, String name) {
    return new String(
        headers.lastHeader(name).value(),
        StandardCharsets.UTF_8
    );
  }
}
