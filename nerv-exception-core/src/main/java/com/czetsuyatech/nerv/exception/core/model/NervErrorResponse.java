package com.czetsuyatech.nerv.exception.core.model;

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
    Map<String, Object> details
) {

}
