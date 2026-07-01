package com.czetsuyatech.nerv.exception.core.registry;

import com.czetsuyatech.nerv.exception.api.NervErrorCode;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class EnumNervErrorCodeRegistry implements NervErrorCodeRegistry {

  private final Map<String, NervErrorCode> errorCodes;

  public EnumNervErrorCodeRegistry(NervErrorCode[]... errorCodeGroups) {

    this.errorCodes = Arrays.stream(errorCodeGroups)
        .filter(Objects::nonNull)
        .flatMap(Arrays::stream)
        .collect(Collectors.toUnmodifiableMap(
            NervErrorCode::code,
            Function.identity(),
            (left, right) -> {
              throw new IllegalStateException(
                  "Duplicate error code: " + left.code());
            }
        ));
  }

  @Override
  public Optional<NervErrorCode> findByCode(String code) {

    if (code == null || code.isBlank()) {
      return Optional.empty();
    }

    return Optional.ofNullable(errorCodes.get(code));
  }
}
