package com.czetsuyatech.nerv.exception.web;

import com.czetsuyatech.nerv.exception.core.NervErrorHeaders;
import com.czetsuyatech.nerv.exception.core.NervException;
import com.czetsuyatech.nerv.exception.core.model.NervErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
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

  @ExceptionHandler(HandlerMethodValidationException.class)
  public ResponseEntity<NervErrorResponse> handleHandlerMethodValidationException(
      HandlerMethodValidationException exception,
      HttpServletRequest request) {

    return build(errorResponseMapper.from(exception, request));
  }

  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<NervErrorResponse> handleMissingServletRequestParameterException(
      MissingServletRequestParameterException exception,
      HttpServletRequest request) {

    return build(errorResponseMapper.from(exception, request));
  }

  @ExceptionHandler(MissingRequestHeaderException.class)
  public ResponseEntity<NervErrorResponse> handleMissingRequestHeaderException(
      MissingRequestHeaderException exception,
      HttpServletRequest request) {

    return build(errorResponseMapper.from(exception, request));
  }

  @ExceptionHandler(MissingPathVariableException.class)
  public ResponseEntity<NervErrorResponse> handleMissingPathVariableException(
      MissingPathVariableException exception,
      HttpServletRequest request) {

    return build(errorResponseMapper.from(exception, request));
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<NervErrorResponse> handleMethodArgumentTypeMismatchException(
      MethodArgumentTypeMismatchException exception,
      HttpServletRequest request) {

    return build(errorResponseMapper.from(exception, request));
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<NervErrorResponse> handleHttpMessageNotReadableException(
      HttpMessageNotReadableException exception,
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

  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  public ResponseEntity<NervErrorResponse> handleHttpMediaTypeNotSupportedException(
      HttpMediaTypeNotSupportedException exception,
      HttpServletRequest request) {

    return build(errorResponseMapper.from(exception, request));
  }

  @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
  public ResponseEntity<NervErrorResponse> handleHttpMediaTypeNotAcceptableException(
      HttpMediaTypeNotAcceptableException exception,
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
