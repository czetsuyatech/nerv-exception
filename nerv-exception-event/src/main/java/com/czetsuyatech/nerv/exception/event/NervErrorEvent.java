package com.czetsuyatech.nerv.exception.event;

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
    Map<String, Object> details) {

}
