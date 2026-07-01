package com.czetsuyatech.nerv.exception.event;

import java.util.LinkedHashMap;
import java.util.Map;

public final class NervEventHeaderMapper {

  private NervEventHeaderMapper() {
  }

  public static Map<String, String> headers(NervErrorEvent event) {

    Map<String, String> headers = new LinkedHashMap<>();

    putIfPresent(headers, NervEventErrorHeaders.SOURCE, event.source());
    putIfPresent(headers, NervEventErrorHeaders.ERROR_CODE, event.code());
    putIfPresent(headers, NervEventErrorHeaders.ERROR_CATEGORY, event.category());
    putIfPresent(headers, NervEventErrorHeaders.ERROR_RETRYABLE, String.valueOf(event.retryable()));
    putIfPresent(headers, NervEventErrorHeaders.TRACE_ID, event.traceId());
    putIfPresent(headers, NervEventErrorHeaders.SPAN_ID, event.spanId());
    putIfPresent(headers, NervEventErrorHeaders.PARENT_EVENT_ID, event.parentEventId());

    return Map.copyOf(headers);
  }

  private static void putIfPresent(
      Map<String, String> headers,
      String key,
      String value) {

    if (value != null && !value.isBlank()) {
      headers.put(key, value);
    }
  }
}
