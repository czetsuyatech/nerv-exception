package com.czetsuyatech.nerv.exception.trace;

public interface NervTraceContextResolver {

  NervTraceContext current();
}
