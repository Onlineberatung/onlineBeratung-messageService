package de.caritas.cob.messageservice.api.controller;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import de.caritas.cob.messageservice.Messenger;
import de.caritas.cob.messageservice.api.exception.BadRequestException;
import de.caritas.cob.messageservice.api.helper.JSONHelper;
import de.caritas.cob.messageservice.api.model.AliasArgs;
import de.caritas.cob.messageservice.api.model.AliasMessageDTO;
import de.caritas.cob.messageservice.api.model.AliasOnlyMessageDTO;
import de.caritas.cob.messageservice.api.model.ChatMessage;
import de.caritas.cob.messageservice.api.model.DraftMessageDTO;
import de.caritas.cob.messageservice.api.model.ForwardMessageDTO;
import de.caritas.cob.messageservice.api.model.MasterKeyDTO;
import de.caritas.cob.messageservice.api.model.MessageDTO;
import de.caritas.cob.messageservice.api.model.MessageResponseDTO;
import de.caritas.cob.messageservice.api.model.MessageStreamDTO;
import de.caritas.cob.messageservice.api.model.MessageType;
import de.caritas.cob.messageservice.api.model.ReassignStatus;
import de.caritas.cob.messageservice.api.model.VideoCallMessageDTO;
import de.caritas.cob.messageservice.api.model.draftmessage.SavedDraftType;
import de.caritas.cob.messageservice.api.model.rocket.chat.message.MessagesDTO;
import de.caritas.cob.messageservice.api.service.DraftMessageService;
import de.caritas.cob.messageservice.api.service.EncryptionService;
import de.caritas.cob.messageservice.api.service.LogService;
import de.caritas.cob.messageservice.api.service.MessageMapper;
import de.caritas.cob.messageservice.api.service.RocketChatService;
import de.caritas.cob.messageservice.generated.api.controller.MessagesApi;
import io.swagger.annotations.Api;
import java.time.Instant;
import java.util.Optional;
import javax.validation.Valid;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for message requests.
 */
@RestController
@RequiredArgsConstructor
@Api(tags = "message-controller")
public class MessageController implements MessagesApi {

  private final @NonNull RocketChatService rocketChatService;
  private final @NonNull EncryptionService encryptionService;
  private final @NonNull Messenger messenger;
  private final @NonNull DraftMessageService draftMessageService;
  private final @NonNull MessageMapper mapper;


  /**
   * Returns a list of {@link MessageStreamDTO}s from the specified Rocket.Chat group.
   *
   * @param rcToken   (required) Rocket.Chat token of the user
   * @param rcUserId  (required) Rocket.Chat user ID
   * @param rcGroupId (required) Rocket.Chat group ID
   * @return {@link ResponseEntity} containing {@link MessageStreamDTO}
   */
  @Override
  public ResponseEntity<MessageStreamDTO> findMessages(String rcToken, String rcUserId,
      String rcGroupId, Integer offset, Integer count, Instant since) {
    if (isNull(since)) {
      since = Instant.MIN;
    }
    var message = rocketChatService.getGroupMessages(
        rcToken, rcUserId, rcGroupId, offset, count, since
    );

    return (message != null)
        ? new ResponseEntity<>(message, HttpStatus.OK)
        : new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  /**
   * Updates the Master-Key Fragment for the en-/decryption of messages.
   *
   * @param masterKey the master key
   * @return {@link ResponseEntity} with the {@link HttpStatus}
   */
  @Override
  public ResponseEntity<Void> updateKey(@Valid @RequestBody MasterKeyDTO masterKey) {

    if (!encryptionService.getMasterKey().equals(masterKey.getMasterKey())) {
      encryptionService.updateMasterKey(masterKey.getMasterKey());
      LogService.logInfo("MasterKey updated");
      return new ResponseEntity<>(HttpStatus.OK);
    }

    return new ResponseEntity<>(HttpStatus.CONFLICT);
  }

  /**
   * Posts a message in the specified Rocket.Chat group.
   *
   * @param rcToken   (required) Rocket.Chat token of the user
   * @param rcUserId  (required) Rocket.Chat user ID
   * @param rcGroupId (required) Rocket.Chat group ID
   * @param message   (required) the message
   * @return {@link ResponseEntity} with the {@link HttpStatus}
   */
  @Override
  public ResponseEntity<MessageResponseDTO> createMessage(@RequestHeader String rcToken,
      @RequestHeader String rcUserId, @RequestHeader String rcGroupId,
      @Valid @RequestBody MessageDTO message) {

    var groupMessage = ChatMessage.builder()
        .rcToken(rcToken)
        .rcUserId(rcUserId)
        .rcGroupId(rcGroupId)
        .text(message.getMessage())
        .sendNotification(Boolean.TRUE.equals(message.getSendNotification()))
        .type(message.getT()).build();

    var response = messenger.postGroupMessage(groupMessage);

    return new ResponseEntity<>(response, HttpStatus.CREATED);
  }

  /**
   * Forwards/posts a message in the specified Rocket.Chat group and sets the values from the body
   * object in the alias object of the Rocket.Chat message.
   *
   * @param rcToken           (required) Rocket.Chat token of the user
   * @param rcUserId          (required) Rocket.Chat user ID
   * @param rcGroupId         (required) Rocket.Chat group ID
   * @param forwardMessageDTO (required) {@link ForwardMessageDTO}
   * @return {@link ResponseEntity} with the {@link HttpStatus}
   */
  @Override
  public ResponseEntity<MessageResponseDTO> forwardMessage(@RequestHeader String rcToken,
      @RequestHeader String rcUserId, @RequestHeader String rcGroupId,
      @Valid @RequestBody ForwardMessageDTO forwardMessageDTO) {

    Optional<String> alias =
        JSONHelper.convertAliasMessageDTOToString(
            new AliasMessageDTO().forwardMessageDTO(forwardMessageDTO));

    if (alias.isEmpty()) {
      return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }

    var forwardMessage = ChatMessage.builder().rcToken(rcToken).rcUserId(rcUserId)
        .rcGroupId(rcGroupId).text(forwardMessageDTO.getMessage())
        .type(forwardMessageDTO.getT()).alias(alias.get())
        .build();
    var response = messenger.postFeedbackGroupMessage(forwardMessage);

    return new ResponseEntity<>(response, HttpStatus.CREATED);
  }

  /**
   * Posts a message in the specified Feedback Rocket.Chat group.
   *
   * @param rcToken           (required) Rocket.Chat token of the user
   * @param rcUserId          (required) Rocket.Chat user ID
   * @param rcFeedbackGroupId (required) Rocket.Chat group ID
   * @param message           (required) the message
   * @return {@link ResponseEntity} with the {@link HttpStatus}
   */
  @Override
  public ResponseEntity<MessageResponseDTO> createFeedbackMessage(@RequestHeader String rcToken,
      @RequestHeader String rcUserId, @RequestHeader String rcFeedbackGroupId,
      @Valid @RequestBody MessageDTO message) {

    var feedbackMessage = ChatMessage.builder()
        .rcToken(rcToken)
        .rcUserId(rcUserId)
        .rcGroupId(rcFeedbackGroupId)
        .text(message.getMessage())
        .sendNotification(Boolean.TRUE.equals(message.getSendNotification()))
        .type(message.getT()).build();

    var response = messenger.postFeedbackGroupMessage(feedbackMessage);

    return new ResponseEntity<>(response, HttpStatus.CREATED);
  }

  /**
   * Creates a video event hint message.
   *
   * @param rcGroupId           the Rocket.Chat group to post the hint message
   * @param videoCallMessageDTO the {@link VideoCallMessageDTO} containing the information to be
   *                            written in the alias object
   */
  @Override
  public ResponseEntity<MessageResponseDTO> createVideoHintMessage(@RequestHeader String rcGroupId,
      @Valid @RequestBody VideoCallMessageDTO videoCallMessageDTO) {

    var response = this.messenger.createVideoHintMessage(rcGroupId,
        videoCallMessageDTO);

    return new ResponseEntity<>(response, HttpStatus.CREATED);
  }

  /**
   * Saves a draft message identified by current authenticated user and Rocket.Chat group.
   *
   * @param rcGroupId (required) Rocket.Chat group ID
   * @param message   the message
   * @return {@link ResponseEntity} with the {@link HttpStatus}
   */
  @Override
  public ResponseEntity<Void> saveDraftMessage(@RequestHeader String rcGroupId,
      @Valid @RequestBody DraftMessageDTO message) {

    SavedDraftType savedDraftType = this.draftMessageService.saveDraftMessage(message.getMessage(),
        rcGroupId, message.getT());

    return new ResponseEntity<>(savedDraftType.getHttpStatus());
  }

  /**
   * Returns a saved draft message if present.
   *
   * @param rcGroupId (required) Rocket.Chat group ID
   * @return {@link ResponseEntity} with the {@link HttpStatus}
   */
  @Override
  public ResponseEntity<DraftMessageDTO> findDraftMessage(@RequestHeader String rcGroupId) {
    Optional<DraftMessageDTO> draftMessage = this.draftMessageService.findAndDecryptDraftMessage(
        rcGroupId);
    return draftMessage.map(ResponseEntity::ok)
        .orElseGet(() -> new ResponseEntity<>(HttpStatus.NO_CONTENT));
  }

  /**
   * Posts an empty message which only contains an alias with the provided {@link MessageType} in
   * the specified Rocket.Chat group.
   *
   * @param rcGroupId           (required) Rocket.Chat group ID
   * @param aliasOnlyMessageDTO {@link AliasOnlyMessageDTO}
   * @return {@link ResponseEntity} with the {@link HttpStatus}
   */
  @Override
  public ResponseEntity<MessageResponseDTO> saveAliasOnlyMessage(@RequestHeader String rcGroupId,
      @Valid AliasOnlyMessageDTO aliasOnlyMessageDTO) {
    var type = aliasOnlyMessageDTO.getMessageType();
    var aliasArgs = aliasOnlyMessageDTO.getArgs();

    if (type.equals(MessageType.USER_MUTED) || type.equals(MessageType.USER_UNMUTED)) {
      var message = String.format("Message type (%s) is protected.", type);
      throw new BadRequestException(message, LogService::logBadRequest);
    }

    if (nonNull(aliasArgs) && type != MessageType.REASSIGN_CONSULTANT) {
      var message = String.format("Alias args are not supported by type (%s).", type);
      throw new BadRequestException(message, LogService::logBadRequest);
    }

    if (type == MessageType.REASSIGN_CONSULTANT && hasMissingMandatoryAliasArgForReassignment(
        aliasArgs)) {
      var errorFormat = "toConsultantId is required during reassignment creation (%s).";
      var message = String.format(errorFormat, MessageType.REASSIGN_CONSULTANT);
      throw new BadRequestException(message, LogService::logBadRequest);
    }

    var response = messenger.createEvent(rcGroupId, type, aliasArgs);

    return new ResponseEntity<>(response, HttpStatus.CREATED);
  }

  private boolean hasMissingMandatoryAliasArgForReassignment(AliasArgs aliasArgs) {
    if (nonNull(aliasArgs)) {
      return isNull(aliasArgs.getToConsultantId()) || isNull(aliasArgs.getFromConsultantName())
          || isNull(aliasArgs.getToConsultantName()) || isNull(aliasArgs.getToAskerName());
    }
    return true;
  }

  @Override
  public ResponseEntity<MessagesDTO> findMessage(String rcToken, String rcUserId, String msgId) {
    return messenger
        .findMessage(rcToken, rcUserId, msgId)
        .map(mapper::messageDtoOf)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @Override
  public ResponseEntity<Void> patchMessage(String rcToken, String rcUserId, String messageId,
      AliasArgs aliasArgs) {
    var reassignStatus = aliasArgs.getStatus();
    if (reassignStatus == ReassignStatus.REQUESTED) {
      var message = String.format("Updating to status (%s) is not supported.", reassignStatus);
      throw new BadRequestException(message, LogService::logBadRequest);
    }

    return messenger.patchEventMessage(rcToken, rcUserId, messageId, reassignStatus)
        ? ResponseEntity.noContent().build()
        : ResponseEntity.notFound().build();
  }

  @Override
  public ResponseEntity<Void> deleteMessage(String rcToken, String rcUserId, String messageId) {
    var message = messenger
        .findMessage(rcToken, rcUserId, messageId)
        .map(mapper::messageDtoOf)
        .orElse(null);

    if (isNull(message)) {
      return ResponseEntity.notFound().build();
    }

    if (!rcUserId.equals(message.getCreatorId())) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    if (!messenger.deleteMessage(rcToken, rcUserId, messageId)) {
      return ResponseEntity.internalServerError().build();
    }

    if (message.hasFile() && !messenger.deleteAttachment(rcToken, rcUserId, message.getFileId())) {
      return ResponseEntity.status(HttpStatus.MULTI_STATUS).build();
    }

    return ResponseEntity.noContent().build();
  }

  /**
   * Posts a message which contains an alias with the provided {@link MessageType} in the specified
   * Rocket.Chat group.
   *
   * @param rcGroupId           (required) Rocket.Chat group ID
   * @param aliasOnlyMessageDTO {@link AliasOnlyMessageDTO}
   * @return {@link ResponseEntity} with the {@link HttpStatus}
   */
  @Override
  public ResponseEntity<MessageResponseDTO> saveAliasMessageWithContent(
      @RequestHeader String rcGroupId,
      @Valid AliasMessageDTO aliasOnlyMessageDTO) {
    var type = aliasOnlyMessageDTO.getMessageType();
    var response = messenger
        .postAliasMessage(rcGroupId, type, aliasOnlyMessageDTO.getContent());
    return new ResponseEntity<>(response, HttpStatus.CREATED);
  }

}


