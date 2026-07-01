package com.czetsuyatech.nerv.exception.kafka;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.czetsuyatech.nerv.exception.core.NervException;
import com.czetsuyatech.nerv.exception.core.code.NativeNervErrorCodes;
import com.czetsuyatech.nerv.exception.event.DefaultNervErrorEventMapper;
import com.czetsuyatech.nerv.exception.event.NervErrorEvent;
import com.czetsuyatech.nerv.exception.event.NoOpNervEventTraceContextResolver;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;

class NervKafkaErrorHandlerTest {

  private final NervKafkaDlqPublisher dlqPublisher = mock(NervKafkaDlqPublisher.class);

  private final NervKafkaErrorHandler errorHandler =
      new NervKafkaErrorHandler(
          new DefaultNervErrorEventMapper(new NoOpNervEventTraceContextResolver()),
          dlqPublisher,
          "test-service",
          ".dlq"
      );

  @Test
  void shouldMapExceptionAndPublishToDlq() {
    ConsumerRecord<String, String> record =
        new ConsumerRecord<>("payments", 0, 10L, "payment-1", "payload");

    var exception =
        new NervException(NativeNervErrorCodes.INTERNAL_SERVER_ERROR, "Processing failed");

    errorHandler.handle(record, exception);

    verify(dlqPublisher).publish(
        eq("payments.dlq"),
        eq("payment-1"),
        argThat(event ->
            event.code().equals(NativeNervErrorCodes.INTERNAL_SERVER_ERROR.code())
                && event.message().equals("Processing failed")
                && event.source().equals("test-service")
        )
    );
  }

  @Test
  void shouldPublishFailedRecordToDlq() {
    ConsumerRecord<String, String> record =
        new ConsumerRecord<>("payments", 0, 10L, "payment-1", "payload");

    NervException exception =
        new NervException(
            NativeNervErrorCodes.INTERNAL_SERVER_ERROR,
            "Processing failed"
        );

    errorHandler.handle(record, exception);

    verify(dlqPublisher).publish(
        eq("payments.dlq"),
        eq("payment-1"),
        any(NervErrorEvent.class)
    );
  }
}
