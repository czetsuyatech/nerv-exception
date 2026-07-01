package com.czetsuyatech.nerv.exception.feign;

import com.czetsuyatech.nerv.exception.api.NervErrorCode;
import com.czetsuyatech.nerv.exception.core.NervDownstreamException;
import com.czetsuyatech.nerv.exception.core.code.NativeNervErrorCodes;
import com.czetsuyatech.nerv.exception.core.model.NervErrorResponse;
import com.czetsuyatech.nerv.exception.core.registry.NervErrorCodeRegistry;
import feign.Response;
import feign.RetryableException;
import feign.codec.ErrorDecoder;
import java.io.IOException;
import java.io.InputStream;
import tools.jackson.databind.ObjectMapper;

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

    NervErrorResponse errorResponse = readErrorResponse(response);

    if (errorResponse == null) {
      return fallbackDecoder.decode(methodKey, response);
    }

    NervErrorCode errorCode = errorCodeRegistry
        .findByCode(errorResponse.code())
        .orElse(NativeNervErrorCodes.BAD_REQUEST);

    if (errorCode.retryable()) {
      return new RetryableException(
          response.status(),
          errorResponse.message(),
          response.request().httpMethod(),
          null,
          (Long) null,
          response.request()
      );
    }

    return new NervDownstreamException(errorResponse);
  }

  private NervErrorResponse readErrorResponse(Response response) {

    if (response.body() == null) {
      return null;
    }

    try (InputStream inputStream = response.body().asInputStream()) {
      return objectMapper.readValue(inputStream, NervErrorResponse.class);

    } catch (IOException | RuntimeException ex) {
      return null;
    }
  }
}
