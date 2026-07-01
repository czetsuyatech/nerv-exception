package com.czetsuyatech.nerv.exception.kafka;

import com.czetsuyatech.nerv.exception.event.NervErrorEvent;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Headers;
import org.springframework.kafka.core.KafkaTemplate;

@RequiredArgsConstructor
public class NervKafkaDlqPublisher {

  private final KafkaTemplate<String, NervErrorEvent> kafkaTemplate;
  private final NervKafkaHeaderMapper headerMapper;

  public CompletableFuture<Void> publish(
      String dlqTopic,
      String key,
      NervErrorEvent event) {

    ProducerRecord<String, NervErrorEvent> record =
        new ProducerRecord<>(dlqTopic, key, event);

    Headers headers = headerMapper.from(event);

    headers.forEach(header ->
        record.headers().add(header));

    return kafkaTemplate
        .send(record)
        .thenApply(result -> null);
  }
}
