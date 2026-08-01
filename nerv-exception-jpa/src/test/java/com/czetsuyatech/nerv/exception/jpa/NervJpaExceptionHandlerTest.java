package com.czetsuyatech.nerv.exception.jpa;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.czetsuyatech.nerv.exception.core.code.NativeNervErrorCodes;
import com.czetsuyatech.nerv.exception.core.origin.NoOpNervOriginResolver;
import com.czetsuyatech.nerv.exception.trace.NervTraceContextResolver;
import com.czetsuyatech.nerv.exception.trace.NoOpNervTraceContextResolver;
import com.czetsuyatech.nerv.exception.web.DefaultNervExceptionHandler;
import com.czetsuyatech.nerv.exception.web.NervErrorResponseMapper;
import com.czetsuyatech.nerv.exception.web.NervExceptionSettings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

public class NervJpaExceptionHandlerTest {

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
        .setControllerAdvice(
            new NervJpaExceptionHandler(mapper),
            new DefaultNervExceptionHandler(mapper))
        .build();
  }

  // ---------------------------------------------------------------------
  // JPA / Spring Data
  // ---------------------------------------------------------------------

  @Test
  void shouldHandleDataIntegrityViolationException() throws Exception {

    mockMvc.perform(get("/data-integrity-violation"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value(NativeNervErrorCodes.CONFLICT.code()))
        .andExpect(jsonPath("$.status").value(409))
        .andExpect(jsonPath("$.message").value("A resource with the same unique identifier already exists"));
  }

  @Test
  void shouldHandleObjectOptimisticLockingFailureException() throws Exception {

    mockMvc.perform(get("/object-optimistic-lock"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value(NativeNervErrorCodes.OPTIMISTIC_LOCK_CONFLICT.code()))
        .andExpect(jsonPath("$.status").value(409))
        .andExpect(jsonPath("$.retryable").value(true))
        .andExpect(jsonPath("$.category").value("DATA"));
  }

  @Test
  void shouldHandleJpaOptimisticLockException() throws Exception {

    mockMvc.perform(get("/jpa-optimistic-lock"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value(NativeNervErrorCodes.OPTIMISTIC_LOCK_CONFLICT.code()))
        .andExpect(jsonPath("$.status").value(409))
        .andExpect(jsonPath("$.retryable").value(true));
  }

  @Test
  void shouldHandleIncorrectResultSizeDataAccessException() throws Exception {

    mockMvc.perform(get("/incorrect-result-size"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.code").value(NativeNervErrorCodes.AMBIGUOUS_RESULT.code()))
        .andExpect(jsonPath("$.status").value(500))
        .andExpect(jsonPath("$.retryable").value(false))
        .andExpect(jsonPath("$.category").value("SYSTEM"));
  }

  @Test
  void shouldHandleEmptyResultDataAccessException() throws Exception {

    mockMvc.perform(get("/empty-result"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value(NativeNervErrorCodes.RESOURCE_NOT_FOUND.code()))
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.message").value("The requested resource was not found"));
  }

  @Test
  void shouldHandleNoResultException() throws Exception {

    mockMvc.perform(get("/jpa-no-result"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value(NativeNervErrorCodes.RESOURCE_NOT_FOUND.code()))
        .andExpect(jsonPath("$.status").value(404));
  }

  @Test
  void shouldHandlePessimisticLockingFailureException() throws Exception {

    mockMvc.perform(get("/pessimistic-lock-failure"))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.code").value(NativeNervErrorCodes.LOCK_TIMEOUT.code()))
        .andExpect(jsonPath("$.status").value(503))
        .andExpect(jsonPath("$.retryable").value(true))
        .andExpect(jsonPath("$.category").value("DATA"));
  }

  @Test
  void shouldHandleJpaPessimisticLockException() throws Exception {

    mockMvc.perform(get("/jpa-pessimistic-lock"))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.code").value(NativeNervErrorCodes.LOCK_TIMEOUT.code()))
        .andExpect(jsonPath("$.status").value(503))
        .andExpect(jsonPath("$.retryable").value(true));
  }

  @Test
  void shouldHandleHibernateConstraintViolationException() throws Exception {

    mockMvc.perform(get("/hibernate-constraint-violation"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(NativeNervErrorCodes.CONSTRAINT_VIOLATION.code()))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.message").value("Data validation failed due to constraint violation"));
  }
}
