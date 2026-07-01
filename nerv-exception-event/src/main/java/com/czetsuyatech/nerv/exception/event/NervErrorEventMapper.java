package com.czetsuyatech.nerv.exception.event;

import com.czetsuyatech.nerv.exception.core.NervException;

public interface NervErrorEventMapper {

  NervErrorEvent from(
      NervException exception,
      String source,
      String topic,
      String key);
}
