package de.caritas.cob.messageservice;

import static de.caritas.cob.messageservice.api.model.MessageType.MASTER_KEY_LOST;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.BooleanUtils.isTrue;

import de.caritas.cob.messageservice.api.exception.BadRequestException;
import de.caritas.cob.messageservice.api.exception.CustomCryptoException;
import de.caritas.cob.messageservice.api.exception.InternalServerErrorException;
import de.caritas.cob.messageservice.api.exception.RocketChatPostMarkGroupAsReadException;
import de.caritas.cob.messageservice.api.exception.RocketChatSendMessageException;
import de.caritas.cob.messageservice.api.facade.EmailNotificationFacade;
import de.caritas.cob.messageservice.api.helper.AuthenticatedUser;
import de.caritas.cob.messageservice.api.helper.AuthenticatedUserHelper;
import de.caritas.cob.messageservice.api.model.AliasArgs;
import de.caritas.cob.messageservice.api.model.AliasMessageDTO;
import de.caritas.cob.messageservice.api.model.ChatMessage;
import de.caritas.cob.messageservice.api.model.MessageResponseDTO;
import de.caritas.cob.messageservice.api.model.MessageType;
import de.caritas.cob.messageservice.api.model.ReassignStatus;
import de.caritas.cob.messageservice.api.model.VideoCallMessageDTO;
import de.caritas.cob.messageservice.api.model.rocket.chat.group.GetGroupInfoDto;
import de.caritas.cob.messageservice.api.model.rocket.chat.message.SendMessageResponseDTO;
import de.caritas.cob.messageservice.api.service.DraftMessageService;
import de.caritas.cob.messageservice.api.service.LiveEventNotificationService;
import de.caritas.cob.messageservice.api.service.LogService;
import de.caritas.cob.messageservice.api.service.MessageMapper;
import de.caritas.cob.messageservice.api.service.RocketChatService;
import de.caritas.cob.messageservice.api.service.SessionService;
import de.caritas.cob.messageservice.api.service.dto.Message;
import de.caritas.cob.messageservice.api.service.statistics.StatisticsService;
import de.caritas.cob.messageservice.api.service.statistics.event.CreateMessageStatisticsEvent;
import de.caritas.cob.messageservice.api.tenant.TenantContext;
import de.caritas.cob.messageservice.statisticsservice.generated.web.model.UserRole;
import de.caritas.cob.messageservice.userservice.generated.web.model.GroupSessionConsultantDTO;
import de.caritas.cob.messageservice.userservice.generated.web.model.SessionUserDTO;
import java.util.Optional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/*
 * Facade to encapsulate the steps for posting a (group) message to Rocket.Chat
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class Messenger {

  private static final String FEEDBACK_GROUP_IDENTIFIER = "feedback";

  private final @NonNull RocketChatService rocketChatService;
  private final @NonNull EmailNotificationFacade emailNotificationFacade;
  private final @NonNull LiveEventNotificationService liveEventNotificationService;
  private final @NonNull DraftMessageService draftMessageService;
  private final @NonNull StatisticsService statisticsService;
  private final @NonNull AuthenticatedUser authenticatedUser;
  private final @NonNull MessageMapper mapper;

  private final @NonNull SessionService sessionService;

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
  public MessageResponseDTO postGroupMessage(ChatMessage chatMessage) {
    var response = postRocketChatGroupMessage(chatMessage);
    notifyAndClearDraft(chatMessage);
    return response;
  }

  /**
   * Posts a message to the given Rocket.Chat feedback group id and sends out a notification e-mail
   * via the UserService (because we need to get the user information).
   *
   * @param feedbackGroupMessage the message
   */
  public MessageResponseDTO postFeedbackGroupMessage(ChatMessage feedbackGroupMessage) {
    validateFeedbackChatId(feedbackGroupMessage);
    var response = postRocketChatGroupMessage(feedbackGroupMessage);
    notifyAndClearDraftForFeedbackGroup(feedbackGroupMessage);
    return response;
  }

  private void notifyAndClearDraft(ChatMessage chatMessage) {
    draftMessageService.deleteDraftMessageIfExist(chatMessage.getRcGroupId());

    if (!this.rocketChatSystemUserId.equals(chatMessage.getRcUserId())) {
      liveEventNotificationService.sendLiveEvent(
          chatMessage.getRcGroupId(),
          authenticatedUser.getAccessToken(),
          TenantContext.getCurrentTenantOption()
      );
    }
    if (isTrue(chatMessage.isSendNotification())) {
      emailNotificationFacade.sendEmailAboutNewChatMessage(
          chatMessage.getRcGroupId(),
          TenantContext.getCurrentTenantOption(),
          authenticatedUser.getAccessToken());
    }

    statisticsService.fireEvent(new CreateMessageStatisticsEvent(authenticatedUser.getUserId(),
        resolveUserRole(authenticatedUser), chatMessage.getRcGroupId(), false, resolveRecipentUserId(chatMessage)));
  }

  private String resolveRecipentUserId(ChatMessage chatMessage) {
    de.caritas.cob.messageservice.userservice.generated.web.model.GroupSessionListResponseDTO sessionBelongingToRcGroupId = sessionService.findSessionBelongingToRcGroupId(
        chatMessage.getRcToken(), chatMessage.getRcGroupId());
    if (sessionBelongingToRcGroupId != null && sessionBelongingToRcGroupId.getSessions() != null) {
      var optionalSession = sessionBelongingToRcGroupId.getSessions().stream().findFirst();
      if (authenticatedUser.isConsultant()) {
        return optionalSession.isPresent() ? getUserId(optionalSession.get()) : null;
      } else if (authenticatedUser.isAdviceSeeker()) {
        return optionalSession.isPresent() ? getConsultantId(optionalSession.get()) : null;
      }
    }
    return null;
  }

  private String getConsultantId(
      de.caritas.cob.messageservice.userservice.generated.web.model.GroupSessionResponseDTO session) {
    GroupSessionConsultantDTO consultant = session.getConsultant();
    return consultant != null ? consultant.getId() : null;
  }

  private String getUserId(
      de.caritas.cob.messageservice.userservice.generated.web.model.GroupSessionResponseDTO session) {
    SessionUserDTO user = session.getUser();
    return user != null ? user.getId() : null;
  }

  private UserRole resolveUserRole(AuthenticatedUser authenticatedUser) {
    return (AuthenticatedUserHelper.isConsultant(authenticatedUser)) ? UserRole.CONSULTANT
        : UserRole.ASKER;
  }

  private void notifyAndClearDraftForFeedbackGroup(ChatMessage feedbackGroupMessage) {
    draftMessageService.deleteDraftMessageIfExist(feedbackGroupMessage.getRcGroupId());

    if (!this.rocketChatSystemUserId.equals(feedbackGroupMessage.getRcUserId())) {
      liveEventNotificationService.sendLiveEvent(
          feedbackGroupMessage.getRcGroupId(),
          authenticatedUser.getAccessToken(),
          TenantContext.getCurrentTenantOption()
      );
    }
    if (isTrue(feedbackGroupMessage.isSendNotification())) {
      emailNotificationFacade.sendEmailAboutNewFeedbackMessage(
          feedbackGroupMessage.getRcGroupId(),
          TenantContext.getCurrentTenantOption(),
          authenticatedUser.getAccessToken()
      );
    }
  }

  /**
   * Posts a message to the given Rocket.Chat group id
   *
   * @param groupMessage the message
   */
  private MessageResponseDTO postRocketChatGroupMessage(ChatMessage groupMessage) {
    try {
      // Send message to Rocket.Chat via RocketChatService
      SendMessageResponseDTO response = rocketChatService.postGroupMessage(groupMessage);
      if (isNull(response) || !response.isSuccess()) {
        throw new InternalServerErrorException();
      }
      // Set all messages as read for system message user
      rocketChatService.markGroupAsReadForSystemUser(groupMessage.getRcGroupId());
      return mapper.messageResponseOf(response);
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
   * @return {@link MessageResponseDTO}
   */
  public MessageResponseDTO createVideoHintMessage(String rcGroupId,
      VideoCallMessageDTO videoCallMessageDTO) {
    AliasMessageDTO aliasMessageDTO = new AliasMessageDTO().videoCallMessageDTO(
        videoCallMessageDTO);
    var response = this.rocketChatService.postAliasOnlyMessageAsSystemUser(rcGroupId,
        aliasMessageDTO);
    return mapper.messageResponseOf(response);
  }

  /**
   * Creates an event with the provided {@link MessageType} in specified Rocket.Chat group.
   *
   * @param rcGroupId   Rocket.Chat group ID
   * @param messageType {@link MessageType}
   * @param aliasArgs   optional arguments
   * @return {@link MessageResponseDTO}
   */
  public MessageResponseDTO createEvent(String rcGroupId, MessageType messageType,
      AliasArgs aliasArgs) {
    var aliasMessage = mapper.aliasMessageDtoOf(messageType);
    var messageString = mapper.messageStringOf(aliasArgs);

    var response = rocketChatService.postAliasOnlyMessageAsSystemUser(
        rcGroupId, aliasMessage, messageString
    );

    if (MASTER_KEY_LOST.equals(messageType)) {
      emailNotificationFacade.sendEmailAboutNewChatMessage(
          rcGroupId,
          TenantContext.getCurrentTenantOption(),
          authenticatedUser.getAccessToken()
      );
    }

    if (nonNull(aliasArgs) && aliasArgs.getStatus().equals(ReassignStatus.REQUESTED)) {
      emailNotificationFacade.sendEmailAboutReassignRequest(
          rcGroupId,
          aliasArgs,
          TenantContext.getCurrentTenantOption(),
          authenticatedUser.getAccessToken()
      );
    }

    return mapper.messageResponseOf(response);
  }

  public Optional<Message> findMessage(String rcToken, String rcUserId, String messageId) {
    var message = rocketChatService.findMessage(rcToken, rcUserId, messageId);

    return Optional.ofNullable(message)
        .map(mapper::decryptedMessageOf)
        .map(mapper::typedMessageOf);
  }

  public boolean patchEventMessage(String rcToken, String rcUserId, String messageId,
      ReassignStatus status) {
    var message = rocketChatService.findMessage(rcToken, rcUserId, messageId);
    if (isNull(message)) {
      return false;
    }

    if (!message.isA(MessageType.REASSIGN_CONSULTANT)) {
      var errorMessage = String.format("Message (%s) is not a reassignment.", messageId);
      throw new BadRequestException(errorMessage, LogService::logBadRequest);
    }

    var consultantReassignment = mapper.consultantReassignmentOf(message);
    consultantReassignment.setStatus(status);
    var updatedMessage = mapper.updateMessageOf(message, consultantReassignment);

    var isUpdated = rocketChatService.updateMessage(updatedMessage);
    if (isUpdated && status == ReassignStatus.CONFIRMED) {
      emailNotificationFacade.sendEmailAboutReassignDecision(
          updatedMessage.getRoomId(),
          consultantReassignment,
          TenantContext.getCurrentTenantOption(),
          authenticatedUser.getAccessToken()
      );
    }

    return isUpdated;
  }

  /**
   * Posts a message which contains an alias with the provided {@link MessageType} in
   * the specified Rocket.Chat group.
   *
   * @param rcGroupId   Rocket.Chat group ID
   * @param messageType {@link MessageType}
   * @return {@link MessageResponseDTO}
   */
  public MessageResponseDTO postAliasMessage(String rcGroupId, MessageType messageType, String content) {
    AliasMessageDTO aliasMessageDTO = new AliasMessageDTO().messageType(messageType).content(content);

    var response = this.rocketChatService.postAliasOnlyMessageAsSystemUser(rcGroupId,
        aliasMessageDTO);
    return mapper.messageResponseOf(response);
  }

  public boolean deleteMessage(String rcToken, String rcUserId, String messageId) {
    return rocketChatService.deleteMessage(rcToken, rcUserId, messageId);
  }

  public boolean deleteAttachment(String rcToken, String rcUserId, String attachmentId) {
    return rocketChatService.deleteAttachment(rcToken, rcUserId, attachmentId);
  }
}
