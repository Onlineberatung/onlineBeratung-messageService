package de.caritas.cob.messageservice.api.exception;

import static java.util.Objects.nonNull;

import java.util.function.Consumer;

/**
 * Representation of a custom exception for status code responses with logging consumers.
 */
public abstract class CustomLoggableResponseException extends RuntimeException {

  private Consumer<Exception> exceptionLoggingMethod;
  private Consumer<String> messageLoggingMethod;

  /**
   * Creates a Exception without custom messages, the logging method will only log this Exception
   * stacktrace.
   *
   * @param exceptionLoggingMethod the logging method
   */
  CustomLoggableResponseException(Consumer<Exception> exceptionLoggingMethod) {
    super();
    this.exceptionLoggingMethod = exceptionLoggingMethod;
  }

  /**
   * Creates a Exception with a custom caused Exception, the logging method will log the
   * stacktrace containing also the caused Exception.
   *
   * @param e the caused Exception
   * @param exceptionLoggingMethod the logging method
   */
  CustomLoggableResponseException(Exception e, Consumer<Exception> exceptionLoggingMethod) {
    super(e);
    this.exceptionLoggingMethod = exceptionLoggingMethod;
  }

  /**
   * Creates a Exception with a custom message, the logging method will log the
   * custom message only.
   *
   * @param message the custom log message
   * @param messageLoggingMethod the logging method
   */
  CustomLoggableResponseException(String message, Consumer<String> messageLoggingMethod) {
    super(message);
    this.messageLoggingMethod = messageLoggingMethod;
  }

  /**
   * Executes the non null logging methods.
   */
  public void executeLogging() {
    if (nonNull(this.exceptionLoggingMethod)) {
      this.exceptionLoggingMethod.accept(this);
    }
    if (nonNull(this.messageLoggingMethod)) {
      this.messageLoggingMethod.accept(super.getMessage());
    }
  }

}
