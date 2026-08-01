package com.czetsuyatech.nerv.exception.jpa;

import java.lang.reflect.Method;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

  @GetMapping("/data-integrity-violation")
  String dataIntegrityViolation() {
    throw new org.springframework.dao.DataIntegrityViolationException(
        "Duplicate entry 'john@example.com' for key 'users.email'");
  }

  @GetMapping("/object-optimistic-lock")
  String objectOptimisticLock() {
    throw new org.springframework.orm.ObjectOptimisticLockingFailureException(
        "com.example.User", 42L);
  }

  @GetMapping("/jpa-optimistic-lock")
  String jpaOptimisticLock() {
    throw new jakarta.persistence.OptimisticLockException("Entity version mismatch");
  }

  @GetMapping("/incorrect-result-size")
  String incorrectResultSize() {
    throw new org.springframework.dao.IncorrectResultSizeDataAccessException(1, 3);
  }

  @GetMapping("/empty-result")
  String emptyResult() {
    throw new org.springframework.dao.EmptyResultDataAccessException(1);
  }

  @GetMapping("/jpa-no-result")
  String jpaNoResult() {
    throw new jakarta.persistence.NoResultException("No entity found for query");
  }

  @GetMapping("/pessimistic-lock-failure")
  String pessimisticLockFailure() {
    throw new org.springframework.dao.PessimisticLockingFailureException(
        "Could not acquire lock on row");
  }

  @GetMapping("/jpa-pessimistic-lock")
  String jpaPessimisticLock() {
    throw new jakarta.persistence.PessimisticLockException("Lock timeout");
  }

  @GetMapping("/hibernate-constraint-violation")
  String hibernateConstraintViolation() {
    throw new org.hibernate.exception.ConstraintViolationException(
        "could not execute statement", null, "uk_users_email");
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
