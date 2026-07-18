package com.czetsuyatech.nerv.exception.core.model;

import com.czetsuyatech.nerv.exception.api.origin.NervOrigin;
import java.time.Instant;
import java.util.Map;
import lombok.Builder;

@Builder
public record NervErrorResponse(

    String code,
    String message,
    int status,
    boolean retryable,
    String category,
    String traceId,
    String spanId,
    String path,
    Instant timestamp,
    NervOrigin origin,
    Map<String, Object> details
) {

}
