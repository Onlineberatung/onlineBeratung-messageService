package de.caritas.cob.messageservice.api.facade;

import static java.util.Objects.isNull;

import de.caritas.cob.messageservice.api.exception.BadRequestException;
import de.caritas.cob.messageservice.api.exception.CustomCryptoException;
import de.caritas.cob.messageservice.api.exception.InternalServerErrorException;
import de.caritas.cob.messageservice.api.exception.RocketChatPostMarkGroupAsReadException;
import de.caritas.cob.messageservice.api.exception.RocketChatPostMessageException;
import de.caritas.cob.messageservice.api.model.MessageDTO;
import de.caritas.cob.messageservice.api.model.rocket.chat.group.GetGroupInfoDto;
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

  private static final String FEEDBACK_GROUP_IDENTIFIER = "feedback";
  private final RocketChatService rocketChatService;
  private final EmailNotificationFacade emailNotificationFacade;

  /**
   * Constructor
   *
   * @param rocketChatService {@link RocketChatService}
   */
  @Autowired
  public PostGroupMessageFacade(RocketChatService rocketChatService,
      EmailNotificationFacade emailNotificationFacade) {
    this.rocketChatService = rocketChatService;
    this.emailNotificationFacade = emailNotificationFacade;
  }

  /**
   * Posts a message to the given Rocket.Chat group id and sends out a notification e-mail via the
   * UserService (because we need to get the user information).
   *
   * @param rcToken   Rocket.Chat token
   * @param rcUserId  Rocket.Chat user ID
   * @param rcGroupId Rocket.Chat group ID
   * @param message   the message
   */
  public void postGroupMessage(String rcToken, String rcUserId, String rcGroupId,
      MessageDTO message) {

    postRocketChatGroupMessage(rcToken, rcUserId, rcGroupId, message.getMessage(), null);

    if (message.isSendNotification()) {
      emailNotificationFacade.sendEmailNotification(rcGroupId);
    }
  }

  /**
   * Posts a message to the given Rocket.Chat feedback group id and sends out a notification e-mail
   * via the UserService (because we need to get the user information).
   *
   * @param rcToken           Rocket.Chat token
   * @param rcUserId          Rocket.Chat user ID
   * @param rcFeedbackGroupId Rocket.Chat feedback group ID
   * @param message           the message
   */
  public void postFeedbackGroupMessage(String rcToken, String rcUserId, String rcFeedbackGroupId,
      String message, String alias) {

    validateFeedbackChatId(rcToken, rcUserId, rcFeedbackGroupId);
    postRocketChatGroupMessage(rcToken, rcUserId, rcFeedbackGroupId, message, alias);
    emailNotificationFacade.sendFeedbackEmailNotification(rcFeedbackGroupId);
  }

  /**
   * Posts a message to the given Rocket.Chat group id
   *
   * @param rcToken   Rocket.Chat token
   * @param rcUserId  Rocket.Chat user ID
   * @param rcGroupId Rocket.Chat group ID
   * @param message   the message
   * @param alias     alias containing forward message information
   */
  private void postRocketChatGroupMessage(String rcToken, String rcUserId, String rcGroupId,
      String message, String alias) {

    try {
      // Send message to Rocket.Chat via RocketChatService
      PostMessageResponseDTO response =
          rocketChatService.postGroupMessage(rcToken, rcUserId, rcGroupId, message, alias);
      if (isNull(response) || !response.isSuccess()) {
        throw new InternalServerErrorException();
      }

      // Set all messages as read for system message user
      rocketChatService.markGroupAsReadForSystemUser(rcGroupId);

    } catch (RocketChatPostMessageException
        | RocketChatPostMarkGroupAsReadException
        | CustomCryptoException ex) {
      throw new InternalServerErrorException(ex, LogService::logInternalServerError);
    }
  }

  private void validateFeedbackChatId(String rcToken, String rcUserId, String rcFeedbackGroupId) {
    GetGroupInfoDto groupDto = rocketChatService.getGroupInfo(rcToken, rcUserId, rcFeedbackGroupId);

    if (!groupDto.getGroup().getName().contains(FEEDBACK_GROUP_IDENTIFIER)) {
      throw new BadRequestException(
          String.format("Provided Rocket.Chat group ID %s is no feedback chat.", rcFeedbackGroupId),
          LogService::logBadRequest);
    }
  }
}
