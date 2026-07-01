package com.czetsuyatech.nerv.exception.feign;

import static org.assertj.core.api.Assertions.assertThat;

import com.czetsuyatech.nerv.exception.core.registry.NativeNervErrorCodeRegistry;
import com.czetsuyatech.nerv.exception.core.NervDownstreamException;
import com.czetsuyatech.nerv.exception.core.NervException;
import com.czetsuyatech.nerv.exception.core.code.NativeNervErrorCodes;
import com.czetsuyatech.nerv.exception.core.model.NervErrorResponse;
import feign.Request;
import feign.Response;
import feign.RetryableException;
import feign.codec.ErrorDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class NervFeignErrorDecoderTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  private final ErrorDecoder fallbackDecoder =
      (methodKey, response) -> new IllegalStateException("fallback");

  private final NervFeignErrorDecoder decoder = new NervFeignErrorDecoder(
      objectMapper,
      new NativeNervErrorCodeRegistry(),
      fallbackDecoder);

  @Test
  void shouldRestoreRegisteredErrorCodeForClientError() throws Exception {

    Response response = response(
        409,
        errorResponse(NativeNervErrorCodes.CONFLICT));

    Exception exception = decoder.decode("TestClient#create", response);

    assertThat(exception).isInstanceOf(NervException.class);

    NervException nervException = (NervException) exception;

    assertThat(nervException.getErrorCode())
        .isEqualTo(NativeNervErrorCodes.CONFLICT);

    assertThat(nervException.getMessage())
        .isEqualTo(NativeNervErrorCodes.CONFLICT.message());
  }

  @Test
  void shouldWrapRetryableServerErrorAsRetryableException() throws Exception {

    Response response = response(
        502,
        errorResponse(NativeNervErrorCodes.DOWNSTREAM_SERVICE_ERROR));

    Exception exception = decoder.decode("TestClient#get", response);

    assertThat(exception).isInstanceOf(RetryableException.class);
  }

  @Test
  void shouldWrapNonRetryableServerErrorAsDownstreamException() throws Exception {

    Response response = response(
        500,
        errorResponse(NativeNervErrorCodes.INTERNAL_SERVER_ERROR));

    Exception exception = decoder.decode("TestClient#get", response);

    assertThat(exception).isInstanceOf(NervDownstreamException.class);
  }

  @Test
  void shouldUseFallbackDecoderWhenBodyIsMissing() {

    Response response = response(500, null);

    Exception exception = decoder.decode("TestClient#get", response);

    assertThat(exception)
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("fallback");
  }

  @Test
  void shouldUseFallbackDecoderWhenBodyIsNotNervErrorResponse() {

    Response response = response(500, "not-json");

    Exception exception = decoder.decode("TestClient#get", response);

    assertThat(exception)
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("fallback");
  }

  private String errorResponse(NativeNervErrorCodes errorCode) throws Exception {

    NervErrorResponse response = NervErrorResponse.builder()
        .code(errorCode.code())
        .message(errorCode.message())
        .status(errorCode.status())
        .retryable(errorCode.retryable())
        .category(errorCode.category())
        .traceId("trace-123")
        .spanId("span-456")
        .path("/downstream")
        .timestamp(Instant.now())
        .details(Map.of("field", "value"))
        .build();

    return objectMapper.writeValueAsString(response);
  }

  private Response response(
      int status,
      String body) {

    Response.Builder builder = Response.builder()
        .status(status)
        .reason("error")
        .request(request());

    if (body != null) {
      builder.body(body, StandardCharsets.UTF_8);
    }

    return builder.build();
  }

  private Request request() {

    return Request.create(
        Request.HttpMethod.GET,
        "http://localhost/test",
        Map.of(),
        null,
        StandardCharsets.UTF_8,
        null);
  }
}
