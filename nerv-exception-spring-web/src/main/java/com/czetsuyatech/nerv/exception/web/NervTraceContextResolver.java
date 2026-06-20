package com.czetsuyatech.nerv.exception.web;

public interface NervTraceContextResolver {

  String traceId();

  String spanId();
}
