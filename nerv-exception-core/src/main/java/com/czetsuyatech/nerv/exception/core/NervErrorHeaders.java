package com.czetsuyatech.nerv.exception.core;

public final class NervErrorHeaders {

  public static final String ERROR_CODE = "X-Nerv-Error-Code";
  public static final String ERROR_CATEGORY = "X-Nerv-Error-Category";
  public static final String ERROR_RETRYABLE = "X-Nerv-Error-Retryable";
  public static final String TRACE_ID = "X-Nerv-Trace-Id";
  public static final String SPAN_ID = "X-Nerv-Span-Id";

  private NervErrorHeaders() {
  }
}
