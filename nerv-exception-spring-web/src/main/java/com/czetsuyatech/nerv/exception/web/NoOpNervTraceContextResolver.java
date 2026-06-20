package com.czetsuyatech.nerv.exception.web;

public class NoOpNervTraceContextResolver implements NervTraceContextResolver {

  @Override
  public String traceId() {
    return null;
  }

  @Override
  public String spanId() {
    return null;
  }
}
