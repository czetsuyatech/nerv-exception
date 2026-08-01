package com.czetsuyatech.nerv.exception.jpa;

import com.czetsuyatech.nerv.exception.core.model.NervErrorResponse;
import com.czetsuyatech.nerv.exception.web.NervErrorResponseMapper;
import com.czetsuyatech.nerv.exception.web.NervExceptionHandler;
import jakarta.persistence.NoResultException;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PessimisticLockException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Handles JPA / Spring Data / Hibernate exceptions and maps them to {@link NervErrorResponse}.
 *
 * <p>Only registered when JPA-related classes (e.g. {@code DataAccessException}) are present on
 * the classpath — see {@code NervExceptionAutoConfiguration.WebConfiguration.JpaConfiguration}. This keeps
 * {@code nerv-exception-web} free of any JPA/Hibernate dependency.
 */
@RestControllerAdvice
@RequiredArgsConstructor
public class NervJpaExceptionHandler implements NervExceptionHandler {

  private final NervErrorResponseMapper errorResponseMapper;

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
}
