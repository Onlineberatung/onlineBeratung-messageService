package de.caritas.cob.messageservice.api.facade;

import static java.util.Objects.isNull;

import de.caritas.cob.messageservice.api.exception.CustomCryptoException;
import de.caritas.cob.messageservice.api.exception.InternalServerErrorException;
import de.caritas.cob.messageservice.api.exception.RocketChatPostMarkGroupAsReadException;
import de.caritas.cob.messageservice.api.exception.RocketChatPostMessageException;
import de.caritas.cob.messageservice.api.model.MessageDTO;
import de.caritas.cob.messageservice.api.model.rocket.chat.message.PostMessageResponseDTO;
import de.caritas.cob.messageservice.api.service.LogService;
import de.caritas.cob.messageservice.api.service.RocketChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
   */
  public void postGroupMessage(String rcToken, String rcUserId, String rcGroupId,
      MessageDTO message) {

    postRocketChatGroupMessage(rcToken, rcUserId, rcGroupId, message.getMessage(), null);

    if (message.isSendNotification()) {
      emailNoticationFacade.sendEmailNotification(rcGroupId);
    }
  }

  /**
   * Posts a message to the given Rocket.Chat feedback group id and sends out a notification e-mail
   * via the UserService (because we need to get the user information).
   *
   * @param rcToken
   * @param rcUserId
   * @param rcFeedbackGroupId
   * @param message
   */
  public void postFeedbackGroupMessage(String rcToken, String rcUserId,
      String rcFeedbackGroupId, String message, String alias) {

    postRocketChatGroupMessage(rcToken, rcUserId, rcFeedbackGroupId, message, alias);
    emailNoticationFacade.sendFeedbackEmailNotification(rcFeedbackGroupId);
  }

  /**
   * Posts a message to the given Rocket.Chat group id
   *
   * @param rcToken
   * @param rcUserId
   * @param rcGroupId
   * @param message
   * @param alias
   */
  private void postRocketChatGroupMessage(String rcToken, String rcUserId, String rcGroupId,
      String message, String alias) {

    try {
      // Send message to Rocket.Chat via RocketChatService
      PostMessageResponseDTO response = rocketChatService.postGroupMessage(rcToken, rcUserId,
          rcGroupId, message, alias);
      if (isNull(response) || !response.isSuccess()) {
        throw new InternalServerErrorException();
      }

      // Set all messages as read for system message user
      rocketChatService.markGroupAsReadForSystemUser(rcGroupId);

    } catch (RocketChatPostMessageException | RocketChatPostMarkGroupAsReadException | CustomCryptoException ex) {
      throw new InternalServerErrorException(ex, LogService::logInternalServerError);
    }
  }
}
