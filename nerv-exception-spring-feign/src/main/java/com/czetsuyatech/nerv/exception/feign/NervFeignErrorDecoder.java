package com.czetsuyatech.nerv.exception.feign;

import com.czetsuyatech.nerv.exception.api.NervErrorCode;
import com.czetsuyatech.nerv.exception.core.NervDownstreamException;
import com.czetsuyatech.nerv.exception.core.code.RemoteNervErrorCode;
import com.czetsuyatech.nerv.exception.core.model.NervErrorResponse;
import com.czetsuyatech.nerv.exception.core.registry.NervErrorCodeRegistry;
import feign.Response;
import feign.RetryableException;
import feign.codec.ErrorDecoder;
import java.io.IOException;
import java.io.InputStream;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.DeserializationFeature;

public class NervFeignErrorDecoder implements ErrorDecoder {

  private final ObjectMapper objectMapper;
  private final NervErrorCodeRegistry errorCodeRegistry;
  private final ErrorDecoder fallbackDecoder;

  public NervFeignErrorDecoder(
      ObjectMapper objectMapper,
      NervErrorCodeRegistry errorCodeRegistry) {

    this(objectMapper, errorCodeRegistry, new Default());
  }

  public NervFeignErrorDecoder(
      ObjectMapper objectMapper,
      NervErrorCodeRegistry errorCodeRegistry,
      ErrorDecoder fallbackDecoder) {

    this.objectMapper = objectMapper;
    this.errorCodeRegistry = errorCodeRegistry;
    this.fallbackDecoder = fallbackDecoder;
  }

  @Override
  public Exception decode(String methodKey, Response response) {

    // Buffer once so a consumed network stream remains available to the fallback decoder.
    if (response.body() != null) {
      try (InputStream inputStream = response.body().asInputStream()) {
        response = response.toBuilder().body(inputStream.readAllBytes()).build();
      } catch (IOException | RuntimeException ex) {
        return fallbackDecoder.decode(methodKey, response);
      }
    }

    NervErrorResponse errorResponse = readErrorResponse(response);

    if (errorResponse == null) {
      return fallbackDecoder.decode(methodKey, response);
    }

    NervErrorCode errorCode = errorCodeRegistry
        .findByCode(errorResponse.code())
        .orElseGet(() -> RemoteNervErrorCode.builder()
            .code(errorResponse.code())
            .message(errorResponse.message())
            .status(errorResponse.status())
            .retryable(errorResponse.retryable())
            .category(errorResponse.category())
            .build());

    NervDownstreamException downstreamException = NervDownstreamException.builder()
        .errorCode(errorCode)
        .response(errorResponse)
        .build();

    if (errorCode.retryable()) {
      return new RetryableException(
          response.status(),
          errorResponse.message(),
          response.request().httpMethod(),
          downstreamException,
          (Long) null,
          response.request()
      );
    }

    return downstreamException;
  }

  private NervErrorResponse readErrorResponse(Response response) {

    if (response.body() == null) {
      return null;
    }

    try (InputStream inputStream = response.body().asInputStream()) {
      NervErrorResponse errorResponse = objectMapper.readerFor(NervErrorResponse.class)
          .without(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
          .readValue(inputStream);
      // Optional metadata may be absent; a code, message and error status identify the contract.
      if (errorResponse == null || errorResponse.code() == null
          || errorResponse.code().isBlank() || errorResponse.message() == null
          || errorResponse.status() < 400 || errorResponse.status() > 599) {
        return null;
      }
      return errorResponse;

    } catch (IOException | RuntimeException ex) {
      return null;
    }
  }
}
