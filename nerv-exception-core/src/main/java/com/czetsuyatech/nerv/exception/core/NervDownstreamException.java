package com.czetsuyatech.nerv.exception.core;

import com.czetsuyatech.nerv.exception.core.code.NativeNervErrorCodes;
import com.czetsuyatech.nerv.exception.core.model.NervErrorResponse;
import java.util.Map;

public class NervDownstreamException extends NervException {

  public NervDownstreamException(NervErrorResponse response) {
    super(
        NativeNervErrorCodes.DOWNSTREAM_SERVICE_ERROR,
        response.message(),
        Map.of(
            "downstreamCode", response.code(),
            "downstreamStatus", response.status(),
            "downstreamCategory", response.category(),
            "downstreamRetryable", response.retryable(),
            "downstreamTraceId", response.traceId(),
            "downstreamSpanId", response.spanId(),
            "downstreamPath", response.path()));
  }
}
