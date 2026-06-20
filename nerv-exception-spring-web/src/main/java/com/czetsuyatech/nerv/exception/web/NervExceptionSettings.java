package com.czetsuyatech.nerv.exception.web;

public record NervExceptionSettings(
    boolean includeDetails,
    boolean exposeInternalMessage,
    boolean includeCause,
    boolean includeStackTrace
) {

  public static NervExceptionSettings defaults() {
    return new NervExceptionSettings(
        true,
        false,
        false,
        false
    );
  }
}
