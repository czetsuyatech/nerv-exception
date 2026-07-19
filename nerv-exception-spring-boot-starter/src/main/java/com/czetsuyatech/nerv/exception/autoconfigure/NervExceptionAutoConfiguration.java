package com.czetsuyatech.nerv.exception.autoconfigure;

import com.czetsuyatech.nerv.exception.api.origin.NervOriginResolver;
import com.czetsuyatech.nerv.exception.core.registry.CompositeNervErrorCodeRegistry;
import com.czetsuyatech.nerv.exception.core.registry.NativeNervErrorCodeRegistry;
import com.czetsuyatech.nerv.exception.core.registry.NervErrorCodeRegistry;
import com.czetsuyatech.nerv.exception.event.DefaultNervErrorEventMapper;
import com.czetsuyatech.nerv.exception.event.NervErrorEvent;
import com.czetsuyatech.nerv.exception.event.NervErrorEventMapper;
import com.czetsuyatech.nerv.exception.event.NervEventTraceContextResolver;
import com.czetsuyatech.nerv.exception.event.NoOpNervEventTraceContextResolver;
import com.czetsuyatech.nerv.exception.feign.NervFeignErrorDecoder;
import com.czetsuyatech.nerv.exception.kafka.NervKafkaDlqPublisher;
import com.czetsuyatech.nerv.exception.kafka.NervKafkaErrorHandler;
import com.czetsuyatech.nerv.exception.kafka.NervKafkaHeaderMapper;
import com.czetsuyatech.nerv.exception.trace.MicrometerNervTraceContextResolver;
import com.czetsuyatech.nerv.exception.trace.NervTraceContextResolver;
import com.czetsuyatech.nerv.exception.trace.NoOpNervTraceContextResolver;
import com.czetsuyatech.nerv.exception.web.NervErrorResponseMapper;
import com.czetsuyatech.nerv.exception.web.NervExceptionHandler;
import com.czetsuyatech.nerv.exception.web.NervExceptionSettings;
import feign.codec.ErrorDecoder;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.kafka.core.KafkaTemplate;
import tools.jackson.databind.ObjectMapper;

@AutoConfiguration(after = {NervExceptionPropertiesAutoConfiguration.class, KafkaAutoConfiguration.class})
@ConditionalOnProperty(
    prefix = "nerv.exception",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class NervExceptionAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  NervOriginResolver nervOriginResolver(
      Environment environment,
      ObjectProvider<BuildProperties> buildProperties) {

    return new DefaultNervOriginResolver(environment, buildProperties.getIfAvailable());
  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnProperty(
      prefix = "nerv.exception.web",
      name = "enabled",
      havingValue = "true",
      matchIfMissing = true)
  static class WebConfiguration {

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
    @ConditionalOnClass(name = "io.micrometer.tracing.Tracer")
    @ConditionalOnBean(type = "io.micrometer.tracing.Tracer")
    @ConditionalOnMissingBean(NervTraceContextResolver.class)
    NervTraceContextResolver micrometerNervTraceContextResolver(
        io.micrometer.tracing.Tracer tracer
    ) {
      return new MicrometerNervTraceContextResolver(tracer);
    }

    @Bean
    @ConditionalOnMissingBean(NervTraceContextResolver.class)
    NervTraceContextResolver noOpNervTraceContextResolver() {
      return new NoOpNervTraceContextResolver();
    }

    @Bean
    @ConditionalOnMissingBean
    public NervErrorResponseMapper nervErrorResponseMapper(
        NervExceptionSettings settings,
        NervTraceContextResolver traceContextResolver,
        NervOriginResolver originResolver) {

      return new NervErrorResponseMapper(settings, traceContextResolver, originResolver);
    }

    @Bean
    @ConditionalOnMissingBean
    public NervExceptionHandler nervExceptionHandler(
        NervErrorResponseMapper errorResponseMapper) {

      return new NervExceptionHandler(errorResponseMapper);
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(ErrorDecoder.class)
    @ConditionalOnProperty(
        prefix = "nerv.exception.feign",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
    )
    static class FeignConfiguration {

      @Bean
      @ConditionalOnMissingBean
      NativeNervErrorCodeRegistry nativeNervErrorCodeRegistry() {
        return new NativeNervErrorCodeRegistry();
      }

      @Bean
      @Primary
      NervErrorCodeRegistry nervErrorCodeRegistry(
          ObjectProvider<NervErrorCodeRegistry> registries,
          NativeNervErrorCodeRegistry nativeRegistry
      ) {

        List<NervErrorCodeRegistry> delegates = registries.orderedStream()
            .filter(registry -> registry != nativeRegistry)
            .toList();

        List<NervErrorCodeRegistry> all = new ArrayList<>(delegates);
        all.add(nativeRegistry);

        return new CompositeNervErrorCodeRegistry(all);
      }

      @Bean
      @ConditionalOnMissingBean
      ObjectMapper nervFeignObjectMapper() {
        return new ObjectMapper();
      }

      @Bean
      @ConditionalOnMissingBean(ErrorDecoder.class)
      ErrorDecoder nervFeignErrorDecoder(
          ObjectMapper objectMapper,
          NervErrorCodeRegistry registry
      ) {

        return new NervFeignErrorDecoder(
            objectMapper,
            registry
        );
      }
    }
  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnProperty(
      prefix = "nerv.exception.event",
      name = "enabled",
      havingValue = "true",
      matchIfMissing = true)
  static class EventConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public NervEventTraceContextResolver nervEventTraceContextResolver() {
      return new NoOpNervEventTraceContextResolver();
    }

    @Bean
    @ConditionalOnMissingBean
    public NervErrorEventMapper nervErrorEventMapper(
        NervEventTraceContextResolver traceContextResolver) {

      return new DefaultNervErrorEventMapper(traceContextResolver);
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(KafkaTemplate.class)
    @ConditionalOnBean(KafkaTemplate.class)
    @ConditionalOnProperty(
        prefix = "nerv.exception.kafka",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
    static class KafkaConfiguration {

      @Bean
      @ConditionalOnMissingBean
      public NervKafkaHeaderMapper nervKafkaHeaderMapper() {
        return new NervKafkaHeaderMapper();
      }

      @Bean
      @ConditionalOnMissingBean
      public NervKafkaDlqPublisher nervKafkaDlqPublisher(
          KafkaTemplate<String, NervErrorEvent> kafkaTemplate,
          NervKafkaHeaderMapper headerMapper) {

        return new NervKafkaDlqPublisher(kafkaTemplate, headerMapper);
      }

      @Bean
      @ConditionalOnMissingBean
      public NervKafkaErrorHandler nervKafkaErrorHandler(
          NervErrorEventMapper errorEventMapper,
          NervKafkaDlqPublisher dlqPublisher,
          NervExceptionProperties properties) {

        return new NervKafkaErrorHandler(
            errorEventMapper,
            dlqPublisher,
            properties.getKafka().getSource(),
            properties.getKafka().getDlqTopicSuffix());
      }
    }
  }
}
