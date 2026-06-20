package com.czetsuyatech.nerv.exception.core;

import com.czetsuyatech.nerv.exception.api.NervErrorCode;
import java.util.Optional;

public interface NervErrorCodeRegistry {

  Optional<NervErrorCode> findByCode(String code);
}
