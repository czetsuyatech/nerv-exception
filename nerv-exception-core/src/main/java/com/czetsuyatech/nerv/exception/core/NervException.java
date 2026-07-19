package com.czetsuyatech.nerv.exception.core;

import com.czetsuyatech.nerv.exception.api.NervErrorCode;
import com.czetsuyatech.nerv.exception.api.origin.NervOrigin;
import java.util.Collections;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;

@Getter
public class NervException extends RuntimeException {

  private final NervOrigin origin;
  private final NervErrorCode errorCode;
  private final Map<String, Object> details;

  @Builder(builderMethodName = "nervExceptionBuilder")
  NervException(
      NervOrigin origin,
      NervErrorCode errorCode,
      String message,
      Throwable cause,
      Map<String, Object> details) {
    super(message != null ? message : errorCode.message(), cause);
    this.origin = origin;
    this.errorCode = errorCode;
    this.details = details == null ? Collections.emptyMap() : Map.copyOf(details);
  }

  public static NervException of(NervErrorCode errorCode) {
    return NervException.nervExceptionBuilder()
        .errorCode(errorCode)
        .build();
  }

  public static NervException of(NervErrorCode errorCode, String message) {
    return NervException.nervExceptionBuilder()
        .errorCode(errorCode)
        .message(message)
        .build();
  }

  public static NervException of(NervErrorCode errorCode, Throwable cause) {
    return NervException.nervExceptionBuilder()
        .errorCode(errorCode)
        .cause(cause)
        .build();
  }

  public static NervException of(
      NervErrorCode errorCode,
      String message,
      Map<String, Object> details) {
    return NervException.nervExceptionBuilder()
        .errorCode(errorCode)
        .message(message)
        .details(details)
        .build();
  }

  public static NervException of(
      NervOrigin origin,
      NervErrorCode errorCode) {
    return NervException.nervExceptionBuilder()
        .origin(origin)
        .errorCode(errorCode)
        .build();
  }
}
