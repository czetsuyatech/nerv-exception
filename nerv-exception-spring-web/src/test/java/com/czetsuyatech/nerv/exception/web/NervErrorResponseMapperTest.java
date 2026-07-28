package com.czetsuyatech.nerv.exception.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.czetsuyatech.nerv.exception.api.NervErrorCode;
import com.czetsuyatech.nerv.exception.api.origin.NervOrigin;
import com.czetsuyatech.nerv.exception.api.origin.NervOriginResolver;
import com.czetsuyatech.nerv.exception.core.NervException;
import com.czetsuyatech.nerv.exception.core.code.NativeNervErrorCodes;
import com.czetsuyatech.nerv.exception.core.model.NervErrorResponse;
import com.czetsuyatech.nerv.exception.core.origin.NoOpNervOriginResolver;
import com.czetsuyatech.nerv.exception.trace.NervTraceContext;
import com.czetsuyatech.nerv.exception.trace.NervTraceContextResolver;
import com.czetsuyatech.nerv.exception.trace.NoOpNervTraceContextResolver;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.MethodValidationResult;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

class NervErrorResponseMapperTest {

  private static final MockHttpServletRequest REQUEST =
      new MockHttpServletRequest("GET", "/api/test");

  private static final NervTraceContextResolver NO_OP_TRACE = new NoOpNervTraceContextResolver();

  private static final NervExceptionSettings DETAILS_ON =
      new NervExceptionSettings(true, false, false, false);

  private static final NervExceptionSettings DETAILS_OFF =
      new NervExceptionSettings(false, false, false, false);

  private static NervErrorResponseMapper mapper(NervExceptionSettings settings) {
    return new NervErrorResponseMapper(settings, NO_OP_TRACE, new NoOpNervOriginResolver());
  }

  // ---------------------------------------------------------------------
  // NervException
  // ---------------------------------------------------------------------

  @Test
  void shouldMapNervExceptionUsingItsErrorCode() {

    NervException exception = Mockito.mock(NervException.class);

    when(exception.getErrorCode()).thenReturn(TestErrorCode.TEST_ERROR);
    when(exception.getMessage()).thenReturn("Something failed");
    when(exception.getDetails()).thenReturn(Map.of("field", "value"));

    NervErrorResponse response = mapper(DETAILS_ON).from(exception, REQUEST);

    assertThat(response.code()).isEqualTo("TEST_ERROR");
    assertThat(response.message()).isEqualTo("Something failed");
    assertThat(response.status()).isEqualTo(422);
    assertThat(response.retryable()).isFalse();
    assertThat(response.category()).isEqualTo("TEST");
    assertThat(response.path()).isEqualTo("/api/test");
    assertThat(response.details()).containsEntry("field", "value");
  }

  @Test
  void shouldFallBackToErrorCodeMessageWhenNervExceptionHasNoMessage() {

    NervException exception = NervException.of(NativeNervErrorCodes.CONFLICT);

    NervErrorResponse response = mapper(DETAILS_ON).from(exception, REQUEST);

    assertThat(response.code()).isEqualTo(NativeNervErrorCodes.CONFLICT.code());
    assertThat(response.message()).isEqualTo(NativeNervErrorCodes.CONFLICT.message());
    assertThat(response.status()).isEqualTo(409);
    assertThat(response.details()).isEmpty();
  }

  @Test
  void shouldNotIncludeNervExceptionDetailsWhenDetailsDisabled() {

    NervException exception = NervException.of(
        NativeNervErrorCodes.CONFLICT,
        Map.of("field", "value"));

    NervErrorResponse response = mapper(DETAILS_OFF).from(exception, REQUEST);

    assertThat(response.details()).isEmpty();
  }

  @Test
  void shouldPropagateRetryableErrorCodes() {

    NervException exception = NervException.of(NativeNervErrorCodes.GATEWAY_TIMEOUT);

    NervErrorResponse response = mapper(DETAILS_ON).from(exception, REQUEST);

    assertThat(response.status()).isEqualTo(504);
    assertThat(response.retryable()).isTrue();
    assertThat(response.category()).isEqualTo("DOWNSTREAM");
  }

  // ---------------------------------------------------------------------
  // Generic exception
  // ---------------------------------------------------------------------

  @Test
  void shouldHideInternalMessageByDefault() {

    Exception exception = new IllegalStateException("Database password leaked");

    NervErrorResponse response = mapper(DETAILS_ON).from(exception, REQUEST);

    assertThat(response.code()).isEqualTo(NativeNervErrorCodes.INTERNAL_SERVER_ERROR.code());
    assertThat(response.message()).isEqualTo(NativeNervErrorCodes.INTERNAL_SERVER_ERROR.message());
    assertThat(response.status()).isEqualTo(500);
    assertThat(response.details()).isEmpty();
  }

  @Test
  void shouldExposeInternalMessageWhenEnabled() {

    NervExceptionSettings settings = new NervExceptionSettings(true, true, false, false);

    Exception exception = new IllegalStateException("Actual internal error");

    NervErrorResponse response = mapper(settings).from(exception, REQUEST);

    assertThat(response.code()).isEqualTo(NativeNervErrorCodes.INTERNAL_SERVER_ERROR.code());
    assertThat(response.message()).isEqualTo("Actual internal error");
  }

  @Test
  void shouldIncludeCauseWhenEnabled() {

    NervExceptionSettings settings = new NervExceptionSettings(true, false, true, false);

    Exception exception = new IllegalStateException(
        "Wrapper",
        new IllegalArgumentException("Cause"));

    NervErrorResponse response = mapper(settings).from(exception, REQUEST);

    assertThat(response.details())
        .containsEntry("cause", IllegalArgumentException.class.getName());
  }

  @Test
  void shouldNotIncludeCauseWhenExceptionHasNone() {

    NervExceptionSettings settings = new NervExceptionSettings(true, false, true, false);

    NervErrorResponse response =
        mapper(settings).from(new IllegalStateException("No cause"), REQUEST);

    assertThat(response.details()).doesNotContainKey("cause");
  }

  @Test
  void shouldNotIncludeCauseWhenDisabled() {

    Exception exception = new IllegalStateException(
        "Wrapper",
        new IllegalArgumentException("Cause"));

    NervErrorResponse response = mapper(DETAILS_ON).from(exception, REQUEST);

    assertThat(response.details()).doesNotContainKey("cause");
  }

  @Test
  void shouldIncludeStackTraceWhenEnabled() {

    NervExceptionSettings settings = new NervExceptionSettings(true, false, false, true);

    Exception exception = new IllegalStateException("Stack trace error");

    NervErrorResponse response = mapper(settings).from(exception, REQUEST);

    assertThat(response.details()).containsKey("stackTrace");
    assertThat(response.details().get("stackTrace").toString())
        .contains("Stack trace error");
  }

  @Test
  void shouldNotIncludeStackTraceWhenDisabled() {

    NervErrorResponse response =
        mapper(DETAILS_ON).from(new IllegalStateException("boom"), REQUEST);

    assertThat(response.details()).doesNotContainKey("stackTrace");
  }

  // ---------------------------------------------------------------------
  // Validation
  // ---------------------------------------------------------------------

  @Test
  void shouldMapMethodArgumentNotValidExceptionToValidationError() {

    MethodArgumentNotValidException exception =
        methodArgumentNotValidException(new FieldError("request", "name", "Name is required"));

    NervErrorResponse response = mapper(DETAILS_ON).from(exception, REQUEST);

    assertThat(response.code()).isEqualTo(NativeNervErrorCodes.VALIDATION_ERROR.code());
    assertThat(response.message()).isEqualTo(NativeNervErrorCodes.VALIDATION_ERROR.message());
    assertThat(response.status()).isEqualTo(400);
    assertThat(response.category()).isEqualTo("VALIDATION");
    assertThat(errors(response)).containsEntry("name", "Name is required");
  }

  @Test
  void shouldNotIncludeMethodArgumentNotValidErrorsWhenDetailsDisabled() {

    MethodArgumentNotValidException exception =
        methodArgumentNotValidException(new FieldError("request", "name", "Name is required"));

    NervErrorResponse response = mapper(DETAILS_OFF).from(exception, REQUEST);

    assertThat(response.code()).isEqualTo(NativeNervErrorCodes.VALIDATION_ERROR.code());
    assertThat(response.details()).isEmpty();
  }

  @Test
  void shouldMapBindExceptionToValidationError() {

    BindException exception = new BindException(new TestRequest(), "request");
    exception.addError(new FieldError("request", "name", "Name is required"));

    NervErrorResponse response = mapper(DETAILS_ON).from(exception, REQUEST);

    assertThat(response.code()).isEqualTo(NativeNervErrorCodes.VALIDATION_ERROR.code());
    assertThat(response.message()).isEqualTo(NativeNervErrorCodes.VALIDATION_ERROR.message());
    assertThat(response.status()).isEqualTo(400);
    assertThat(errors(response)).containsEntry("name", "Name is required");
  }

  @Test
  void shouldNotIncludeValidationErrorsWhenDetailsDisabled() {

    BindException exception = new BindException(new TestRequest(), "request");
    exception.addError(new FieldError("request", "name", "Name is required"));

    NervErrorResponse response = mapper(DETAILS_OFF).from(exception, REQUEST);

    assertThat(response.code()).isEqualTo(NativeNervErrorCodes.VALIDATION_ERROR.code());
    assertThat(response.details()).isEmpty();
  }

  @Test
  void shouldFallBackToInvalidValueWhenFieldErrorHasNoDefaultMessage() {

    BindException exception = new BindException(new TestRequest(), "request");
    exception.addError(new FieldError("request", "name", null));

    NervErrorResponse response = mapper(DETAILS_ON).from(exception, REQUEST);

    assertThat(errors(response)).containsEntry("name", "Invalid value");
  }

  @Test
  void shouldKeepFirstErrorPerFieldWhenFieldHasMultipleErrors() {

    BindException exception = new BindException(new TestRequest(), "request");
    exception.addError(new FieldError("request", "name", "First"));
    exception.addError(new FieldError("request", "name", "Second"));

    NervErrorResponse response = mapper(DETAILS_ON).from(exception, REQUEST);

    assertThat(errors(response)).containsExactly(Map.entry("name", "First"));
  }

  @Test
  void shouldMapConstraintViolationExceptionToConstraintViolation() {

    ConstraintViolationException exception = new ConstraintViolationException(
        "Constraint violation",
        violations(
            violation("createUser.name", "must not be blank"),
            violation("createUser.age", "must be positive")));

    NervErrorResponse response = mapper(DETAILS_ON).from(exception, REQUEST);

    assertThat(response.code()).isEqualTo(NativeNervErrorCodes.CONSTRAINT_VIOLATION.code());
    assertThat(response.message()).isEqualTo(NativeNervErrorCodes.CONSTRAINT_VIOLATION.message());
    assertThat(response.status()).isEqualTo(400);
    assertThat(response.category()).isEqualTo("VALIDATION");
    assertThat(errors(response))
        .containsEntry("createUser.name", "must not be blank")
        .containsEntry("createUser.age", "must be positive");
  }

  @Test
  void shouldCollapseViolationsThatShareAPropertyPath() {

    // ConstraintViolationException stores its violations in an unordered set, so
    // which message wins is undefined -- what matters is that the duplicate key
    // does not blow up the Collectors.toMap accumulation.
    ConstraintViolationException exception = new ConstraintViolationException(
        "Constraint violation",
        violations(
            violation("createUser.name", "First"),
            violation("createUser.name", "Second")));

    NervErrorResponse response = mapper(DETAILS_ON).from(exception, REQUEST);

    assertThat(errors(response))
        .hasSize(1)
        .hasEntrySatisfying(
            "createUser.name",
            message -> assertThat(message).isIn("First", "Second"));
  }

  @Test
  void shouldNotIncludeConstraintViolationErrorsWhenDetailsDisabled() {

    ConstraintViolationException exception = new ConstraintViolationException(
        "Constraint violation",
        violations(violation("createUser.name", "must not be blank")));

    NervErrorResponse response = mapper(DETAILS_OFF).from(exception, REQUEST);

    assertThat(response.details()).isEmpty();
  }

  @Test
  void shouldMapHandlerMethodValidationExceptionToValidationError() {

    HandlerMethodValidationException exception =
        handlerMethodValidationException("Name is required");

    NervErrorResponse response = mapper(DETAILS_ON).from(exception, REQUEST);

    assertThat(response.code()).isEqualTo(NativeNervErrorCodes.VALIDATION_ERROR.code());
    assertThat(response.status()).isEqualTo(400);
    assertThat(errors(response)).containsEntry("name", "Name is required");
  }

  @Test
  void shouldFallBackToInvalidValueWhenHandlerMethodValidationErrorHasNoDefaultMessage() {

    HandlerMethodValidationException exception = handlerMethodValidationException(null);

    NervErrorResponse response = mapper(DETAILS_ON).from(exception, REQUEST);

    assertThat(errors(response)).containsEntry("name", "Invalid value");
  }

  @Test
  void shouldNotIncludeHandlerMethodValidationErrorsWhenDetailsDisabled() {

    HandlerMethodValidationException exception =
        handlerMethodValidationException("Name is required");

    NervErrorResponse response = mapper(DETAILS_OFF).from(exception, REQUEST);

    assertThat(response.details()).isEmpty();
  }

  // ---------------------------------------------------------------------
  // Bad request
  // ---------------------------------------------------------------------

  @Test
  void shouldMapMissingServletRequestParameterExceptionToBadRequest() {

    MissingServletRequestParameterException exception =
        new MissingServletRequestParameterException("page", "int");

    NervErrorResponse response = mapper(DETAILS_ON).from(exception, REQUEST);

    assertThat(response.code()).isEqualTo(NativeNervErrorCodes.BAD_REQUEST.code());
    assertThat(response.message()).isEqualTo(NativeNervErrorCodes.BAD_REQUEST.message());
    assertThat(response.status()).isEqualTo(400);
    assertThat(response.details())
        .containsEntry("parameter", "page")
        .containsEntry("expectedType", "int");
  }

  @Test
  void shouldNotIncludeMissingServletRequestParameterDetailsWhenDetailsDisabled() {

    MissingServletRequestParameterException exception =
        new MissingServletRequestParameterException("page", "int");

    NervErrorResponse response = mapper(DETAILS_OFF).from(exception, REQUEST);

    assertThat(response.details()).isEmpty();
  }

  @Test
  void shouldMapMissingRequestHeaderExceptionToBadRequest() {

    MissingRequestHeaderException exception =
        new MissingRequestHeaderException("X-Tenant-Id", methodParameter(0));

    NervErrorResponse response = mapper(DETAILS_ON).from(exception, REQUEST);

    assertThat(response.code()).isEqualTo(NativeNervErrorCodes.BAD_REQUEST.code());
    assertThat(response.status()).isEqualTo(400);
    assertThat(response.details()).containsEntry("header", "X-Tenant-Id");
  }

  @Test
  void shouldNotIncludeMissingRequestHeaderDetailsWhenDetailsDisabled() {

    MissingRequestHeaderException exception =
        new MissingRequestHeaderException("X-Tenant-Id", methodParameter(0));

    NervErrorResponse response = mapper(DETAILS_OFF).from(exception, REQUEST);

    assertThat(response.details()).isEmpty();
  }

  @Test
  void shouldMapMissingPathVariableExceptionToBadRequest() {

    MissingPathVariableException exception =
        new MissingPathVariableException("id", methodParameter(0));

    NervErrorResponse response = mapper(DETAILS_ON).from(exception, REQUEST);

    assertThat(response.code()).isEqualTo(NativeNervErrorCodes.BAD_REQUEST.code());
    assertThat(response.status()).isEqualTo(400);
    assertThat(response.details()).containsEntry("variable", "id");
  }

  @Test
  void shouldNotIncludeMissingPathVariableDetailsWhenDetailsDisabled() {

    MissingPathVariableException exception =
        new MissingPathVariableException("id", methodParameter(0));

    NervErrorResponse response = mapper(DETAILS_OFF).from(exception, REQUEST);

    assertThat(response.details()).isEmpty();
  }

  @Test
  void shouldMapMethodArgumentTypeMismatchExceptionToBadRequest() {

    MethodArgumentTypeMismatchException exception = new MethodArgumentTypeMismatchException(
        "abc",
        Integer.class,
        "age",
        methodParameter(1),
        new NumberFormatException("For input string: \"abc\""));

    NervErrorResponse response = mapper(DETAILS_ON).from(exception, REQUEST);

    assertThat(response.code()).isEqualTo(NativeNervErrorCodes.BAD_REQUEST.code());
    assertThat(response.status()).isEqualTo(400);
    assertThat(response.details())
        .containsEntry("parameter", "age")
        .containsEntry("value", "abc")
        .containsEntry("expectedType", "Integer");
  }

  @Test
  void shouldOmitExpectedTypeWhenMethodArgumentTypeMismatchHasNoRequiredType() {

    MethodArgumentTypeMismatchException exception = new MethodArgumentTypeMismatchException(
        "abc",
        null,
        "age",
        methodParameter(1),
        new NumberFormatException("For input string: \"abc\""));

    NervErrorResponse response = mapper(DETAILS_ON).from(exception, REQUEST);

    assertThat(response.details())
        .containsEntry("parameter", "age")
        .doesNotContainKey("expectedType");
  }

  @Test
  void shouldNotIncludeMethodArgumentTypeMismatchDetailsWhenDetailsDisabled() {

    MethodArgumentTypeMismatchException exception = new MethodArgumentTypeMismatchException(
        "abc",
        Integer.class,
        "age",
        methodParameter(1),
        new NumberFormatException("boom"));

    NervErrorResponse response = mapper(DETAILS_OFF).from(exception, REQUEST);

    assertThat(response.details()).isEmpty();
  }

  @Test
  void shouldMapHttpMessageNotReadableExceptionToBadRequest() {

    HttpMessageNotReadableException exception = new HttpMessageNotReadableException(
        "Malformed JSON",
        new MockHttpInputMessage("{".getBytes()));

    NervErrorResponse response = mapper(DETAILS_ON).from(exception, REQUEST);

    assertThat(response.code()).isEqualTo(NativeNervErrorCodes.BAD_REQUEST.code());
    assertThat(response.message()).isEqualTo(NativeNervErrorCodes.BAD_REQUEST.message());
    assertThat(response.status()).isEqualTo(400);
    assertThat(response.details()).isEmpty();
  }

  // ---------------------------------------------------------------------
  // Routing / negotiation
  // ---------------------------------------------------------------------

  @Test
  void shouldMapNoHandlerFoundExceptionToResourceNotFound() {

    NoHandlerFoundException exception =
        new NoHandlerFoundException("GET", "/missing", new HttpHeaders());

    NervErrorResponse response = mapper(DETAILS_ON).from(exception, REQUEST);

    assertThat(response.code()).isEqualTo(NativeNervErrorCodes.RESOURCE_NOT_FOUND.code());
    assertThat(response.message()).isEqualTo(NativeNervErrorCodes.RESOURCE_NOT_FOUND.message());
    assertThat(response.status()).isEqualTo(404);
    assertThat(response.category()).isEqualTo("CLIENT");
  }

  @Test
  void shouldMapHttpRequestMethodNotSupportedExceptionToMethodNotAllowed() {

    HttpRequestMethodNotSupportedException exception =
        new HttpRequestMethodNotSupportedException("POST", List.of("GET", "HEAD"));

    NervErrorResponse response = mapper(DETAILS_ON).from(exception, REQUEST);

    assertThat(response.code()).isEqualTo(NativeNervErrorCodes.METHOD_NOT_ALLOWED.code());
    assertThat(response.status()).isEqualTo(405);
    assertThat(response.details()).containsEntry("method", "POST");
    assertThat((String[]) response.details().get("supportedMethods"))
        .containsExactly("GET", "HEAD");
  }

  @Test
  void shouldNotIncludeMethodNotAllowedDetailsWhenDetailsDisabled() {

    HttpRequestMethodNotSupportedException exception =
        new HttpRequestMethodNotSupportedException("POST", List.of("GET"));

    NervErrorResponse response = mapper(DETAILS_OFF).from(exception, REQUEST);

    assertThat(response.details()).isEmpty();
  }

  @Test
  void shouldMapHttpMediaTypeNotSupportedExceptionToUnsupportedMediaType() {

    HttpMediaTypeNotSupportedException exception = new HttpMediaTypeNotSupportedException(
        MediaType.TEXT_PLAIN,
        List.of(MediaType.APPLICATION_JSON));

    NervErrorResponse response = mapper(DETAILS_ON).from(exception, REQUEST);

    assertThat(response.code()).isEqualTo(NativeNervErrorCodes.UNSUPPORTED_MEDIA_TYPE.code());
    assertThat(response.status()).isEqualTo(415);
    assertThat(response.details())
        .containsEntry("contentType", MediaType.TEXT_PLAIN)
        .containsEntry("supportedMediaTypes", List.of(MediaType.APPLICATION_JSON));
  }

  @Test
  void shouldNotIncludeUnsupportedMediaTypeDetailsWhenDetailsDisabled() {

    HttpMediaTypeNotSupportedException exception = new HttpMediaTypeNotSupportedException(
        MediaType.TEXT_PLAIN,
        List.of(MediaType.APPLICATION_JSON));

    NervErrorResponse response = mapper(DETAILS_OFF).from(exception, REQUEST);

    assertThat(response.details()).isEmpty();
  }

  @Test
  void shouldMapHttpMediaTypeNotAcceptableExceptionToNotAcceptable() {

    HttpMediaTypeNotAcceptableException exception =
        new HttpMediaTypeNotAcceptableException(List.of(MediaType.APPLICATION_JSON));

    NervErrorResponse response = mapper(DETAILS_ON).from(exception, REQUEST);

    assertThat(response.code()).isEqualTo(NativeNervErrorCodes.NOT_ACCEPTABLE.code());
    assertThat(response.status()).isEqualTo(406);
    assertThat(response.details())
        .containsEntry("supportedMediaTypes", List.of(MediaType.APPLICATION_JSON));
  }

  @Test
  void shouldNotIncludeNotAcceptableDetailsWhenDetailsDisabled() {

    HttpMediaTypeNotAcceptableException exception =
        new HttpMediaTypeNotAcceptableException(List.of(MediaType.APPLICATION_JSON));

    NervErrorResponse response = mapper(DETAILS_OFF).from(exception, REQUEST);

    assertThat(response.details()).isEmpty();
  }

  // ---------------------------------------------------------------------
  // Security
  // ---------------------------------------------------------------------

  static List<Arguments> authenticationExceptions() {
    return List.of(
        Arguments.of(new BadCredentialsException("raw"), "Bad credentials"),
        Arguments.of(new InsufficientAuthenticationException("raw"), "Insufficient authentication"),
        Arguments.of(new LockedException("raw"), "Account locked"),
        Arguments.of(new DisabledException("raw"), "Account disabled"),
        Arguments.of(new AccountExpiredException("raw"), "Account expired"),
        Arguments.of(new CredentialsExpiredException("raw"), "Credentials expired"),
        Arguments.of(new AuthenticationServiceException("raw"), "Authentication error"));
  }

  @ParameterizedTest
  @MethodSource("authenticationExceptions")
  void shouldMapAuthenticationExceptionToUnauthorized(
      AuthenticationException exception,
      String expectedMessage) {

    NervErrorResponse response = mapper(DETAILS_ON).from(exception, REQUEST);

    assertThat(response.code()).isEqualTo(NativeNervErrorCodes.UNAUTHORIZED.code());
    assertThat(response.message()).isEqualTo(expectedMessage);
    assertThat(response.status()).isEqualTo(401);
    assertThat(response.category()).isEqualTo("SECURITY");
    assertThat(response.retryable()).isFalse();
  }

  @Test
  void shouldNeverLeakTheRawAuthenticationExceptionMessage() {

    NervExceptionSettings exposeInternalMessage =
        new NervExceptionSettings(true, true, false, false);

    NervErrorResponse response = mapper(exposeInternalMessage)
        .from(new BadCredentialsException("password 'hunter2' did not match"), REQUEST);

    assertThat(response.message()).isEqualTo("Bad credentials");
  }

  @Test
  void shouldMapAccessDeniedExceptionToUnauthorized() {

    java.nio.file.AccessDeniedException exception =
        new java.nio.file.AccessDeniedException("/secret");

    NervErrorResponse response = mapper(DETAILS_ON).from(exception, REQUEST);

    assertThat(response.code()).isEqualTo(NativeNervErrorCodes.UNAUTHORIZED.code());
    assertThat(response.message()).isEqualTo(NativeNervErrorCodes.UNAUTHORIZED.message());
    assertThat(response.status()).isEqualTo(401);
    assertThat(response.category()).isEqualTo("SECURITY");
  }

  // ---------------------------------------------------------------------
  // Cross-cutting response fields
  // ---------------------------------------------------------------------

  @Test
  void shouldPopulateTraceAndSpanIds() {

    NervTraceContextResolver resolver =
        () -> new NervTraceContext("0af7651916cd43dd8448eb211c80319c", "b9c7c989f97918e1");

    NervErrorResponseMapper mapper =
        new NervErrorResponseMapper(DETAILS_ON, resolver, new NoOpNervOriginResolver());

    NervErrorResponse response = mapper.from(new IllegalStateException("boom"), REQUEST);

    assertThat(response.traceId()).isEqualTo("0af7651916cd43dd8448eb211c80319c");
    assertThat(response.spanId()).isEqualTo("b9c7c989f97918e1");
  }

  @Test
  void shouldLeaveTraceAndSpanIdsNullWhenNoTraceContextIsAvailable() {

    NervErrorResponse response =
        mapper(DETAILS_ON).from(new IllegalStateException("boom"), REQUEST);

    assertThat(response.traceId()).isNull();
    assertThat(response.spanId()).isNull();
  }

  @Test
  void shouldPopulateOriginFromResolver() {

    NervOrigin origin = NervOrigin.builder()
        .service("payment-service")
        .instance("payment-service-7d4f")
        .version("1.3.0")
        .environment("prod")
        .build();

    NervOriginResolver originResolver = () -> origin;

    NervErrorResponseMapper mapper =
        new NervErrorResponseMapper(DETAILS_ON, NO_OP_TRACE, originResolver);

    NervErrorResponse response = mapper.from(new IllegalStateException("boom"), REQUEST);

    assertThat(response.origin()).isEqualTo(origin);
  }

  @Test
  void shouldPopulatePathAndTimestamp() {

    Instant before = Instant.now();

    NervErrorResponse response = mapper(DETAILS_ON).from(
        new IllegalStateException("boom"),
        new MockHttpServletRequest("POST", "/api/payments/42"));

    assertThat(response.path()).isEqualTo("/api/payments/42");
    assertThat(response.timestamp()).isNotNull().isAfterOrEqualTo(before);
  }

  // ---------------------------------------------------------------------
  // Fixtures
  // ---------------------------------------------------------------------

  @SuppressWarnings("unchecked")
  private static Map<String, String> errors(NervErrorResponse response) {

    assertThat(response.details()).containsKey("errors");

    return (Map<String, String>) response.details().get("errors");
  }

  private static MethodArgumentNotValidException methodArgumentNotValidException(
      FieldError... fieldErrors) {

    BeanPropertyBindingResult bindingResult =
        new BeanPropertyBindingResult(new TestRequest(), "request");

    for (FieldError fieldError : fieldErrors) {
      bindingResult.addError(fieldError);
    }

    return new MethodArgumentNotValidException(methodParameter(0), bindingResult);
  }

  private static HandlerMethodValidationException handlerMethodValidationException(
      String defaultMessage) {

    MethodParameter parameter = methodParameter(0);

    MessageSourceResolvable error = new DefaultMessageSourceResolvable(
        new String[] {"NotBlank"},
        null,
        defaultMessage);

    ParameterValidationResult result = new ParameterValidationResult(
        parameter,
        "",
        List.of(error),
        null,
        null,
        null,
        (resolvable, type) -> {
          throw new IllegalArgumentException("Not supported");
        });

    return new HandlerMethodValidationException(
        MethodValidationResult.create(
            new NervErrorResponseMapperTest(),
            parameter.getMethod(),
            List.of(result)));
  }

  private static MethodParameter methodParameter(int index) {

    try {
      Method method = NervErrorResponseMapperTest.class
          .getDeclaredMethod("annotatedHandler", String.class, Integer.class);

      MethodParameter parameter = new MethodParameter(method, index);
      parameter.initParameterNameDiscovery(new DefaultParameterNameDiscoverer());

      return parameter;

    } catch (NoSuchMethodException exception) {
      throw new IllegalStateException(exception);
    }
  }

  @SuppressWarnings("unused")
  private void annotatedHandler(String name, Integer age) {
    // Reflection target used to build MethodParameter instances.
  }

  private static Set<ConstraintViolation<?>> violations(ConstraintViolation<?>... violations) {
    return new LinkedHashSet<>(List.of(violations));
  }

  private static ConstraintViolation<?> violation(String propertyPath, String message) {

    ConstraintViolation<?> violation = mock(ConstraintViolation.class);
    when(violation.getPropertyPath()).thenReturn(new TestPath(propertyPath));
    when(violation.getMessage()).thenReturn(message);

    return violation;
  }

  private record TestPath(String value) implements Path {

    @Override
    public Iterator<Node> iterator() {
      return Collections.emptyIterator();
    }

    @Override
    public String toString() {
      return value;
    }
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
