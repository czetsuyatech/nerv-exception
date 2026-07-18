package com.czetsuyatech.nerv.exception.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.czetsuyatech.nerv.exception.api.NervErrorCode;
import com.czetsuyatech.nerv.exception.core.NervException;
import com.czetsuyatech.nerv.exception.core.code.NativeNervErrorCodes;
import com.czetsuyatech.nerv.exception.core.model.NervErrorResponse;
import com.czetsuyatech.nerv.exception.core.origin.NoOpNervOriginResolver;
import com.czetsuyatech.nerv.exception.trace.NervTraceContext;
import com.czetsuyatech.nerv.exception.trace.NervTraceContextResolver;
import com.czetsuyatech.nerv.exception.trace.NoOpNervTraceContextResolver;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;

class NervErrorResponseMapperTest {

  private static final MockHttpServletRequest REQUEST =
      new MockHttpServletRequest("GET", "/api/test");

  private static final NervTraceContextResolver NO_OP_TRACE = new NoOpNervTraceContextResolver();

  @Test
  void shouldMapNervExceptionUsingItsErrorCode() {

    NervExceptionSettings settings = new NervExceptionSettings(
        true,
        false,
        false,
        false);

    NervErrorResponseMapper mapper = new NervErrorResponseMapper(settings, NO_OP_TRACE, new NoOpNervOriginResolver());

    NervException exception = Mockito.mock(NervException.class);

    when(exception.getErrorCode()).thenReturn(TestErrorCode.TEST_ERROR);
    when(exception.getMessage()).thenReturn("Something failed");
    when(exception.getDetails()).thenReturn(Map.of("field", "value"));

    NervErrorResponse response = mapper.from(exception, REQUEST);

    assertThat(response.code()).isEqualTo("TEST_ERROR");
    assertThat(response.message()).isEqualTo("Something failed");
    assertThat(response.status()).isEqualTo(422);
    assertThat(response.retryable()).isFalse();
    assertThat(response.category()).isEqualTo("TEST");
    assertThat(response.path()).isEqualTo("/api/test");
    assertThat(response.details()).containsEntry("field", "value");
  }

  @Test
  void shouldHideInternalMessageByDefault() {

    NervExceptionSettings settings = new NervExceptionSettings(
        true,
        false,
        false,
        false);

    NervErrorResponseMapper mapper = new NervErrorResponseMapper(settings, NO_OP_TRACE, new NoOpNervOriginResolver());

    Exception exception = new IllegalStateException("Database password leaked");

    NervErrorResponse response = mapper.from(exception, REQUEST);

    assertThat(response.code()).isEqualTo(NativeNervErrorCodes.INTERNAL_SERVER_ERROR.code());
    assertThat(response.message()).isEqualTo(NativeNervErrorCodes.INTERNAL_SERVER_ERROR.message());
    assertThat(response.status()).isEqualTo(500);
    assertThat(response.details()).isEmpty();
  }

  @Test
  void shouldExposeInternalMessageWhenEnabled() {

    NervExceptionSettings settings = new NervExceptionSettings(
        true,
        true,
        false,
        false);

    NervErrorResponseMapper mapper = new NervErrorResponseMapper(settings, NO_OP_TRACE, new NoOpNervOriginResolver());

    Exception exception = new IllegalStateException("Actual internal error");

    NervErrorResponse response = mapper.from(exception, REQUEST);

    assertThat(response.code()).isEqualTo(NativeNervErrorCodes.INTERNAL_SERVER_ERROR.code());
    assertThat(response.message()).isEqualTo("Actual internal error");
  }

  @Test
  void shouldIncludeCauseWhenEnabled() {

    NervExceptionSettings settings = new NervExceptionSettings(
        true,
        false,
        true,
        false);

    NervErrorResponseMapper mapper = new NervErrorResponseMapper(settings, NO_OP_TRACE, new NoOpNervOriginResolver());

    Exception exception = new IllegalStateException(
        "Wrapper",
        new IllegalArgumentException("Cause"));

    NervErrorResponse response = mapper.from(exception, REQUEST);

    assertThat(response.details())
        .containsEntry("cause", IllegalArgumentException.class.getName());
  }

  @Test
  void shouldIncludeStackTraceWhenEnabled() {

    NervExceptionSettings settings = new NervExceptionSettings(
        true,
        false,
        false,
        true);

    NervErrorResponseMapper mapper = new NervErrorResponseMapper(settings, NO_OP_TRACE, new NoOpNervOriginResolver());

    Exception exception = new IllegalStateException("Stack trace error");

    NervErrorResponse response = mapper.from(exception, REQUEST);

    assertThat(response.details())
        .containsKey("stackTrace");

    assertThat(response.details().get("stackTrace").toString())
        .contains("Stack trace error");
  }

  @Test
  void shouldMapBindExceptionToValidationError() {

    NervExceptionSettings settings = new NervExceptionSettings(
        true,
        false,
        false,
        false);

    NervErrorResponseMapper mapper = new NervErrorResponseMapper(settings, NO_OP_TRACE, new NoOpNervOriginResolver());

    BindException exception = new BindException(new TestRequest(), "request");
    exception.addError(new FieldError(
        "request",
        "name",
        "Name is required"));

    NervErrorResponse response = mapper.from(exception, REQUEST);

    assertThat(response.code()).isEqualTo(NativeNervErrorCodes.VALIDATION_ERROR.code());
    assertThat(response.message()).isEqualTo(NativeNervErrorCodes.VALIDATION_ERROR.message());
    assertThat(response.status()).isEqualTo(400);
    assertThat(response.details()).containsKey("errors");

    @SuppressWarnings("unchecked")
    Map<String, String> errors = (Map<String, String>) response.details().get("errors");

    assertThat(errors).containsEntry("name", "Name is required");
  }

  @Test
  void shouldNotIncludeValidationErrorsWhenDetailsDisabled() {

    NervExceptionSettings settings = new NervExceptionSettings(
        false,
        false,
        false,
        false);

    NervErrorResponseMapper mapper = new NervErrorResponseMapper(settings, NO_OP_TRACE, new NoOpNervOriginResolver());

    BindException exception = new BindException(new TestRequest(), "request");
    exception.addError(new FieldError(
        "request",
        "name",
        "Name is required"));

    NervErrorResponse response = mapper.from(exception, REQUEST);

    assertThat(response.code()).isEqualTo(NativeNervErrorCodes.VALIDATION_ERROR.code());
    assertThat(response.details()).isEmpty();
  }

  @Test
  void shouldPopulateTraceAndSpanIds() {

    NervExceptionSettings settings = new NervExceptionSettings(
        true,
        false,
        false,
        false);

    NervTraceContextResolver resolver =
        new NervTraceContextResolver() {
          @Override
          public NervTraceContext current() {
            return NervTraceContext.empty();
          }
        };

    NervErrorResponseMapper mapper =
        new NervErrorResponseMapper(settings, resolver, new NoOpNervOriginResolver());
  }

  private record TestRequest() {

  }

  private enum TestErrorCode implements NervErrorCode {

    TEST_ERROR;

    @Override
    public String code() {
      return "TEST_ERROR";
    }

    @Override
    public String message() {
      return "Test error";
    }

    @Override
    public int status() {
      return 422;
    }

    @Override
    public boolean retryable() {
      return false;
    }

    @Override
    public String category() {
      return "TEST";
    }
  }
}
