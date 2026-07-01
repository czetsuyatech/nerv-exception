package com.czetsuyatech.nerv.exception.kafka;

import com.czetsuyatech.nerv.exception.event.NervErrorEvent;
import com.czetsuyatech.nerv.exception.event.NervEventHeaderMapper;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;

public class NervKafkaHeaderMapper {

  public Headers from(NervErrorEvent event) {

    Headers headers = new RecordHeaders();

    for (Map.Entry<String, String> entry : NervEventHeaderMapper.headers(event).entrySet()) {
      headers.add(
          entry.getKey(),
          entry.getValue().getBytes(StandardCharsets.UTF_8));
    }

    return headers;
  }
}
