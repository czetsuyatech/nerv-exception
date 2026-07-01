package com.czetsuyatech.nerv.exception.kafka;

import com.czetsuyatech.nerv.exception.core.NervException;
import com.czetsuyatech.nerv.exception.core.code.NativeNervErrorCodes;
import com.czetsuyatech.nerv.exception.event.NervErrorEvent;
import com.czetsuyatech.nerv.exception.event.NervErrorEventMapper;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerRecord;

public class NervKafkaErrorHandler {

  private final NervErrorEventMapper errorEventMapper;
  private final NervKafkaDlqPublisher dlqPublisher;
  private final String source;
  private final String dlqTopicSuffix;

  public NervKafkaErrorHandler(
      NervErrorEventMapper errorEventMapper,
      NervKafkaDlqPublisher dlqPublisher,
      String source,
      String dlqTopicSuffix) {

    this.errorEventMapper = errorEventMapper;
    this.dlqPublisher = dlqPublisher;
    this.source = source;
    this.dlqTopicSuffix = dlqTopicSuffix;
  }

  public void handle(
      ConsumerRecord<String, ?> record,
      Exception exception) {

    NervException nervException = toNervException(exception);

    NervErrorEvent event = errorEventMapper.from(
        nervException,
        source,
        record.topic(),
        record.key());

    dlqPublisher.publish(
        record.topic() + dlqTopicSuffix,
        record.key(),
        event);
  }

  private NervException toNervException(Exception exception) {

    if (exception instanceof NervException nervException) {
      return nervException;
    }

    return new NervException(
        NativeNervErrorCodes.INTERNAL_SERVER_ERROR,
        exception.getMessage(),
        Map.of("exceptionType", exception.getClass().getName()));
  }
}
