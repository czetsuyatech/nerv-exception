package com.czetsuyatech.nerv.exception.autoconfigure;

import com.czetsuyatech.nerv.exception.core.NativeNervErrorCodeRegistry;
import com.czetsuyatech.nerv.exception.core.NervErrorCodeRegistry;
import com.czetsuyatech.nerv.exception.feign.NervFeignErrorDecoder;
import com.czetsuyatech.nerv.exception.web.NervErrorResponseMapper;
import com.czetsuyatech.nerv.exception.web.NervExceptionHandler;
import com.czetsuyatech.nerv.exception.web.NervExceptionSettings;
import com.czetsuyatech.nerv.exception.web.NervTraceContextResolver;
import com.czetsuyatech.nerv.exception.web.NoOpNervTraceContextResolver;
import feign.codec.ErrorDecoder;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import tools.jackson.databind.ObjectMapper;

@AutoConfiguration(after = NervExceptionPropertiesAutoConfiguration.class)
@ConditionalOnProperty(
    prefix = "nerv.exception",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class NervExceptionAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  public NervTraceContextResolver nervTraceContextResolver() {
    return new NoOpNervTraceContextResolver();
  }

  @Bean
  @ConditionalOnMissingBean
  public NervExceptionSettings nervExceptionSettings(
      NervExceptionProperties properties) {

    return new NervExceptionSettings(
        properties.isIncludeDetails(),
        properties.isExposeInternalMessage(),
        properties.isIncludeCause(),
        properties.isIncludeStackTrace()
    );
  }

  @Bean
  @ConditionalOnMissingBean
  public NervErrorResponseMapper nervErrorResponseMapper(
      NervExceptionSettings settings,
      NervTraceContextResolver traceContextResolver) {

    return new NervErrorResponseMapper(settings, traceContextResolver);
  }

  @Bean
  @ConditionalOnMissingBean
  public NervExceptionHandler nervExceptionHandler(
      NervErrorResponseMapper errorResponseMapper) {

    return new NervExceptionHandler(errorResponseMapper);
  }

  @Bean
  @ConditionalOnMissingBean
  public NervErrorCodeRegistry nervErrorCodeRegistry() {
    return new NativeNervErrorCodeRegistry();
  }

  @Bean
  @ConditionalOnClass(ErrorDecoder.class)
  @ConditionalOnBean(ObjectMapper.class)
  @ConditionalOnMissingBean(ErrorDecoder.class)
  public ErrorDecoder nervFeignErrorDecoder(
      ObjectMapper objectMapper,
      NervErrorCodeRegistry errorCodeRegistry) {

    return new NervFeignErrorDecoder(
        objectMapper,
        errorCodeRegistry);
  }
}
