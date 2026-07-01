package com.czetsuyatech.nerv.exception.autoconfigure;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "nerv.exception")
public class NervExceptionProperties {

  /**
   * Enables or disables Nerv exception handling.
   */
  private boolean enabled = true;

  /**
   * Include exception details map in the response.
   */
  private boolean includeDetails = true;

  /**
   * Expose original exception message for non-Nerv exceptions.
   */
  private boolean exposeInternalMessage = false;

  /**
   * Include cause class name in details.
   */
  private boolean includeCause = false;

  /**
   * Include stack trace in details.
   */
  private boolean includeStackTrace = false;

  private Kafka kafka = new Kafka();

  @Getter
  @Setter
  public static class Kafka {

    private boolean enabled = true;
    private String source = "application";
    private String dlqTopicSuffix = ".DLQ";
  }
}
