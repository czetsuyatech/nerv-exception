package com.czetsuyatech.nerv.exception.event;

public class NoOpNervEventTraceContextResolver implements NervEventTraceContextResolver {

  @Override
  public String traceId() {
    return null;
  }

  @Override
  public String spanId() {
    return null;
  }

  @Override
  public String parentEventId() {
    return null;
  }
}
