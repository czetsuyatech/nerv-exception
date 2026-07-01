package com.czetsuyatech.nerv.exception.event;

public final class NervEventErrorHeaders {

  public static final String ERROR_CODE = "nerv-error-code";
  public static final String ERROR_CATEGORY = "nerv-error-category";
  public static final String ERROR_RETRYABLE = "nerv-error-retryable";
  public static final String TRACE_ID = "nerv-trace-id";
  public static final String SPAN_ID = "nerv-span-id";
  public static final String PARENT_EVENT_ID = "nerv-parent-event-id";
  public static final String SOURCE = "nerv-source";

  private NervEventErrorHeaders() {
  }
}
