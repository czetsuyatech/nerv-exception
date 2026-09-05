package com.czetsuyatech.nerv.exception.feign;

import static org.assertj.core.api.Assertions.assertThat;

import com.czetsuyatech.nerv.exception.core.registry.NativeNervErrorCodeRegistry;
import com.czetsuyatech.nerv.exception.api.origin.NervOrigin;
import com.czetsuyatech.nerv.exception.core.code.RemoteNervErrorCode;
import com.czetsuyatech.nerv.exception.core.NervDownstreamException;
import com.czetsuyatech.nerv.exception.core.NervException;
import com.czetsuyatech.nerv.exception.core.code.NativeNervErrorCodes;
import com.czetsuyatech.nerv.exception.core.model.NervErrorResponse;
import feign.Request;
import feign.FeignException;
import feign.Response;
import feign.RetryableException;
import feign.codec.ErrorDecoder;
import java.nio.charset.StandardCharsets;
import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
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

  @ParameterizedTest
  @CsvSource({"422,422,false", "429,429,true", "500,500,false", "503,503,true",
      "502,422,false", "502,429,true"})
  void shouldPreserveUnknownErrorMetadataAndRetryOnlyWhenRequested(
      int transportStatus, int status, boolean retryable) {
    NervOrigin origin = NervOrigin.builder().service("remote-service").build();
    NervErrorResponse remote = NervErrorResponse.builder()
        .code("REMOTE_FAILURE")
        .message("Downstream explanation")
        .status(status)
        .retryable(retryable)
        .category("REMOTE_CATEGORY")
        .origin(origin)
        .details(Map.of("reason", "remote detail"))
        .build();

    // Transport status must not override the error code's status or retry decision.
    Exception exception = decoder.decode("TestClient#get",
        response(transportStatus, objectMapper.writeValueAsString(remote)));

    if (retryable) {
      assertThat(exception).isInstanceOf(RetryableException.class);
      assertThat(((RetryableException) exception).status()).isEqualTo(transportStatus);
      exception = (Exception) exception.getCause();
    }
    assertThat(exception).isInstanceOf(NervDownstreamException.class);
    NervDownstreamException downstream = (NervDownstreamException) exception;
    assertThat(downstream.getErrorCode()).isEqualTo(RemoteNervErrorCode.builder()
        .code(remote.code()).message(remote.message()).status(status)
        .retryable(retryable).category(remote.category()).build());
    assertThat(downstream.getMessage()).isEqualTo(remote.message());
    assertThat(downstream.getOrigin()).isEqualTo(origin);
    assertThat(downstream.getDetails()).isEqualTo(remote.details());
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void shouldKeepRegistryMetadataAuthoritative(boolean registeredRetryable) {
    NativeNervErrorCodes registered = registeredRetryable
        ? NativeNervErrorCodes.DOWNSTREAM_SERVICE_ERROR : NativeNervErrorCodes.CONFLICT;
    NervErrorResponse remote = NervErrorResponse.builder()
        .code(registered.code()).message("Remote message").status(503)
        .retryable(!registeredRetryable).category("REMOTE").build();

    Exception exception = decoder.decode("TestClient#get",
        response(503, objectMapper.writeValueAsString(remote)));

    if (registeredRetryable) {
      assertThat(exception).isInstanceOf(RetryableException.class);
      exception = (Exception) exception.getCause();
    }
    assertThat(exception).isInstanceOf(NervDownstreamException.class);
    assertThat(((NervException) exception).getErrorCode()).isSameAs(registered);
    assertThat(exception).hasMessage("Remote message");
  }

  @Test
  void shouldAcceptMissingOptionalFieldsWithoutAssumingRetryable() {
    Exception exception = decoder.decode("TestClient#get", response(503,
        "{\"code\":\"REMOTE_MINIMAL\",\"message\":\"Unavailable\",\"status\":503}"));

    assertThat(exception).isInstanceOf(NervDownstreamException.class);
    NervException downstream = (NervException) exception;
    assertThat(downstream.getErrorCode().code()).isEqualTo("REMOTE_MINIMAL");
    assertThat(downstream.getErrorCode().retryable()).isFalse();
    assertThat(downstream.getErrorCode().category()).isNull();
    assertThat(downstream.getOrigin()).isNull();
    assertThat(downstream.getDetails()).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(strings = {"not-json", "null", "{}", "[]", "{\"message\":\"failure\"}",
      "{\"code\":\"\",\"message\":\"failure\",\"status\":500}",
      "{\"code\":\"REMOTE\",\"status\":500}",
      "{\"code\":\"REMOTE\",\"message\":\"failure\"}",
      "{\"code\":\"REMOTE\",\"message\":\"failure\",\"status\":200}",
      "{\"code\":\"REMOTE\",\"message\":\"failure\",\"status\":600}"})
  void shouldPreserveNonNervStreamBodyForDefaultFallback(String body) {
    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
    Response response = response(500, null).toBuilder()
        .body(new ByteArrayInputStream(bytes), bytes.length).build();
    NervFeignErrorDecoder defaultDecoder = new NervFeignErrorDecoder(
        objectMapper, new NativeNervErrorCodeRegistry());

    Exception exception = defaultDecoder.decode("TestClient#get", response);

    assertThat(exception).isInstanceOf(FeignException.class);
    assertThat(((FeignException) exception).contentUTF8()).isEqualTo(body);
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
