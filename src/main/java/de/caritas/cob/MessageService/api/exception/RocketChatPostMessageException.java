package de.caritas.cob.MessageService.api.exception;

public class RocketChatPostMessageException extends RuntimeException {

  private static final long serialVersionUID = 1232112575979020931L;

  /**
   * Exception, when a Rocket.Chat API call for message posting fails
   * 
   * @param ex
   */
  public RocketChatPostMessageException(Exception ex) {
    super(ex);
  }

}
