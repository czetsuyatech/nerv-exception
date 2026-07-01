package com.czetsuyatech.nerv.exception.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import com.czetsuyatech.nerv.exception.core.registry.CompositeNervErrorCodeRegistry;
import com.czetsuyatech.nerv.exception.core.registry.NervErrorCodeRegistry;
import com.czetsuyatech.nerv.exception.feign.NervFeignErrorDecoder;
import feign.codec.ErrorDecoder;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

class NervExceptionFeignAutoConfigurationTest {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
      .withConfiguration(
          AutoConfigurations.of(
              NervExceptionPropertiesAutoConfiguration.class,
              NervExceptionAutoConfiguration.class
          )
      )
      .withBean(ObjectMapper.class, ObjectMapper::new);

  @Test
  void shouldCreateFeignErrorDecoderWhenFeignAndObjectMapperAreAvailable() {

    contextRunner.run(context -> {
      assertThat(context.getBean(NervErrorCodeRegistry.class))
          .isInstanceOf(CompositeNervErrorCodeRegistry.class);
      assertThat(context).hasSingleBean(ErrorDecoder.class);
      assertThat(context.getBean(ErrorDecoder.class))
          .isInstanceOf(NervFeignErrorDecoder.class);
    });
  }

  @Test
  void shouldBackOffWhenCustomErrorDecoderExists() {

    contextRunner
        .withUserConfiguration(CustomErrorDecoderConfiguration.class)
        .run(context -> {
          assertThat(context).hasSingleBean(ErrorDecoder.class);
          assertThat(context.getBean(ErrorDecoder.class))
              .isInstanceOf(CustomErrorDecoder.class);
        });
  }

  @Configuration
  static class CustomErrorDecoderConfiguration {

    @Bean
    ErrorDecoder customErrorDecoder() {
      return new CustomErrorDecoder();
    }
  }

  static class CustomErrorDecoder implements ErrorDecoder {

    @Override
    public Exception decode(String methodKey, feign.Response response) {
      return new IllegalStateException("custom");
    }
  }
}
