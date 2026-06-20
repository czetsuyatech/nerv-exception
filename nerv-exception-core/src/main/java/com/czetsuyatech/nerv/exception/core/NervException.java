package com.czetsuyatech.nerv.exception.core;

import com.czetsuyatech.nerv.exception.api.NervErrorCode;
import java.util.Collections;
import java.util.Map;
import lombok.Getter;

@Getter
public class NervException extends RuntimeException {

  private final NervErrorCode errorCode;
  private final Map<String, Object> details;

  public NervException(
      NervErrorCode errorCode,
      String message,
      Map<String, Object> details) {
    super(message);
    this.errorCode = errorCode;
    this.details = details == null ? Collections.emptyMap() : Map.copyOf(details);
  }

  public NervException(NervErrorCode errorCode) {
    this(errorCode, errorCode.message(), null, Collections.emptyMap());
  }

  public NervException(NervErrorCode errorCode, String message) {
    this(errorCode, message, null, Collections.emptyMap());
  }

  public NervException(NervErrorCode errorCode, Throwable cause) {
    this(errorCode, errorCode.message(), cause, Collections.emptyMap());
  }

  public NervException(
      NervErrorCode errorCode,
      String message,
      Throwable cause,
      Map<String, Object> details
  ) {
    super(message, cause);
    this.errorCode = errorCode;
    this.details = details == null ? Collections.emptyMap() : Map.copyOf(details);
  }
}
