package com.czetsuyatech.nerv.exception.web;

import com.czetsuyatech.nerv.exception.core.NervErrorHeaders;
import com.czetsuyatech.nerv.exception.core.NervException;
import com.czetsuyatech.nerv.exception.core.model.NervErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

@RestControllerAdvice
@RequiredArgsConstructor
public class NervExceptionHandler {

  private final NervErrorResponseMapper errorResponseMapper;

  @ExceptionHandler(NervException.class)
  public ResponseEntity<NervErrorResponse> handleNervException(
      NervException exception,
      HttpServletRequest request) {

    return build(errorResponseMapper.from(exception, request));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<NervErrorResponse> handleMethodArgumentNotValidException(
      MethodArgumentNotValidException exception,
      HttpServletRequest request) {

    return build(errorResponseMapper.from(exception, request));
  }

  @ExceptionHandler(BindException.class)
  public ResponseEntity<NervErrorResponse> handleBindException(
      BindException exception,
      HttpServletRequest request) {

    return build(errorResponseMapper.from(exception, request));
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<NervErrorResponse> handleConstraintViolationException(
      ConstraintViolationException exception,
      HttpServletRequest request) {

    return build(errorResponseMapper.from(exception, request));
  }

  @ExceptionHandler(NoHandlerFoundException.class)
  public ResponseEntity<NervErrorResponse> handleNoHandlerFoundException(
      NoHandlerFoundException exception,
      HttpServletRequest request) {

    return build(errorResponseMapper.from(exception, request));
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<NervErrorResponse> handleHttpRequestMethodNotSupportedException(
      HttpRequestMethodNotSupportedException exception,
      HttpServletRequest request) {

    return build(errorResponseMapper.from(exception, request));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<NervErrorResponse> handleException(
      Exception exception,
      HttpServletRequest request) {

    return build(errorResponseMapper.from(exception, request));
  }

  private ResponseEntity<NervErrorResponse> build(NervErrorResponse response) {
    ResponseEntity.BodyBuilder builder = ResponseEntity
        .status(response.status())
        .header(NervErrorHeaders.ERROR_CODE, response.code())
        .header(NervErrorHeaders.ERROR_CATEGORY, response.category())
        .header(NervErrorHeaders.ERROR_RETRYABLE, String.valueOf(response.retryable()));

    addHeader(builder, NervErrorHeaders.TRACE_ID, response.traceId());
    addHeader(builder, NervErrorHeaders.SPAN_ID, response.spanId());

    return builder.body(response);
  }

  private void addHeader(
      ResponseEntity.BodyBuilder builder,
      String name,
      String value) {

    if (value != null && !value.isBlank()) {
      builder.header(name, value);
    }
  }
}
