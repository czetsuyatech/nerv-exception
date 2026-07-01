package com.czetsuyatech.nerv.exception.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.czetsuyatech.nerv.exception.event.NervErrorEventMapper;
import com.czetsuyatech.nerv.exception.event.NervEventTraceContextResolver;
import com.czetsuyatech.nerv.exception.kafka.NervKafkaDlqPublisher;
import com.czetsuyatech.nerv.exception.kafka.NervKafkaErrorHandler;
import com.czetsuyatech.nerv.exception.kafka.NervKafkaHeaderMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.kafka.core.KafkaTemplate;

class NervExceptionKafkaAutoConfigurationTest {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
      .withConfiguration(AutoConfigurations.of(
              NervExceptionPropertiesAutoConfiguration.class,
              NervExceptionAutoConfiguration.class
          )
      );

  @Test
  void shouldCreateEventBeans() {

    contextRunner.run(context -> {
      assertThat(context).hasSingleBean(NervEventTraceContextResolver.class);
      assertThat(context).hasSingleBean(NervErrorEventMapper.class);
    });
  }

  @Test
  void shouldCreateKafkaBeansWhenKafkaTemplateExists() {

    contextRunner
        .withBean(KafkaTemplate.class, () -> mock(KafkaTemplate.class))
        .run(context -> {
          assertThat(context).hasSingleBean(NervKafkaHeaderMapper.class);
          assertThat(context).hasSingleBean(NervKafkaDlqPublisher.class);
          assertThat(context).hasSingleBean(NervKafkaErrorHandler.class);
        });
  }

  @Test
  void shouldNotCreateKafkaBeansWhenKafkaIsDisabled() {

    contextRunner
        .withPropertyValues("nerv.exception.kafka.enabled=false")
        .withBean(KafkaTemplate.class, () -> mock(KafkaTemplate.class))
        .run(context -> {
          assertThat(context).doesNotHaveBean(NervKafkaHeaderMapper.class);
          assertThat(context).doesNotHaveBean(NervKafkaDlqPublisher.class);
          assertThat(context).doesNotHaveBean(NervKafkaErrorHandler.class);
        });
  }
}
