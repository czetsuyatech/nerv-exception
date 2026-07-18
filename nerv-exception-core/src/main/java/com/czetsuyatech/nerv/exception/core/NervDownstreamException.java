package com.czetsuyatech.nerv.exception.core;

import com.czetsuyatech.nerv.exception.api.NervErrorCode;
import com.czetsuyatech.nerv.exception.core.model.NervErrorResponse;
import lombok.Builder;

public class NervDownstreamException extends NervException {

  @Builder
  public NervDownstreamException(
      NervErrorCode errorCode,
      NervErrorResponse response,
      Throwable cause) {

    super(
        response.origin(),
        errorCode,
        response.message(),
        cause,
        response.details());

  }
}
