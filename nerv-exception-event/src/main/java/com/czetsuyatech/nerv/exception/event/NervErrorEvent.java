package com.czetsuyatech.nerv.exception.event;

import com.czetsuyatech.nerv.exception.api.origin.NervOrigin;
import java.time.Instant;
import java.util.Map;
import lombok.Builder;

@Builder
public record NervErrorEvent(

    String code,
    String message,
    boolean retryable,
    String category,
    String traceId,
    String spanId,
    String parentEventId,
    String source,
    String topic,
    String key,
    Instant timestamp,
    NervOrigin origin,
    Map<String, Object> details) {

}
