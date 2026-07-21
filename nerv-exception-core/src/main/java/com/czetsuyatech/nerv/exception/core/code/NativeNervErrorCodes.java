package com.czetsuyatech.nerv.exception.core.code;

import com.czetsuyatech.nerv.exception.api.NervErrorCode;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum NativeNervErrorCodes implements NervErrorCode {

  BAD_REQUEST("BAD_REQUEST", "Bad request", 400, false, "CLIENT"),
  UNAUTHORIZED("UNAUTHORIZED", "Unauthorized", 401, false, "SECURITY"),
  FORBIDDEN("FORBIDDEN", "Forbidden", 403, false, "SECURITY"),
  RESOURCE_NOT_FOUND("RESOURCE_NOT_FOUND", "Resource not found", 404, false, "CLIENT"),
  METHOD_NOT_ALLOWED("METHOD_NOT_ALLOWED", "Method not allowed", 405, false, "CLIENT"),
  CONFLICT("CONFLICT", "Conflict", 409, false, "CLIENT"),
  VALIDATION_ERROR("VALIDATION_ERROR", "Validation error", 400, false, "VALIDATION"),
  CONSTRAINT_VIOLATION("CONSTRAINT_VIOLATION", "Constraint violation", 400, false, "VALIDATION"),
  TOO_MANY_REQUESTS("TOO_MANY_REQUESTS", "Too many requests", 429, true, "CLIENT"),
  INTERNAL_SERVER_ERROR("INTERNAL_SERVER_ERROR", "Internal server error", 500, false, "SYSTEM"),
  DOWNSTREAM_SERVICE_ERROR("DOWNSTREAM_SERVICE_ERROR", "Downstream service error", 502, true, "DOWNSTREAM"),
  SERVICE_UNAVAILABLE("SERVICE_UNAVAILABLE", "Service unavailable", 503, true, "SYSTEM"),
  GATEWAY_TIMEOUT("GATEWAY_TIMEOUT", "Gateway timeout", 504, true, "DOWNSTREAM"),
  NOT_ACCEPTABLE("NOT_ACCEPTABLE", "Not acceptable", 406, false, "CLIENT"),
  UNSUPPORTED_MEDIA_TYPE("UNSUPPORTED_MEDIA_TYPE", "Unsupported media type", 415, false, "CLIENT"),;

  private final String code;
  private final String message;
  private final int status;
  private final boolean retryable;
  private final String category;

  @Override
  public String code() {
    return code;
  }

  @Override
  public String message() {
    return message;
  }

  @Override
  public int status() {
    return status;
  }

  @Override
  public boolean retryable() {
    return retryable;
  }

  @Override
  public String category() {
    return category;
  }
}
