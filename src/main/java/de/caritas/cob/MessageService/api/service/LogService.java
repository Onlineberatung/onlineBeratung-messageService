package de.caritas.cob.MessageService.api.service;

import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for logging
 */

@Service
@Slf4j
public class LogService {

  private final String RC_SERVICE_ERROR = "Rocket.Chat service error: ";
  private final String RC_ENCRYPTION_SERVICE_ERROR = "Encryption service error: ";
  private final String RC_ENCRYPTION_BAD_KEY_SERVICE_ERROR =
      "Encryption service error - possible bad key error: ";
  private final String USERSERVICE_HELPER_ERROR = "UserServiceHelper error: ";
  private final String RC_BAD_REQUEST_ERROR = "Rocket.Chat Bad Request service error: ";
  private final String INTERNAL_SERVER_ERROR_TEXT = "Internal Server Error: ";
  private final String BAD_REQUEST_TEXT = "Bad Request: ";

  /**
   * Logs a Rocket.Chat service error
   *
   * @param exception
   */
  public void logRocketChatServiceError(Exception exception) {
    log.error(RC_SERVICE_ERROR + "{}",
        org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace(exception));
  }

  /**
   * Logs a Rocket.Chat service error
   * 
   * @param message
   */
  public void logRocketChatServiceError(String message) {
    log.error(RC_SERVICE_ERROR + "{}", message);
  }

  /**
   * Logs a Rocket.Chat service error
   *
   * @param exception
   */
  public void logRocketChatServiceError(String message, Exception exception) {
    log.error(RC_SERVICE_ERROR + "{}", message);
    log.error(RC_SERVICE_ERROR + "{}",
        org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace(exception));
  }

  /**
   * Logs a Encryption service error
   *
   * @param exception
   */
  public void logEncryptionServiceError(Exception exception) {
    log.error(RC_ENCRYPTION_SERVICE_ERROR + "{}",
        org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace(exception));
  }

  public void logEncryptionPossibleBadKeyError(Exception exception) {
    log.error(RC_ENCRYPTION_BAD_KEY_SERVICE_ERROR + "{}",
        org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace(exception));
  }

  /**
   * Logs a Rocket.Chat Bad Request error
   * 
   * @param exception
   */
  public void logRocketChatBadRequestError(Exception exception) {
    log.error(RC_BAD_REQUEST_ERROR + "{}",
        org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace(exception));
  }

  /**
   * Logs a UserServiceHelper error
   * 
   * @param exception
   */
  public void logUserServiceHelperError(Exception exception) {
    log.error(USERSERVICE_HELPER_ERROR + "{}",
        org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace(exception));
  }

  /**
   * Logs a Info message
   * 
   * @param msg The message
   */
  public void logInfo(String msg) {
    log.info(msg);
  }

  /**
   * Internal Server Error/Exception
   * 
   * @param message
   * @param exception
   */
  public void logInternalServerError(String message, Exception exception) {
    log.error("{}{}", INTERNAL_SERVER_ERROR_TEXT, message);
    log.error("{}", org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace(exception));
  }

  /**
   * Bad Request
   * 
   * @param message
   */
  public void logBadRequest(String message) {
    log.error(BAD_REQUEST_TEXT + "{}", message);
  }
}
