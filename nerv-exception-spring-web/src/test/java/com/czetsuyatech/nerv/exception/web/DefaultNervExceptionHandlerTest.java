package com.czetsuyatech.nerv.exception.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.czetsuyatech.nerv.exception.core.NervErrorHeaders;
import com.czetsuyatech.nerv.exception.core.NervException;
import com.czetsuyatech.nerv.exception.core.code.NativeNervErrorCodes;
import com.czetsuyatech.nerv.exception.core.origin.NoOpNervOriginResolver;
import com.czetsuyatech.nerv.exception.trace.NervTraceContext;
import com.czetsuyatech.nerv.exception.trace.NervTraceContextResolver;
import com.czetsuyatech.nerv.exception.trace.NoOpNervTraceContextResolver;
import jakarta.validation.ConstraintViolationException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.MethodValidationResult;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.NoHandlerFoundException;

class DefaultNervExceptionHandlerTest {

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = mockMvc(new NoOpNervTraceContextResolver());
  }

  private static MockMvc mockMvc(NervTraceContextResolver traceContextResolver) {

    NervExceptionSettings settings = new NervExceptionSettings(
        true,
        false,
        false,
        false);

    NervErrorResponseMapper mapper = new NervErrorResponseMapper(
        settings,
        traceContextResolver,
        new NoOpNervOriginResolver());

    return MockMvcBuilders
        .standaloneSetup(new TestController())
        .setControllerAdvice(new DefaultNervExceptionHandler(mapper))
        .build();
  }

  // ---------------------------------------------------------------------
  // NervException
  // ---------------------------------------------------------------------

  @Test
  void shouldHandleNervException() throws Exception {

    mockMvc.perform(get("/nerv"))
        .andExpect(status().isConflict())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.code").value("CONFLICT"))
        .andExpect(jsonPath("$.message").value("Duplicate resource"))
        .andExpect(jsonPath("$.status").value(409))
        .andExpect(jsonPath("$.category").value("CLIENT"))
        .andExpect(jsonPath("$.retryable").value(false))
        .andExpect(jsonPath("$.path").value("/nerv"))
        .andExpect(jsonPath("$.timestamp").exists())
        .andExpect(jsonPath("$.origin.service").value("unknown-service"));
  }

  @Test
  void shouldExposeErrorMetadataAsResponseHeaders() throws Exception {

    mockMvc.perform(get("/nerv"))
        .andExpect(header().string(NervErrorHeaders.ERROR_CODE, "CONFLICT"))
        .andExpect(header().string(NervErrorHeaders.ERROR_CATEGORY, "CLIENT"))
        .andExpect(header().string(NervErrorHeaders.ERROR_RETRYABLE, "false"));
  }

  @Test
  void shouldReportRetryableErrorCodesInTheRetryableHeader() throws Exception {

    mockMvc.perform(get("/retryable"))
        .andExpect(status().isGatewayTimeout())
        .andExpect(header().string(NervErrorHeaders.ERROR_CODE, "GATEWAY_TIMEOUT"))
        .andExpect(header().string(NervErrorHeaders.ERROR_CATEGORY, "DOWNSTREAM"))
        .andExpect(header().string(NervErrorHeaders.ERROR_RETRYABLE, "true"))
        .andExpect(jsonPath("$.retryable").value(true));
  }

  // ---------------------------------------------------------------------
  // Trace headers
  // ---------------------------------------------------------------------

  @Test
  void shouldOmitTraceHeadersWhenNoTraceContextIsAvailable() throws Exception {

    mockMvc.perform(get("/nerv"))
        .andExpect(header().doesNotExist(NervErrorHeaders.TRACE_ID))
        .andExpect(header().doesNotExist(NervErrorHeaders.SPAN_ID));
  }

  @Test
  void shouldExposeTraceHeadersWhenTraceContextIsAvailable() throws Exception {

    MockMvc tracing = mockMvc(
        () -> new NervTraceContext("0af7651916cd43dd8448eb211c80319c", "b9c7c989f97918e1"));

    tracing.perform(get("/nerv"))
        .andExpect(header().string(
            NervErrorHeaders.TRACE_ID, "0af7651916cd43dd8448eb211c80319c"))
        .andExpect(header().string(
            NervErrorHeaders.SPAN_ID, "b9c7c989f97918e1"))
        .andExpect(jsonPath("$.traceId").value("0af7651916cd43dd8448eb211c80319c"))
        .andExpect(jsonPath("$.spanId").value("b9c7c989f97918e1"));
  }

  @Test
  void shouldOmitTraceHeadersWhenTraceContextIsBlank() throws Exception {

    MockMvc tracing = mockMvc(() -> new NervTraceContext("   ", ""));

    tracing.perform(get("/nerv"))
        .andExpect(header().doesNotExist(NervErrorHeaders.TRACE_ID))
        .andExpect(header().doesNotExist(NervErrorHeaders.SPAN_ID));
  }

  // ---------------------------------------------------------------------
  // Validation
  // ---------------------------------------------------------------------

  @Test
  void shouldHandleMethodArgumentNotValidException() throws Exception {

    mockMvc.perform(get("/method-argument-not-valid"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code")
            .value(NativeNervErrorCodes.VALIDATION_ERROR.code()))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.details.errors.name").value("Name is required"));
  }

  @Test
  void shouldHandleBindException() throws Exception {

    mockMvc.perform(get("/bind"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code")
            .value(NativeNervErrorCodes.VALIDATION_ERROR.code()))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.details.errors.name").value("Name is required"));
  }

  @Test
  void shouldHandleConstraintViolationException() throws Exception {

    mockMvc.perform(get("/constraint"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code")
            .value(NativeNervErrorCodes.CONSTRAINT_VIOLATION.code()))
        .andExpect(jsonPath("$.message")
            .value(NativeNervErrorCodes.CONSTRAINT_VIOLATION.message()))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.path").value("/constraint"));
  }

  @Test
  void shouldHandleHandlerMethodValidationException() throws Exception {

    mockMvc.perform(get("/handler-method-validation"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code")
            .value(NativeNervErrorCodes.VALIDATION_ERROR.code()))
        .andExpect(jsonPath("$.details.errors.name").value("Name is required"));
  }

  // ---------------------------------------------------------------------
  // Bad request
  // ---------------------------------------------------------------------

  @Test
  void shouldHandleMissingServletRequestParameterException() throws Exception {

    mockMvc.perform(get("/required-param"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(NativeNervErrorCodes.BAD_REQUEST.code()))
        .andExpect(jsonPath("$.details.parameter").value("page"))
        .andExpect(jsonPath("$.details.expectedType").value("int"));
  }

  @Test
  void shouldHandleMissingRequestHeaderException() throws Exception {

    mockMvc.perform(get("/required-header"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(NativeNervErrorCodes.BAD_REQUEST.code()))
        .andExpect(jsonPath("$.details.header").value("X-Tenant-Id"));
  }

  @Test
  void shouldHandleMissingPathVariableException() throws Exception {

    mockMvc.perform(get("/missing-path-variable"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(NativeNervErrorCodes.BAD_REQUEST.code()))
        .andExpect(jsonPath("$.details.variable").value("id"));
  }

  @Test
  void shouldHandleMethodArgumentTypeMismatchException() throws Exception {

    mockMvc.perform(get("/typed-param").param("age", "abc"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(NativeNervErrorCodes.BAD_REQUEST.code()))
        .andExpect(jsonPath("$.details.parameter").value("age"))
        .andExpect(jsonPath("$.details.value").value("abc"))
        .andExpect(jsonPath("$.details.expectedType").value("Integer"));
  }

  @Test
  void shouldHandleHttpMessageNotReadableException() throws Exception {

    mockMvc.perform(get("/unreadable"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(NativeNervErrorCodes.BAD_REQUEST.code()))
        .andExpect(jsonPath("$.message").value(NativeNervErrorCodes.BAD_REQUEST.message()));
  }

  // ---------------------------------------------------------------------
  // Routing / negotiation
  // ---------------------------------------------------------------------

  @Test
  void shouldHandleNoHandlerFoundException() throws Exception {

    mockMvc.perform(get("/no-handler"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code")
            .value(NativeNervErrorCodes.RESOURCE_NOT_FOUND.code()))
        .andExpect(jsonPath("$.status").value(404));
  }

  @Test
  void shouldHandleHttpRequestMethodNotSupportedException() throws Exception {

    mockMvc.perform(post("/nerv"))
        .andExpect(status().isMethodNotAllowed())
        .andExpect(jsonPath("$.code")
            .value(NativeNervErrorCodes.METHOD_NOT_ALLOWED.code()))
        .andExpect(jsonPath("$.status").value(405))
        .andExpect(jsonPath("$.details.method").value("POST"));
  }

  @Test
  void shouldHandleHttpMediaTypeNotSupportedException() throws Exception {

    mockMvc.perform(post("/consumes-json")
            .contentType(MediaType.TEXT_PLAIN)
            .content("not json"))
        .andExpect(status().isUnsupportedMediaType())
        .andExpect(jsonPath("$.code")
            .value(NativeNervErrorCodes.UNSUPPORTED_MEDIA_TYPE.code()))
        .andExpect(jsonPath("$.status").value(415))
        .andExpect(jsonPath("$.details.contentType").exists());
  }

  @Test
  void shouldHandleHttpMediaTypeNotAcceptableException() throws Exception {

    mockMvc.perform(get("/not-acceptable"))
        .andExpect(status().isNotAcceptable())
        .andExpect(jsonPath("$.code")
            .value(NativeNervErrorCodes.NOT_ACCEPTABLE.code()))
        .andExpect(jsonPath("$.status").value(406));
  }

  // ---------------------------------------------------------------------
  // Security
  //
  // DefaultNervExceptionHandler declares its two security handlers against
  // java.nio.file.AccessDeniedException and javax.security.sasl.
  // AuthenticationException -- NOT the Spring Security types of the same
  // simple name. These tests pin the behaviour that ships today.
  // ---------------------------------------------------------------------

  @Test
  void shouldHandleNioAccessDeniedExceptionAsUnauthorized() throws Exception {

    mockMvc.perform(get("/nio-access-denied"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value(NativeNervErrorCodes.UNAUTHORIZED.code()))
        .andExpect(jsonPath("$.status").value(401))
        .andExpect(jsonPath("$.category").value("SECURITY"));
  }

  @Test
  void shouldFallBackToInternalServerErrorForSaslAuthenticationException() throws Exception {

    // The handler routes it, but the mapper overload picked at compile time is
    // from(Exception, ..) -- there is no from(javax.security.sasl..) overload.
    mockMvc.perform(get("/sasl-authentication"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.code")
            .value(NativeNervErrorCodes.INTERNAL_SERVER_ERROR.code()));
  }

  @Test
  void shouldFallBackToInternalServerErrorForSpringSecurityAuthenticationException()
      throws Exception {

    // No @ExceptionHandler matches org.springframework.security.core
    // .AuthenticationException, so it lands on the catch-all handler.
    mockMvc.perform(get("/bad-credentials"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.code")
            .value(NativeNervErrorCodes.INTERNAL_SERVER_ERROR.code()));
  }

  @Test
  void shouldFallBackToInternalServerErrorForSpringSecurityAccessDeniedException()
      throws Exception {

    // No @ExceptionHandler matches org.springframework.security.access
    // .AccessDeniedException, so it lands on the catch-all handler.
    mockMvc.perform(get("/spring-access-denied"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.code")
            .value(NativeNervErrorCodes.INTERNAL_SERVER_ERROR.code()));
  }

  // ---------------------------------------------------------------------
  // Catch-all
  // ---------------------------------------------------------------------

  @Test
  void shouldHandleUnhandledException() throws Exception {

    mockMvc.perform(get("/error"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.code")
            .value(NativeNervErrorCodes.INTERNAL_SERVER_ERROR.code()))
        .andExpect(jsonPath("$.message")
            .value(NativeNervErrorCodes.INTERNAL_SERVER_ERROR.message()))
        .andExpect(jsonPath("$.status").value(500))
        .andExpect(jsonPath("$.path").value("/error"))
        .andExpect(header().string(NervErrorHeaders.ERROR_CODE, "INTERNAL_SERVER_ERROR"))
        .andExpect(header().string(NervErrorHeaders.ERROR_CATEGORY, "SYSTEM"));
  }

  @Test
  void shouldNotLeakInternalMessagesFromUnhandledExceptions() throws Exception {

    mockMvc.perform(get("/leaky"))
        .andExpect(status().isInternalServerError())
        .andExpect(content().string(Matchers.not(Matchers.containsString("hunter2"))));
  }

  // ---------------------------------------------------------------------
  // Fixtures
  // ---------------------------------------------------------------------

  private static MethodParameter methodParameter() {

    try {
      Method method = DefaultNervExceptionHandlerTest.class
          .getDeclaredMethod("validatedHandler", String.class);

      MethodParameter parameter = new MethodParameter(method, 0);
      parameter.initParameterNameDiscovery(new DefaultParameterNameDiscoverer());

      return parameter;

    } catch (NoSuchMethodException exception) {
      throw new IllegalStateException(exception);
    }
  }

  @SuppressWarnings("unused")
  private void validatedHandler(String name) {
    // Reflection target used to build MethodParameter instances.
  }

  @RestController
  static class TestController {

    @GetMapping("/nerv")
    String nerv() {
      throw NervException.of(NativeNervErrorCodes.CONFLICT, "Duplicate resource");
    }

    @GetMapping("/retryable")
    String retryable() {
      throw NervException.of(NativeNervErrorCodes.GATEWAY_TIMEOUT);
    }

    @GetMapping("/method-argument-not-valid")
    String methodArgumentNotValid() throws Exception {

      BeanPropertyBindingResult bindingResult =
          new BeanPropertyBindingResult(new Object(), "request");
      bindingResult.addError(new FieldError("request", "name", "Name is required"));

      throw new MethodArgumentNotValidException(methodParameter(), bindingResult);
    }

    @GetMapping("/bind")
    String bind() throws Exception {

      BindException exception = new BindException(new Object(), "request");
      exception.addError(new FieldError("request", "name", "Name is required"));

      throw exception;
    }

    @GetMapping("/constraint")
    String constraint() {
      throw new ConstraintViolationException("Constraint violation", Set.of());
    }

    @GetMapping("/handler-method-validation")
    String handlerMethodValidation() {

      MethodParameter parameter = methodParameter();

      ParameterValidationResult result = new ParameterValidationResult(
          parameter,
          "",
          List.of(new DefaultMessageSourceResolvable(
              new String[] {"NotBlank"},
              null,
              "Name is required")),
          null,
          null,
          null,
          (resolvable, type) -> {
            throw new IllegalArgumentException("Not supported");
          });

      throw new HandlerMethodValidationException(
          MethodValidationResult.create(
              new DefaultNervExceptionHandlerTest(),
              parameter.getMethod(),
              List.of(result)));
    }

    @GetMapping("/required-param")
    String requiredParam(@RequestParam("page") int page) {
      return String.valueOf(page);
    }

    @GetMapping("/required-header")
    String requiredHeader(@RequestHeader("X-Tenant-Id") String tenantId) {
      return tenantId;
    }

    @GetMapping("/missing-path-variable")
    String missingPathVariable() throws Exception {
      throw new MissingPathVariableException("id", methodParameter());
    }

    @GetMapping("/typed-param")
    String typedParam(@RequestParam("age") Integer age) {
      return String.valueOf(age);
    }

    @GetMapping("/unreadable")
    String unreadable() {
      throw new HttpMessageNotReadableException(
          "Malformed JSON",
          new MockHttpInputMessage("{".getBytes()));
    }

    @GetMapping("/no-handler")
    String noHandler() throws Exception {
      throw new NoHandlerFoundException("GET", "/missing", new HttpHeaders());
    }

    @PostMapping(value = "/consumes-json", consumes = MediaType.APPLICATION_JSON_VALUE)
    String consumesJson() {
      return "ok";
    }

    @GetMapping("/not-acceptable")
    String notAcceptable() throws Exception {
      throw new HttpMediaTypeNotAcceptableException(List.of(MediaType.APPLICATION_JSON));
    }

    @GetMapping("/nio-access-denied")
    String nioAccessDenied() throws Exception {
      throw new java.nio.file.AccessDeniedException("/secret");
    }

    @GetMapping("/sasl-authentication")
    String saslAuthentication() throws Exception {
      throw new javax.security.sasl.AuthenticationException("sasl failure");
    }

    @GetMapping("/bad-credentials")
    String badCredentials() {
      throw new BadCredentialsException("Bad credentials");
    }

    @GetMapping("/spring-access-denied")
    String springAccessDenied() {
      throw new AccessDeniedException("Access is denied");
    }

    @GetMapping("/error")
    String error() {
      throw new IllegalStateException("boom");
    }

    @GetMapping("/leaky")
    String leaky() {
      throw new IllegalStateException("database password is hunter2");
    }
  }
}
