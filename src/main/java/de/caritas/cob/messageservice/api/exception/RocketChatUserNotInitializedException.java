package de.caritas.cob.messageservice.api.exception;

/**
 * Exception for uninitialized rocket chat user.
 */
public class RocketChatUserNotInitializedException extends Exception {

  /**
   * Constructor creates a new {@link RocketChatUserNotInitializedException}.
   *
   * @param message the message
   */
  public RocketChatUserNotInitializedException(String message) {
    super(message);
  }

}
