package de.caritas.cob.messageservice.api.exception;

public class RocketChatGetGroupMessagesException extends RuntimeException {

  private static final long serialVersionUID = 5774038043545795683L;

  /**
   * Rocket.Chat exception
   * 
   * @param message
   */
  public RocketChatGetGroupMessagesException(String message) {
    super(message);
  }
}
