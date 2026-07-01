package com.czetsuyatech.nerv.exception.event;

public interface NervEventTraceContextResolver {

  String traceId();

  String spanId();

  String parentEventId();
}
