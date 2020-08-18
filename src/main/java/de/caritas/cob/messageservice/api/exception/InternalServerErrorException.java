package de.caritas.cob.messageservice.api.exception;

import de.caritas.cob.messageservice.api.service.LogService;
import java.util.function.Consumer;

/**
 * Represents the response status code 500 - Internal Server Error.
 */
public class InternalServerErrorException extends CustomLoggableResponseException {

  /**
   * Creates an Internal Server Error with a custom cause to be logged.
   *
   * @param cause the caused exception
   * @param loggingMethod the method to log that exception
   */
  public InternalServerErrorException(Exception cause, Consumer<Exception> loggingMethod) {
    super(cause, loggingMethod);
  }

  /**
   * Creates an Internal Server Error with the default error logging method.
   */
  public InternalServerErrorException() {
    super(LogService::logInternalServerError);
  }

}
