package com.czetsuyatech.nerv.exception.web;

import com.czetsuyatech.nerv.exception.core.NervException;
import com.czetsuyatech.nerv.exception.core.code.NativeNervErrorCodes;
import jakarta.validation.ConstraintViolationException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
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

@RestController
public class TestController {

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
            new String[]{"NotBlank"},
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

  // ---------------------------------------------------------------------
  // Fixtures
  // ---------------------------------------------------------------------

  private static MethodParameter methodParameter() {

    try {
      Method method = TestController.class
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
}
