package de.caritas.cob.messageservice.api.exception;

import java.util.function.Consumer;

/** Represents the response status code 400 - Bad Request. */
public class BadRequestException extends CustomLoggableResponseException {

  /**
   * Creates a Bad Request with a custom message to be logged.
   *
   * @param message the message
   * @param loggingMethod the method to log that exception
   */
  public BadRequestException(String message, Consumer<String> loggingMethod) {
    super(message, loggingMethod);
  }
}
