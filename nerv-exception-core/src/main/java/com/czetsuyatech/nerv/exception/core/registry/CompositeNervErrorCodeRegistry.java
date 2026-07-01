package com.czetsuyatech.nerv.exception.core.registry;

import com.czetsuyatech.nerv.exception.api.NervErrorCode;
import java.util.List;
import java.util.Optional;

public class CompositeNervErrorCodeRegistry implements NervErrorCodeRegistry {

  private final List<NervErrorCodeRegistry> registries;

  public CompositeNervErrorCodeRegistry(
      List<NervErrorCodeRegistry> registries
  ) {
    this.registries = List.copyOf(registries);
  }

  @Override
  public Optional<NervErrorCode> findByCode(String code) {
    return registries.stream()
        .map(registry -> registry.findByCode(code))
        .flatMap(Optional::stream)
        .findFirst();
  }
}
