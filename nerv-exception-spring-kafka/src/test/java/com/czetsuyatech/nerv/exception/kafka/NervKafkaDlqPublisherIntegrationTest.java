package com.czetsuyatech.nerv.exception.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.czetsuyatech.nerv.exception.event.NervErrorEvent;
import com.czetsuyatech.nerv.exception.event.NervEventErrorHeaders;
import java.util.concurrent.CompletableFuture;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Headers;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.springframework.kafka.core.KafkaTemplate;

class NervKafkaDlqPublisherIntegrationTest {

  private final KafkaTemplate<String, NervErrorEvent> kafkaTemplate = mock(KafkaTemplate.class);
  private final NervKafkaHeaderMapper headerMapper = new NervKafkaHeaderMapper();

  private final NervKafkaDlqPublisher publisher =
      new NervKafkaDlqPublisher(kafkaTemplate, headerMapper);

  @Test
  void shouldPublishErrorEventToDlqWithHeaders() {
    NervErrorEvent event = NervErrorEvent.builder()
        .code("PAYMENT_TIMEOUT")
        .message("Payment timed out")
        .category("INTEGRATION")
        .retryable(true)
        .traceId("trace-123")
        .spanId("span-456")
        .source("payment-service")
        .build();

    when(kafkaTemplate.send(ArgumentMatchers.any(ProducerRecord.class)))
        .thenReturn(CompletableFuture.completedFuture(null));

    publisher.publish("payments.dlq", "payment-1", event).join();

    ArgumentCaptor<ProducerRecord<String, NervErrorEvent>> captor =
        ArgumentCaptor.forClass(ProducerRecord.class);

    verify(kafkaTemplate).send(captor.capture());

    ProducerRecord<String, NervErrorEvent> record = captor.getValue();

    assertThat(record.topic()).isEqualTo("payments.dlq");
    assertThat(record.key()).isEqualTo("payment-1");
    assertThat(record.topic()).isEqualTo("payments.dlq");
    assertThat(record.value()).isEqualTo(event);

    Headers headers = record.headers();

    assertThat(headers.lastHeader(NervEventErrorHeaders.ERROR_CODE)).isNotNull();
    assertThat(headers.lastHeader(NervEventErrorHeaders.ERROR_CATEGORY)).isNotNull();
    assertThat(headers.lastHeader(NervEventErrorHeaders.TRACE_ID)).isNotNull();
    assertThat(headers.lastHeader(NervEventErrorHeaders.SPAN_ID)).isNotNull();
  }
}
