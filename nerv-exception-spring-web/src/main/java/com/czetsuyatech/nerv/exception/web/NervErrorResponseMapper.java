package com.czetsuyatech.nerv.exception.web;

import com.czetsuyatech.nerv.exception.api.NervErrorCode;
import com.czetsuyatech.nerv.exception.core.NervException;
import com.czetsuyatech.nerv.exception.core.code.NativeNervErrorCodes;
import com.czetsuyatech.nerv.exception.core.model.NervErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.servlet.NoHandlerFoundException;

@RequiredArgsConstructor
public class NervErrorResponseMapper {

  private final NervExceptionSettings settings;
  private final NervTraceContextResolver traceContextResolver;

  public NervErrorResponse from(
      NervException exception,
      HttpServletRequest request) {

    NervErrorCode errorCode = exception.getErrorCode();

    return build(
        errorCode,
        exception.getMessage(),
        request,
        resolveNervExceptionDetails(exception));
  }

  public NervErrorResponse from(
      MethodArgumentNotValidException exception,
      HttpServletRequest request) {

    return build(
        NativeNervErrorCodes.VALIDATION_ERROR,
        NativeNervErrorCodes.VALIDATION_ERROR.message(),
        request,
        validationDetails(exception));
  }

  public NervErrorResponse from(
      BindException exception,
      HttpServletRequest request) {

    return build(
        NativeNervErrorCodes.VALIDATION_ERROR,
        NativeNervErrorCodes.VALIDATION_ERROR.message(),
        request,
        validationDetails(exception));
  }

  public NervErrorResponse from(
      ConstraintViolationException exception,
      HttpServletRequest request) {

    return build(
        NativeNervErrorCodes.CONSTRAINT_VIOLATION,
        NativeNervErrorCodes.CONSTRAINT_VIOLATION.message(),
        request,
        constraintViolationDetails(exception));
  }

  public NervErrorResponse from(
      NoHandlerFoundException exception,
      HttpServletRequest request) {

    return build(
        NativeNervErrorCodes.RESOURCE_NOT_FOUND,
        NativeNervErrorCodes.RESOURCE_NOT_FOUND.message(),
        request,
        exceptionDetails(exception));
  }

  public NervErrorResponse from(
      HttpRequestMethodNotSupportedException exception,
      HttpServletRequest request) {

    Map<String, Object> details = exceptionDetails(exception);

    if (settings.includeDetails()) {
      details.put("method", exception.getMethod());
      details.put("supportedMethods", exception.getSupportedMethods());
    }

    return build(
        NativeNervErrorCodes.METHOD_NOT_ALLOWED,
        NativeNervErrorCodes.METHOD_NOT_ALLOWED.message(),
        request,
        details);
  }

  public NervErrorResponse from(
      Exception exception,
      HttpServletRequest request) {

    String message = settings.exposeInternalMessage()
        ? exception.getMessage()
        : NativeNervErrorCodes.INTERNAL_SERVER_ERROR.message();

    return build(
        NativeNervErrorCodes.INTERNAL_SERVER_ERROR,
        message,
        request,
        exceptionDetails(exception));
  }

  private NervErrorResponse build(
      NervErrorCode errorCode,
      String message,
      HttpServletRequest request,
      Map<String, Object> details) {

    return NervErrorResponse.builder()
        .code(errorCode.code())
        .message(message)
        .status(errorCode.status())
        .retryable(errorCode.retryable())
        .category(errorCode.category())
        .traceId(traceContextResolver.traceId())
        .spanId(traceContextResolver.spanId())
        .path(request.getRequestURI())
        .timestamp(Instant.now())
        .details(details)
        .build();
  }

  private Map<String, Object> resolveNervExceptionDetails(NervException exception) {

    Map<String, Object> details = exceptionDetails(exception);

    if (settings.includeDetails() && exception.getDetails() != null) {
      details.putAll(exception.getDetails());
    }

    return details;
  }

  private Map<String, Object> validationDetails(MethodArgumentNotValidException exception) {

    Map<String, Object> details = exceptionDetails(exception);

    if (settings.includeDetails()) {
      details.put("errors", fieldErrors(exception.getBindingResult().getFieldErrors()));
    }

    return details;
  }

  private Map<String, Object> validationDetails(BindException exception) {

    Map<String, Object> details = exceptionDetails(exception);

    if (settings.includeDetails()) {
      details.put("errors", fieldErrors(exception.getBindingResult().getFieldErrors()));
    }

    return details;
  }

  private Map<String, String> fieldErrors(Iterable<FieldError> fieldErrors) {

    Map<String, String> errors = new LinkedHashMap<>();

    for (FieldError fieldError : fieldErrors) {
      errors.putIfAbsent(
          fieldError.getField(),
          fieldError.getDefaultMessage() == null
              ? "Invalid value"
              : fieldError.getDefaultMessage());
    }

    return errors;
  }

  private Map<String, Object> constraintViolationDetails(
      ConstraintViolationException exception) {

    Map<String, Object> details = exceptionDetails(exception);

    if (settings.includeDetails()) {
      details.put(
          "errors",
          exception.getConstraintViolations()
              .stream()
              .collect(Collectors.toMap(
                  violation -> violation.getPropertyPath().toString(),
                  ConstraintViolation::getMessage,
                  (first, second) -> first,
                  LinkedHashMap::new)));
    }

    return details;
  }

  private Map<String, Object> exceptionDetails(Exception exception) {

    Map<String, Object> details = new LinkedHashMap<>();

    if (settings.includeCause() && exception.getCause() != null) {
      details.put("cause", exception.getCause().getClass().getName());
    }

    if (settings.includeStackTrace()) {
      details.put("stackTrace", stackTrace(exception));
    }

    return details;
  }

  private String stackTrace(Exception exception) {

    StringWriter stringWriter = new StringWriter();

    exception.printStackTrace(new PrintWriter(stringWriter));

    return stringWriter.toString();
  }
}
