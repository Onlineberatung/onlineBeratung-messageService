package de.caritas.cob.MessageService.api.facade;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import de.caritas.cob.MessageService.api.exception.RocketChatPostMarkGroupAsReadException;
import de.caritas.cob.MessageService.api.exception.RocketChatPostMessageException;
import de.caritas.cob.MessageService.api.model.MessageDTO;
import de.caritas.cob.MessageService.api.model.rocket.chat.message.PostMessageResponseDTO;
import de.caritas.cob.MessageService.api.service.RocketChatService;

/*
 * Facade to encapsulate the steps for posting a (group) message to Rocket.Chat
 */
@Service
public class PostGroupMessageFacade {

  private final RocketChatService rocketChatService;
  private final EmailNotificationFacade emailNoticationFacade;

  /**
   * Constructor
   * 
   * @param rocketChatService
   */
  @Autowired
  public PostGroupMessageFacade(RocketChatService rocketChatService,
      EmailNotificationFacade emailNoticationFacade) {
    this.rocketChatService = rocketChatService;
    this.emailNoticationFacade = emailNoticationFacade;
  }

  /**
   * Posts a message to the given Rocket.Chat group id and sends out a notification e-mail via the
   * UserService (because we need to get the user information).
   * 
   * @param rcToken
   * @param rcUserId
   * @param rcGroupId
   * @param message
   * @return
   */
  public HttpStatus postGroupMessage(String rcToken, String rcUserId, String rcGroupId,
      MessageDTO message) {

    HttpStatus httpStatus =
        postRocketChatGroupMessage(rcToken, rcUserId, rcGroupId, message.getMessage(), null);

    if (message.isSendNotification() && httpStatus == HttpStatus.CREATED) {
      emailNoticationFacade.sendEmailNotification(rcGroupId);
    }

    return httpStatus;
  }

  /**
   * Posts a message to the given Rocket.Chat feedback group id and sends out a notification e-mail
   * via the UserService (because we need to get the user information).
   * 
   * @param rcToken
   * @param rcUserId
   * @param rcFeedbackGroupId
   * @param message
   * @return
   */
  public HttpStatus postFeedbackGroupMessage(String rcToken, String rcUserId,
      String rcFeedbackGroupId, String message, String alias) {

    HttpStatus httpStatus =
        postRocketChatGroupMessage(rcToken, rcUserId, rcFeedbackGroupId, message, alias);
    if (httpStatus == HttpStatus.CREATED) {
      emailNoticationFacade.sendFeedbackEmailNotification(rcFeedbackGroupId);
    }
    return httpStatus;

  }

  /**
   * Posts a message to the given Rocket.Chat group id
   * 
   * @param rcToken
   * @param rcUserId
   * @param rcGroupId
   * @param message
   * @param alias
   * @return
   */
  private HttpStatus postRocketChatGroupMessage(String rcToken, String rcUserId, String rcGroupId,
      String message, String alias) {

    PostMessageResponseDTO response = null;

    try {
      // Send message to Rocket.Chat via RocketChatService
      response = rocketChatService.postGroupMessage(rcToken, rcUserId, rcGroupId, message, alias);

      // Set all messages as read for system message user
      rocketChatService.markGroupAsReadForSystemUser(rcGroupId);

      if (response != null && response.isSuccess()) {
        return HttpStatus.CREATED;
      }

    } catch (RocketChatPostMessageException postMsgEx) {
      return HttpStatus.INTERNAL_SERVER_ERROR;

    } catch (RocketChatPostMarkGroupAsReadException e) {
      return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    return HttpStatus.INTERNAL_SERVER_ERROR;
  }
}
