package com.czetsuyatech.nerv.exception.web;

import com.czetsuyatech.nerv.exception.api.NervErrorCode;
import com.czetsuyatech.nerv.exception.api.origin.NervOrigin;
import com.czetsuyatech.nerv.exception.api.origin.NervOriginResolver;
import com.czetsuyatech.nerv.exception.core.NervException;
import com.czetsuyatech.nerv.exception.core.code.NativeNervErrorCodes;
import com.czetsuyatech.nerv.exception.core.model.NervErrorResponse;
import com.czetsuyatech.nerv.exception.trace.NervTraceContextResolver;
import jakarta.persistence.NoResultException;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PessimisticLockException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.NotNull;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.AccessDeniedException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
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

@RequiredArgsConstructor
public class NervErrorResponseMapper {

  private final NervExceptionSettings settings;
  private final NervTraceContextResolver traceContextResolver;
  private final NervOriginResolver originResolver;

  public NervErrorResponse from(
      NervException exception,
      HttpServletRequest request) {

    NervErrorCode errorCode = exception.getErrorCode();

    return build(
        exception.getOrigin(),
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
      HandlerMethodValidationException exception,
      HttpServletRequest request) {

    return build(
        NativeNervErrorCodes.VALIDATION_ERROR,
        NativeNervErrorCodes.VALIDATION_ERROR.message(),
        request,
        handlerMethodValidationDetails(exception));
  }

  public NervErrorResponse from(
      MissingServletRequestParameterException exception,
      HttpServletRequest request) {

    return build(
        NativeNervErrorCodes.BAD_REQUEST,
        NativeNervErrorCodes.BAD_REQUEST.message(),
        request,
        missingServletRequestParameterDetails(exception));
  }

  public NervErrorResponse from(
      MissingRequestHeaderException exception,
      HttpServletRequest request) {

    return build(
        NativeNervErrorCodes.BAD_REQUEST,
        NativeNervErrorCodes.BAD_REQUEST.message(),
        request,
        missingRequestHeaderDetails(exception));
  }

  public NervErrorResponse from(
      MissingPathVariableException exception,
      HttpServletRequest request) {

    return build(
        NativeNervErrorCodes.BAD_REQUEST,
        NativeNervErrorCodes.BAD_REQUEST.message(),
        request,
        missingPathVariableDetails(exception));
  }

  public NervErrorResponse from(
      MethodArgumentTypeMismatchException exception,
      HttpServletRequest request) {

    return build(
        NativeNervErrorCodes.BAD_REQUEST,
        NativeNervErrorCodes.BAD_REQUEST.message(),
        request,
        methodArgumentTypeMismatchDetails(exception));
  }

  public NervErrorResponse from(
      HttpMessageNotReadableException exception,
      HttpServletRequest request) {

    return build(
        NativeNervErrorCodes.BAD_REQUEST,
        NativeNervErrorCodes.BAD_REQUEST.message(),
        request,
        exceptionDetails(exception));
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
      HttpMediaTypeNotSupportedException exception,
      HttpServletRequest request) {

    Map<String, Object> details = exceptionDetails(exception);

    if (settings.includeDetails()) {
      details.put("contentType", exception.getContentType());
      details.put("supportedMediaTypes", exception.getSupportedMediaTypes());
    }

    return build(
        NativeNervErrorCodes.UNSUPPORTED_MEDIA_TYPE,
        NativeNervErrorCodes.UNSUPPORTED_MEDIA_TYPE.message(),
        request,
        details);
  }

  public NervErrorResponse from(
      HttpMediaTypeNotAcceptableException exception,
      HttpServletRequest request) {

    Map<String, Object> details = exceptionDetails(exception);

    if (settings.includeDetails()) {
      details.put("supportedMediaTypes", exception.getSupportedMediaTypes());
    }

    return build(
        NativeNervErrorCodes.NOT_ACCEPTABLE,
        NativeNervErrorCodes.NOT_ACCEPTABLE.message(),
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

  public NervErrorResponse from(AuthenticationException exception, HttpServletRequest request) {

    String authExceptionMessage = "Authentication error";
    if (exception instanceof BadCredentialsException) {
      authExceptionMessage = "Bad credentials";
    }

    if (exception instanceof InsufficientAuthenticationException) {
      authExceptionMessage = "Insufficient authentication";
    }

    if (exception instanceof LockedException) {
      authExceptionMessage = "Account locked";
    }

    if (exception instanceof DisabledException) {
      authExceptionMessage = "Account disabled";
    }

    if (exception instanceof AccountExpiredException) {
      authExceptionMessage = "Account expired";
    }

    if (exception instanceof CredentialsExpiredException) {
      authExceptionMessage = "Credentials expired";
    }

    return build(
        NativeNervErrorCodes.UNAUTHORIZED,
        authExceptionMessage,
        request,
        exceptionDetails(exception)
    );
  }

  public NervErrorResponse from(AccessDeniedException exception, HttpServletRequest request) {
    return build(
        NativeNervErrorCodes.UNAUTHORIZED,
        NativeNervErrorCodes.UNAUTHORIZED.message(),
        request,
        exceptionDetails(exception)
    );
  }

  // ========================================
  // JPA / Spring Data exception mappings
  // ========================================

  /**
   * Handles unique constraint violations (duplicate key errors).
   * Extracts constraint name and affected fields when possible.
   */
  public NervErrorResponse from(
      DataIntegrityViolationException exception,
      HttpServletRequest request) {

    Map<String, Object> details = exceptionDetails(exception);

    if (settings.includeDetails()) {
      String constraintName = extractConstraintName(exception);
      if (constraintName != null) {
        details.put("constraint", constraintName);
      }

      String rootCauseMessage = getRootCauseMessage(exception);
      if (rootCauseMessage != null) {
        details.put("reason", rootCauseMessage);
      }
    }

    return build(
        NativeNervErrorCodes.CONFLICT,
        "A resource with the same unique identifier already exists",
        request,
        details);
  }

  /**
   * Handles optimistic locking failures from Spring ORM.
   * Includes entity type and identifier when available.
   */
  public NervErrorResponse from(
      ObjectOptimisticLockingFailureException exception,
      HttpServletRequest request) {

    Map<String, Object> details = exceptionDetails(exception);

    if (settings.includeDetails()) {
      if (exception.getPersistentClassName() != null) {
        details.put("entityType", exception.getPersistentClassName());
      }
      if (exception.getIdentifier() != null) {
        details.put("identifier", exception.getIdentifier().toString());
      }
    }

    return build(
        NativeNervErrorCodes.OPTIMISTIC_LOCK_CONFLICT,
        "The resource was modified by another transaction. Please refresh and retry.",
        request,
        details);
  }

  /**
   * Handles JPA optimistic lock exceptions.
   */
  public NervErrorResponse from(
      OptimisticLockException exception,
      HttpServletRequest request) {

    Map<String, Object> details = exceptionDetails(exception);

    if (settings.includeDetails()) {
      Object entity = exception.getEntity();
      if (entity != null) {
        details.put("entityType", entity.getClass().getSimpleName());
      }
    }

    return build(
        NativeNervErrorCodes.OPTIMISTIC_LOCK_CONFLICT,
        "The resource was modified by another transaction. Please refresh and retry.",
        request,
        details);
  }

  /**
   * Handles queries that return more results than expected.
   * Returns 500 as this typically indicates a bug.
   */
  public NervErrorResponse from(
      IncorrectResultSizeDataAccessException exception,
      HttpServletRequest request) {

    // Don't expose internal details - this is typically a bug
    Map<String, Object> details = new LinkedHashMap<>();

    if (settings.includeCause() && exception.getCause() != null) {
      details.put("cause", exception.getCause().getClass().getName());
    }

    if (settings.includeStackTrace()) {
      details.put("stackTrace", stackTrace(exception));
    }

    return build(
        NativeNervErrorCodes.AMBIGUOUS_RESULT,
        NativeNervErrorCodes.AMBIGUOUS_RESULT.message(),
        request,
        details);
  }

  /**
   * Handles queries that return no results when one was expected.
   */
  public NervErrorResponse from(
      EmptyResultDataAccessException exception,
      HttpServletRequest request) {

    Map<String, Object> details = exceptionDetails(exception);

    return build(
        NativeNervErrorCodes.RESOURCE_NOT_FOUND,
        "The requested resource was not found",
        request,
        details);
  }

  /**
   * Handles JPA NoResultException when getSingleResult returns no results.
   */
  public NervErrorResponse from(
      NoResultException exception,
      HttpServletRequest request) {

    Map<String, Object> details = exceptionDetails(exception);

    return build(
        NativeNervErrorCodes.RESOURCE_NOT_FOUND,
        "The requested resource was not found",
        request,
        details);
  }

  /**
   * Handles pessimistic locking failures (row locked by another transaction).
   */
  public NervErrorResponse from(
      PessimisticLockingFailureException exception,
      HttpServletRequest request) {

    Map<String, Object> details = exceptionDetails(exception);

    return build(
        NativeNervErrorCodes.LOCK_TIMEOUT,
        "The resource is currently locked by another operation. Please try again later.",
        request,
        details);
  }

  /**
   * Handles JPA pessimistic lock exceptions.
   */
  public NervErrorResponse from(
      PessimisticLockException exception,
      HttpServletRequest request) {

    Map<String, Object> details = exceptionDetails(exception);

    if (settings.includeDetails()) {
      Object entity = exception.getEntity();
      if (entity != null) {
        details.put("entityType", entity.getClass().getSimpleName());
      }
    }

    return build(
        NativeNervErrorCodes.LOCK_TIMEOUT,
        "The resource is currently locked by another operation. Please try again later.",
        request,
        details);
  }

  /**
   * Handles Hibernate Validator ConstraintViolationException (bean validation).
   * Note: This is different from jakarta.validation.ConstraintViolationException.
   */
  public NervErrorResponse from(
      org.hibernate.exception.ConstraintViolationException exception,
      HttpServletRequest request) {

    Map<String, Object> details = exceptionDetails(exception);

    if (settings.includeDetails()) {
      if (exception.getConstraintName() != null) {
        details.put("constraint", exception.getConstraintName());
      }
      if (exception.getSQLException() != null) {
        details.put("sqlState", exception.getSQLException().getSQLState());
      }
    }

    return build(
        NativeNervErrorCodes.CONSTRAINT_VIOLATION,
        "Data validation failed due to constraint violation",
        request,
        details);
  }

  // ========================================
  // Helper methods for JPA exceptions
  // ========================================

  /**
   * Extracts the constraint name from a DataIntegrityViolationException.
   * Attempts to parse from both the exception hierarchy and error messages.
   */
  private String extractConstraintName(DataIntegrityViolationException exception) {
    Throwable cause = exception.getCause();

    // Check if it's a Hibernate ConstraintViolationException
    if (cause instanceof org.hibernate.exception.ConstraintViolationException hibernateEx) {
      return hibernateEx.getConstraintName();
    }

    // Try to extract from message using common patterns
    String message = exception.getMessage();
    if (message != null) {
      // PostgreSQL pattern: "violates unique constraint \"constraint_name\""
      Pattern pgPattern = Pattern.compile("violates unique constraint \"([^\"]+)\"");
      Matcher pgMatcher = pgPattern.matcher(message);
      if (pgMatcher.find()) {
        return pgMatcher.group(1);
      }

      // MySQL pattern: "Duplicate entry ... for key 'constraint_name'"
      Pattern mysqlPattern = Pattern.compile("for key '([^']+)'");
      Matcher mysqlMatcher = mysqlPattern.matcher(message);
      if (mysqlMatcher.find()) {
        return mysqlMatcher.group(1);
      }

      // H2/Generic pattern: "Unique index or primary key violation: \"CONSTRAINT_NAME\""
      Pattern h2Pattern = Pattern.compile("Unique index or primary key violation: \"([^\"]+)\"");
      Matcher h2Matcher = h2Pattern.matcher(message);
      if (h2Matcher.find()) {
        return h2Matcher.group(1);
      }
    }

    return null;
  }

  /**
   * Gets the root cause message from an exception chain.
   */
  private String getRootCauseMessage(Throwable throwable) {
    Throwable rootCause = throwable;
    while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
      rootCause = rootCause.getCause();
    }
    return rootCause.getMessage();
  }

  public NervErrorResponse build(
      @NotNull NervErrorCode errorCode,
      @NotNull String message,
      HttpServletRequest request,
      Map<String, Object> details) {
    return build(null, errorCode, message, request, details);
  }

  public NervErrorResponse build(
      @NotNull NervOrigin origin,
      @NotNull NervErrorCode errorCode,
      @NotNull String message,
      HttpServletRequest request,
      Map<String, Object> details) {

    return NervErrorResponse.builder()
        .code(errorCode.code())
        .message(message)
        .status(errorCode.status())
        .retryable(errorCode.retryable())
        .category(errorCode.category())
        .traceId(traceContextResolver.current().traceId())
        .spanId(traceContextResolver.current().spanId())
        .path(request.getRequestURI())
        .timestamp(Instant.now())
        .details(details)
        .origin(origin == null ? originResolver.resolve() : origin)
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

  private Map<String, Object> handlerMethodValidationDetails(
      HandlerMethodValidationException exception) {

    Map<String, Object> details = exceptionDetails(exception);

    if (settings.includeDetails()) {
      Map<String, String> errors = new LinkedHashMap<>();

      exception.getParameterValidationResults().forEach(result ->
          result.getResolvableErrors().forEach(error ->
              errors.putIfAbsent(
                  result.getMethodParameter().getParameterName(),
                  error.getDefaultMessage() == null
                      ? "Invalid value"
                      : error.getDefaultMessage())));
      details.put("errors", errors);
    }

    return details;
  }

  private Map<String, Object> missingServletRequestParameterDetails(
      MissingServletRequestParameterException exception) {

    Map<String, Object> details = exceptionDetails(exception);

    if (settings.includeDetails()) {
      details.put("parameter", exception.getParameterName());
      details.put("expectedType", exception.getParameterType());
    }

    return details;
  }

  private Map<String, Object> missingRequestHeaderDetails(
      MissingRequestHeaderException exception) {

    Map<String, Object> details = exceptionDetails(exception);

    if (settings.includeDetails()) {
      details.put("header", exception.getHeaderName());
    }

    return details;
  }

  private Map<String, Object> missingPathVariableDetails(
      MissingPathVariableException exception) {

    Map<String, Object> details = exceptionDetails(exception);

    if (settings.includeDetails()) {
      details.put("variable", exception.getVariableName());
    }

    return details;
  }

  private Map<String, Object> methodArgumentTypeMismatchDetails(
      MethodArgumentTypeMismatchException exception) {

    Map<String, Object> details = exceptionDetails(exception);

    if (settings.includeDetails()) {
      details.put("parameter", exception.getName());
      details.put("value", exception.getValue());

      if (exception.getRequiredType() != null) {
        details.put("expectedType", exception.getRequiredType().getSimpleName());
      }
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

  public Map<String, Object> exceptionDetails(Exception exception) {

    Map<String, Object> details = new LinkedHashMap<>();

    if (settings.includeCause() && exception.getCause() != null) {
      details.put("cause", exception.getCause().getClass().getName());
    }

    if (settings.includeStackTrace()) {
      details.put("stackTrace", stackTrace(exception));
    }

    return details;
  }

  private String stackTrace(Throwable throwable) {

    StringWriter stringWriter = new StringWriter();

    throwable.printStackTrace(new PrintWriter(stringWriter));

    return stringWriter.toString();
  }
}
