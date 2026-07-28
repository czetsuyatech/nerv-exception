package com.czetsuyatech.nerv.exception.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.czetsuyatech.nerv.exception.core.NervErrorHeaders;
import com.czetsuyatech.nerv.exception.core.model.NervErrorResponse;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.ResponseEntity;

/**
 * Unit tests for the default {@code build}/{@code addHeader} methods contributed by the
 * {@link NervExceptionHandler} mixin. {@link DefaultNervExceptionHandlerTest} covers the same
 * behaviour end to end through MockMvc.
 */
class NervExceptionHandlerTest {

  private final NervExceptionHandler handler = new NervExceptionHandler() {
  };

  @Test
  void shouldUseTheErrorCodeStatusAsTheResponseStatus() {

    ResponseEntity<NervErrorResponse> entity = handler.build(response().build());

    assertThat(entity.getStatusCode().value()).isEqualTo(409);
  }

  @Test
  void shouldUseTheResponseAsTheBody() {

    NervErrorResponse response = response()
        .details(Map.of("field", "value"))
        .build();

    ResponseEntity<NervErrorResponse> entity = handler.build(response);

    assertThat(entity.getBody()).isSameAs(response);
  }

  @Test
  void shouldExposeCodeCategoryAndRetryableAsHeaders() {

    ResponseEntity<NervErrorResponse> entity = handler.build(response().build());

    assertThat(entity.getHeaders().getFirst(NervErrorHeaders.ERROR_CODE))
        .isEqualTo("CONFLICT");
    assertThat(entity.getHeaders().getFirst(NervErrorHeaders.ERROR_CATEGORY))
        .isEqualTo("CLIENT");
    assertThat(entity.getHeaders().getFirst(NervErrorHeaders.ERROR_RETRYABLE))
        .isEqualTo("false");
  }

  @Test
  void shouldRenderRetryableAsTrueForRetryableErrorCodes() {

    ResponseEntity<NervErrorResponse> entity =
        handler.build(response().retryable(true).build());

    assertThat(entity.getHeaders().getFirst(NervErrorHeaders.ERROR_RETRYABLE))
        .isEqualTo("true");
  }

  @Test
  void shouldExposeTraceAndSpanIdsAsHeaders() {

    NervErrorResponse response = response()
        .traceId("0af7651916cd43dd8448eb211c80319c")
        .spanId("b9c7c989f97918e1")
        .build();

    ResponseEntity<NervErrorResponse> entity = handler.build(response);

    assertThat(entity.getHeaders().getFirst(NervErrorHeaders.TRACE_ID))
        .isEqualTo("0af7651916cd43dd8448eb211c80319c");
    assertThat(entity.getHeaders().getFirst(NervErrorHeaders.SPAN_ID))
        .isEqualTo("b9c7c989f97918e1");
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"", " ", "\t"})
  void shouldOmitTraceAndSpanHeadersWhenTheyAreNullOrBlank(String value) {

    NervErrorResponse response = response()
        .traceId(value)
        .spanId(value)
        .build();

    ResponseEntity<NervErrorResponse> entity = handler.build(response);

    assertThat(entity.getHeaders().containsHeader(NervErrorHeaders.TRACE_ID)).isFalse();
    assertThat(entity.getHeaders().containsHeader(NervErrorHeaders.SPAN_ID)).isFalse();
  }

  @Test
  void shouldOmitOnlyTheMissingTraceHeader() {

    NervErrorResponse response = response()
        .traceId("0af7651916cd43dd8448eb211c80319c")
        .spanId(null)
        .build();

    ResponseEntity<NervErrorResponse> entity = handler.build(response);

    assertThat(entity.getHeaders().containsHeader(NervErrorHeaders.TRACE_ID)).isTrue();
    assertThat(entity.getHeaders().containsHeader(NervErrorHeaders.SPAN_ID)).isFalse();
  }

  private static NervErrorResponse.NervErrorResponseBuilder response() {
    return NervErrorResponse.builder()
        .code("CONFLICT")
        .message("Conflict")
        .status(409)
        .retryable(false)
        .category("CLIENT")
        .path("/api/test")
        .timestamp(Instant.parse("2026-06-23T00:00:00Z"));
  }
}
