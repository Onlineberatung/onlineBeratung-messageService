package de.caritas.cob.messageservice.api.facade;

import static java.util.Objects.isNull;
import static org.apache.commons.lang3.BooleanUtils.isTrue;

import de.caritas.cob.messageservice.api.exception.BadRequestException;
import de.caritas.cob.messageservice.api.exception.CustomCryptoException;
import de.caritas.cob.messageservice.api.exception.InternalServerErrorException;
import de.caritas.cob.messageservice.api.exception.RocketChatPostMarkGroupAsReadException;
import de.caritas.cob.messageservice.api.exception.RocketChatSendMessageException;
import de.caritas.cob.messageservice.api.helper.AuthenticatedUser;
import de.caritas.cob.messageservice.api.helper.AuthenticatedUserHelper;
import de.caritas.cob.messageservice.api.model.AliasMessageDTO;
import de.caritas.cob.messageservice.api.model.ChatMessage;
import de.caritas.cob.messageservice.api.model.MessageType;
import de.caritas.cob.messageservice.api.model.VideoCallMessageDTO;
import de.caritas.cob.messageservice.api.model.rocket.chat.group.GetGroupInfoDto;
import de.caritas.cob.messageservice.api.model.rocket.chat.message.SendMessageResponseDTO;
import de.caritas.cob.messageservice.api.service.DraftMessageService;
import de.caritas.cob.messageservice.api.service.LiveEventNotificationService;
import de.caritas.cob.messageservice.api.service.LogService;
import de.caritas.cob.messageservice.api.service.RocketChatService;
import de.caritas.cob.messageservice.api.service.statistics.StatisticsService;
import de.caritas.cob.messageservice.api.service.statistics.event.CreateMessageStatisticsEvent;
import de.caritas.cob.messageservice.statisticsservice.generated.web.model.UserRole;
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

  private final @NonNull RocketChatService rocketChatService;
  private final @NonNull EmailNotificationFacade emailNotificationFacade;
  private final @NonNull LiveEventNotificationService liveEventNotificationService;
  private final @NonNull DraftMessageService draftMessageService;
  private final @NonNull StatisticsService statisticsService;
  private final @NonNull AuthenticatedUser authenticatedUser;

  @Value("${rocket.systemuser.id}")
  private String rocketChatSystemUserId;

  /**
   * Posts a message to the given Rocket.Chat group id and sends out a notification e-mail via the
   * UserService (because we need to get the user information).
   * <p>
   * If the statistics function is enabled, the assignment of the enquired is processed as
   * statistical event.
   *
   * @param chatMessage the message
   */
  public void postGroupMessage(ChatMessage chatMessage) {

    postRocketChatGroupMessage(chatMessage);
    this.draftMessageService.deleteDraftMessageIfExist(chatMessage.getRcGroupId());

    if (!this.rocketChatSystemUserId.equals(chatMessage.getRcUserId())) {
      this.liveEventNotificationService.sendLiveEvent(chatMessage.getRcGroupId());
    }
    if (isTrue(chatMessage.isSendNotification())) {
      emailNotificationFacade.sendEmailNotification(chatMessage.getRcGroupId());
    }

    statisticsService.fireEvent(new CreateMessageStatisticsEvent(authenticatedUser.getUserId(),
        resolveUserRole(authenticatedUser), chatMessage.getRcGroupId(), false));
  }

  private UserRole resolveUserRole(AuthenticatedUser authenticatedUser) {
    return (AuthenticatedUserHelper.isConsultant(authenticatedUser)) ? UserRole.CONSULTANT
        : UserRole.ASKER;
  }

  /**
   * Posts a message to the given Rocket.Chat feedback group id and sends out a notification e-mail
   * via the UserService (because we need to get the user information).
   *
   * @param feedbackGroupMessage the message
   */
  public void postFeedbackGroupMessage(ChatMessage feedbackGroupMessage) {
    var rcFeedbackGroupId = feedbackGroupMessage.getRcGroupId();
    validateFeedbackChatId(feedbackGroupMessage);
    postRocketChatGroupMessage(feedbackGroupMessage);
    draftMessageService.deleteDraftMessageIfExist(rcFeedbackGroupId);
    liveEventNotificationService.sendLiveEvent(rcFeedbackGroupId);
    emailNotificationFacade.sendFeedbackEmailNotification(rcFeedbackGroupId);
  }

  /**
   * Posts a message to the given Rocket.Chat group id
   *
   * @param groupMessage the message
   */
  private void postRocketChatGroupMessage(ChatMessage groupMessage) {

    try {
      // Send message to Rocket.Chat via RocketChatService
      SendMessageResponseDTO response = rocketChatService.postGroupMessage(groupMessage);
      if (isNull(response) || !response.isSuccess()) {
        throw new InternalServerErrorException();
      }

      // Set all messages as read for system message user
      rocketChatService.markGroupAsReadForSystemUser(groupMessage.getRcGroupId());

    } catch (RocketChatSendMessageException
             | RocketChatPostMarkGroupAsReadException
             | CustomCryptoException ex) {
      throw new InternalServerErrorException(ex, LogService::logInternalServerError);
    }
  }

  private void validateFeedbackChatId(ChatMessage feedbackMessage) {
    GetGroupInfoDto groupDto = rocketChatService.getGroupInfo(feedbackMessage.getRcToken(),
        feedbackMessage.getRcUserId(), feedbackMessage.getRcGroupId());

    if (!groupDto.getGroup().getName().contains(FEEDBACK_GROUP_IDENTIFIER)) {
      throw new BadRequestException(
          String.format("Provided Rocket.Chat group ID %s is no feedback chat.",
              feedbackMessage.getRcGroupId()), LogService::logBadRequest);
    }
  }

  /**
   * Creates a {@link VideoCallMessageDTO} and posts it into Rocket.Chat room.
   *
   * @param rcGroupId           the identifier for the Rocket.Chat room
   * @param videoCallMessageDTO the {@link VideoCallMessageDTO}
   */
  public void createVideoHintMessage(String rcGroupId, VideoCallMessageDTO videoCallMessageDTO) {
    AliasMessageDTO aliasMessageDTO =
        new AliasMessageDTO().videoCallMessageDTO(videoCallMessageDTO);
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
