package com.czetsuyatech.nerv.exception.core.registry;

import com.czetsuyatech.nerv.exception.api.NervErrorCode;
import com.czetsuyatech.nerv.exception.core.code.NativeNervErrorCodes;
import java.util.Arrays;
import java.util.Optional;

public class NativeNervErrorCodeRegistry implements NervErrorCodeRegistry {

  @Override
  public Optional<NervErrorCode> findByCode(String code) {

    if (code == null || code.isBlank()) {
      return Optional.empty();
    }

    return Arrays.stream(NativeNervErrorCodes.values())
        .filter(errorCode -> errorCode.code().equals(code))
        .map(NervErrorCode.class::cast)
        .findFirst();
  }
}
