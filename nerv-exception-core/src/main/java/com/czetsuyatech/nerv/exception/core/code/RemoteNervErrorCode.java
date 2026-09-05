package com.czetsuyatech.nerv.exception.core.code;

import com.czetsuyatech.nerv.exception.api.NervErrorCode;
import lombok.Builder;

/**
 * Error metadata supplied by a downstream service, without a local registry entry.
 */
@Builder
public record RemoteNervErrorCode(
    String code,
    String message,
    int status,
    boolean retryable,
    String category
) implements NervErrorCode {

}
