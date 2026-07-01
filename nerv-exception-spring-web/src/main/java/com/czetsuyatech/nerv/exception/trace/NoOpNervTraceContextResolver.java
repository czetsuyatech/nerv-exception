package com.czetsuyatech.nerv.exception.trace;

public  class NoOpNervTraceContextResolver implements NervTraceContextResolver {

  @Override
  public NervTraceContext current() {
    return NervTraceContext.empty();
  }
}
