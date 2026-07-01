package com.czetsuyatech.nerv.exception.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import com.czetsuyatech.nerv.exception.web.NervErrorResponseMapper;
import com.czetsuyatech.nerv.exception.web.NervExceptionHandler;
import com.czetsuyatech.nerv.exception.web.NervExceptionSettings;
import com.czetsuyatech.nerv.exception.trace.NervTraceContextResolver;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class NervExceptionAutoConfigurationTest {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
      .withConfiguration(
          org.springframework.boot.autoconfigure.AutoConfigurations.of(
              NervExceptionPropertiesAutoConfiguration.class,
              NervExceptionAutoConfiguration.class));

  @Test
  void shouldCreateBeansWhenEnabledByDefault() {

    contextRunner.run(context -> {
      assertThat(context).hasSingleBean(NervExceptionProperties.class);
      assertThat(context).hasSingleBean(NervExceptionSettings.class);
      assertThat(context).hasSingleBean(NervErrorResponseMapper.class);
      assertThat(context).hasSingleBean(NervExceptionHandler.class);
      assertThat(context).hasSingleBean(NervTraceContextResolver.class);
    });
  }

  @Test
  void shouldNotCreateWebBeansWhenDisabled() {

    contextRunner
        .withPropertyValues("nerv.exception.enabled=false")
        .run(context -> {
          assertThat(context).hasSingleBean(NervExceptionProperties.class);
          assertThat(context).doesNotHaveBean(NervExceptionSettings.class);
          assertThat(context).doesNotHaveBean(NervErrorResponseMapper.class);
          assertThat(context).doesNotHaveBean(NervExceptionHandler.class);
        });
  }

  @Test
  void shouldBindPropertiesToSettings() {

    contextRunner
        .withPropertyValues(
            "nerv.exception.include-details=false",
            "nerv.exception.expose-internal-message=true",
            "nerv.exception.include-cause=true",
            "nerv.exception.include-stack-trace=true")
        .run(context -> {
          NervExceptionSettings settings = context.getBean(NervExceptionSettings.class);

          assertThat(settings.includeDetails()).isFalse();
          assertThat(settings.exposeInternalMessage()).isTrue();
          assertThat(settings.includeCause()).isTrue();
          assertThat(settings.includeStackTrace()).isTrue();
        });
  }
}
