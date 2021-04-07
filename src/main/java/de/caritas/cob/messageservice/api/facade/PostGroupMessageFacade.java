package de.caritas.cob.messageservice.api.facade;

import static java.util.Objects.isNull;
import static org.apache.commons.lang3.BooleanUtils.isTrue;

import de.caritas.cob.messageservice.api.exception.BadRequestException;
import de.caritas.cob.messageservice.api.exception.CustomCryptoException;
import de.caritas.cob.messageservice.api.exception.InternalServerErrorException;
import de.caritas.cob.messageservice.api.exception.RocketChatPostMarkGroupAsReadException;
import de.caritas.cob.messageservice.api.exception.RocketChatPostMessageException;
import de.caritas.cob.messageservice.api.model.AliasMessageDTO;
import de.caritas.cob.messageservice.api.model.MessageDTO;
import de.caritas.cob.messageservice.api.model.MessageType;
import de.caritas.cob.messageservice.api.model.VideoCallMessageDTO;
import de.caritas.cob.messageservice.api.model.rocket.chat.group.GetGroupInfoDto;
import de.caritas.cob.messageservice.api.model.rocket.chat.message.PostMessageResponseDTO;
import de.caritas.cob.messageservice.api.service.DraftMessageService;
import de.caritas.cob.messageservice.api.service.LiveEventNotificationService;
import de.caritas.cob.messageservice.api.service.LogService;
import de.caritas.cob.messageservice.api.service.RocketChatService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/*
 * Facade to encapsulate the steps for posting a (group) message to Rocket.Chat
 */
@Service
@RequiredArgsConstructor
public class PostGroupMessageFacade {

  private static final String FEEDBACK_GROUP_IDENTIFIER = "feedback";

  @Value("${rocket.systemuser.id}")
  private String rocketChatSystemUserId;

  private final @NonNull RocketChatService rocketChatService;
  private final @NonNull EmailNotificationFacade emailNotificationFacade;
  private final @NonNull LiveEventNotificationService liveEventNotificationService;
  private final @NonNull DraftMessageService draftMessageService;

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
    this.draftMessageService.deleteDraftMessageIfExist(rcGroupId);

    if (!this.rocketChatSystemUserId.equals(rcUserId)) {
      this.liveEventNotificationService.sendLiveEvent(rcGroupId);
    }
    if (isTrue(message.getSendNotification())) {
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
    this.draftMessageService.deleteDraftMessageIfExist(rcFeedbackGroupId);
    this.liveEventNotificationService.sendLiveEvent(rcFeedbackGroupId);
    emailNotificationFacade.sendFeedbackEmailNotification(rcFeedbackGroupId);
  }

  /**
   * Posts a message to the given Rocket.Chat group id
   *
   * @param rcToken   Rocket.Chat token
   * @param rcUserId  Rocket.Chat user ID
   * @param rcGroupId Rocket.Chat group ID
   * @param message   the message
   * @param alias     alias containing additional message information
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

  /**
   * Creates a {@link VideoCallMessageDTO} and posts it into Rocket.Chat room.
   *
   * @param rcGroupId the identifier for the Rocket.Chat room
   * @param videoCallMessageDTO the {@link VideoCallMessageDTO}
   */
  public void createVideoHintMessage(String rcGroupId, VideoCallMessageDTO videoCallMessageDTO) {
    AliasMessageDTO aliasMessageDTO = new AliasMessageDTO()
        .videoCallMessageDTO(videoCallMessageDTO);
    this.rocketChatService.postAliasOnlyMessageAsSystemUser(rcGroupId, aliasMessageDTO);
  }

  /**
   * Posts an empty message which only contains an alias with the provided {@link MessageType} in
   * the specified Rocket.Chat group.
   *
   * @param rcGroupId   Rocket.Chat group ID
   * @param messageType {@link MessageType}
   */
  public void postAliasOnlyMessage(String rcGroupId, MessageType messageType) {
    AliasMessageDTO aliasMessageDTO = new AliasMessageDTO().messageType(messageType);
    this.rocketChatService.postAliasOnlyMessageAsSystemUser(rcGroupId, aliasMessageDTO);
  }
}
