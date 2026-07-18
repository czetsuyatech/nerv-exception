package com.czetsuyatech.nerv.exception.core.origin;

import com.czetsuyatech.nerv.exception.api.origin.NervOrigin;
import com.czetsuyatech.nerv.exception.api.origin.NervOriginResolver;

public class NoOpNervOriginResolver implements NervOriginResolver {

  private static final NervOrigin ORIGIN = NervOrigin.builder()
      .service("unknown-service")
      .instance("unknown-instance")
      .version("unknown-version")
      .environment("default")
      .build();

  @Override
  public NervOrigin resolve() {
    return ORIGIN;
  }

}
