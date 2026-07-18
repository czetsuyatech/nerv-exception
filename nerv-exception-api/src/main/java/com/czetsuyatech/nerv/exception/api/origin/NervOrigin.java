package com.czetsuyatech.nerv.exception.api.origin;

import lombok.Builder;

@Builder
public record NervOrigin(
    String service,
    String instance,
    String version,
    String environment
) {}
