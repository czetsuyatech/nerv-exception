package com.czetsuyatech.nerv.exception.web;

import com.czetsuyatech.nerv.exception.core.NervException;
import com.czetsuyatech.nerv.exception.core.model.NervErrorResponse;
import jakarta.persistence.NoResultException;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PessimisticLockException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.nio.file.AccessDeniedException;
import javax.security.sasl.AuthenticationException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
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
public class DefaultNervExceptionHandler implements NervExceptionHandler {

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

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<NervErrorResponse> handleAccessDeniedException(
      AccessDeniedException exception,
      HttpServletRequest request) {

    return build(errorResponseMapper.from(exception, request));
  }

  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<NervErrorResponse> handleAuthenticationException(
      AuthenticationException exception,
      HttpServletRequest request) {

    return build(errorResponseMapper.from(exception, request));
  }

  // ========================================
  // JPA / Spring Data exception handlers
  // ========================================

  /**
   * Handles unique constraint violations (duplicate key errors). Returns 409 Conflict.
   */
  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<NervErrorResponse> handleDataIntegrityViolationException(
      DataIntegrityViolationException exception,
      HttpServletRequest request) {

    return build(errorResponseMapper.from(exception, request));
  }

  /**
   * Handles optimistic locking failures from Spring ORM. Returns 409 Conflict (retryable).
   */
  @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
  public ResponseEntity<NervErrorResponse> handleObjectOptimisticLockingFailureException(
      ObjectOptimisticLockingFailureException exception,
      HttpServletRequest request) {

    return build(errorResponseMapper.from(exception, request));
  }

  /**
   * Handles JPA optimistic lock exceptions. Returns 409 Conflict (retryable).
   */
  @ExceptionHandler(OptimisticLockException.class)
  public ResponseEntity<NervErrorResponse> handleOptimisticLockException(
      OptimisticLockException exception,
      HttpServletRequest request) {

    return build(errorResponseMapper.from(exception, request));
  }

  /**
   * Handles queries that return more results than expected. Returns 500 Internal Server Error (this is typically a
   * bug).
   */
  @ExceptionHandler(IncorrectResultSizeDataAccessException.class)
  public ResponseEntity<NervErrorResponse> handleIncorrectResultSizeDataAccessException(
      IncorrectResultSizeDataAccessException exception,
      HttpServletRequest request) {

    return build(errorResponseMapper.from(exception, request));
  }

  /**
   * Handles queries that return no results when one was expected. Returns 404 Not Found.
   */
  @ExceptionHandler(EmptyResultDataAccessException.class)
  public ResponseEntity<NervErrorResponse> handleEmptyResultDataAccessException(
      EmptyResultDataAccessException exception,
      HttpServletRequest request) {

    return build(errorResponseMapper.from(exception, request));
  }

  /**
   * Handles JPA NoResultException when getSingleResult returns no results. Returns 404 Not Found.
   */
  @ExceptionHandler(NoResultException.class)
  public ResponseEntity<NervErrorResponse> handleNoResultException(
      NoResultException exception,
      HttpServletRequest request) {

    return build(errorResponseMapper.from(exception, request));
  }

  /**
   * Handles pessimistic locking failures (row locked by another transaction). Returns 503 Service Unavailable
   * (retryable).
   */
  @ExceptionHandler(PessimisticLockingFailureException.class)
  public ResponseEntity<NervErrorResponse> handlePessimisticLockingFailureException(
      PessimisticLockingFailureException exception,
      HttpServletRequest request) {

    return build(errorResponseMapper.from(exception, request));
  }

  /**
   * Handles JPA pessimistic lock exceptions. Returns 503 Service Unavailable (retryable).
   */
  @ExceptionHandler(PessimisticLockException.class)
  public ResponseEntity<NervErrorResponse> handlePessimisticLockException(
      PessimisticLockException exception,
      HttpServletRequest request) {

    return build(errorResponseMapper.from(exception, request));
  }

  /**
   * Handles Hibernate ConstraintViolationException (bean validation from Hibernate). Returns 400 Bad Request.
   */
  @ExceptionHandler(org.hibernate.exception.ConstraintViolationException.class)
  public ResponseEntity<NervErrorResponse> handleHibernateConstraintViolationException(
      org.hibernate.exception.ConstraintViolationException exception,
      HttpServletRequest request) {

    return build(errorResponseMapper.from(exception, request));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<NervErrorResponse> handleException(
      Exception exception,
      HttpServletRequest request) {

    return build(errorResponseMapper.from(exception, request));
  }
}
