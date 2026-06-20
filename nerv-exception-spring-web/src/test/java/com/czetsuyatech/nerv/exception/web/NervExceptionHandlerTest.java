package com.czetsuyatech.nerv.exception.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.czetsuyatech.nerv.exception.core.NervException;
import com.czetsuyatech.nerv.exception.core.code.NativeNervErrorCodes;
import jakarta.validation.ConstraintViolationException;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

class NervExceptionHandlerTest {

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {

    NervExceptionSettings settings = new NervExceptionSettings(
        true,
        false,
        false,
        false);
    NervTraceContextResolver traceContextResolver = new NoOpNervTraceContextResolver();

    NervErrorResponseMapper mapper = new NervErrorResponseMapper(settings, traceContextResolver);

    mockMvc = MockMvcBuilders
        .standaloneSetup(new TestController())
        .setControllerAdvice(new NervExceptionHandler(mapper))
        .build();
  }

  @Test
  void shouldHandleNervException() throws Exception {

    mockMvc.perform(get("/nerv"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("CONFLICT"))
        .andExpect(jsonPath("$.message").value("Duplicate resource"))
        .andExpect(jsonPath("$.status").value(409))
        .andExpect(jsonPath("$.path").value("/nerv"));
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
  void shouldHandleUnhandledException() throws Exception {

    mockMvc.perform(get("/error"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.code")
            .value(NativeNervErrorCodes.INTERNAL_SERVER_ERROR.code()))
        .andExpect(jsonPath("$.message")
            .value(NativeNervErrorCodes.INTERNAL_SERVER_ERROR.message()))
        .andExpect(jsonPath("$.status").value(500))
        .andExpect(jsonPath("$.path").value("/error"));
  }

  @RestController
  static class TestController {

    @GetMapping("/nerv")
    String nerv() {
      throw new NervException(
          NativeNervErrorCodes.CONFLICT,
          "Duplicate resource");
    }

    @GetMapping("/constraint")
    String constraint() {
      throw new ConstraintViolationException("Constraint violation", Set.of());
    }

    @GetMapping("/error")
    String error() {
      throw new IllegalStateException("boom");
    }
  }
}
