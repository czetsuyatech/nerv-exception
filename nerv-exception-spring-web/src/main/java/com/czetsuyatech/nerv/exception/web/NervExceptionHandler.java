package com.czetsuyatech.nerv.exception.web;

import com.czetsuyatech.nerv.exception.core.NervErrorHeaders;
import com.czetsuyatech.nerv.exception.core.model.NervErrorResponse;
import org.springframework.http.ResponseEntity;

public interface NervExceptionHandler {

  default ResponseEntity<NervErrorResponse> build(NervErrorResponse response) {

    ResponseEntity.BodyBuilder builder = ResponseEntity
        .status(response.status())
        .header(NervErrorHeaders.ERROR_CODE, response.code())
        .header(NervErrorHeaders.ERROR_CATEGORY, response.category())
        .header(NervErrorHeaders.ERROR_RETRYABLE, String.valueOf(response.retryable()));

    addHeader(builder, NervErrorHeaders.TRACE_ID, response.traceId());
    addHeader(builder, NervErrorHeaders.SPAN_ID, response.spanId());

    return builder.body(response);
  }

  default void addHeader(
      ResponseEntity.BodyBuilder builder,
      String name,
      String value) {

    if (value != null && !value.isBlank()) {
      builder.header(name, value);
    }
  }
}
